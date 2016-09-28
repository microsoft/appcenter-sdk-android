package com.microsoft.sonoma.core;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;

import com.microsoft.sonoma.core.channel.Channel;
import com.microsoft.sonoma.core.channel.DefaultChannel;
import com.microsoft.sonoma.core.ingestion.models.WrapperSdk;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.IdHelper;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.microsoft.sonoma.core.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({Sonoma.class, Channel.class, Constants.class, SonomaLog.class, StorageHelper.class, StorageHelper.PreferencesStorage.class, IdHelper.class, StorageHelper.DatabaseStorage.class, DeviceInfoHelper.class})
public class SonomaTest {

    private static final String DUMMY_APP_SECRET = "123e4567-e89b-12d3-a456-426655440000";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Iterator<ContentValues> mDataBaseScannerIterator;

    private Application application;

    @Before
    public void setUp() {
        Sonoma.unsetInstance();
        DummyFeature.sharedInstance = null;
        AnotherDummyFeature.sharedInstance = null;

        application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);

        mockStatic(Constants.class);
        mockStatic(SonomaLog.class);
        mockStatic(StorageHelper.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(IdHelper.class);
        mockStatic(StorageHelper.DatabaseStorage.class);

        /* First call to com.microsoft.sonoma.isInstanceEnabled shall return true, initial state. */
        when(StorageHelper.PreferencesStorage.getBoolean(anyString(), eq(true))).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                String key = (String) invocation.getArguments()[0];
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(anyString(), anyBoolean());

        /* Mock empty database. */
        StorageHelper.DatabaseStorage databaseStorage = mock(StorageHelper.DatabaseStorage.class);
        when(StorageHelper.DatabaseStorage.getDatabaseStorage(anyString(), anyString(), anyInt(), any(ContentValues.class), anyInt(), any(StorageHelper.DatabaseStorage.DatabaseErrorListener.class))).thenReturn(databaseStorage);
        StorageHelper.DatabaseStorage.DatabaseScanner databaseScanner = mock(StorageHelper.DatabaseStorage.DatabaseScanner.class);
        when(databaseStorage.getScanner(anyString(), anyObject())).thenReturn(databaseScanner);
        when(databaseScanner.iterator()).thenReturn(mDataBaseScannerIterator);
    }

    @After
    public void tearDown() {
        Constants.APPLICATION_DEBUGGABLE = false;
    }

    @Test
    public void singleton() {
        assertNotNull(Sonoma.getInstance());
        assertSame(Sonoma.getInstance(), Sonoma.getInstance());
    }

    @Test
    public void nullVarargClass() {
        Sonoma.start(application, DUMMY_APP_SECRET, (Class<? extends SonomaFeature>) null);

        /* Verify that no modules have been auto-loaded since none are configured for this */
        assertTrue(Sonoma.isInitialized());
        assertEquals(0, Sonoma.getInstance().getFeatures().size());
        assertEquals(application, Sonoma.getInstance().getApplication());
    }

    @Test
    public void nullVarargArray() {
        //noinspection ConfusingArgumentToVarargsMethod
        Sonoma.start(application, DUMMY_APP_SECRET, (Class<? extends SonomaFeature>[]) null);
        Sonoma.start((Class<? extends SonomaFeature>) null);
        //noinspection ConfusingArgumentToVarargsMethod
        Sonoma.start((Class<? extends SonomaFeature>[]) null);

        /* Verify that no modules have been auto-loaded since none are configured for this */
        assertTrue(Sonoma.isInitialized());
        assertEquals(0, Sonoma.getInstance().getFeatures().size());
        assertEquals(application, Sonoma.getInstance().getApplication());
    }

    @Test
    public void startFeatureBeforeInit() {
        Sonoma.start(DummyFeature.class);
        assertFalse(Sonoma.isInitialized());
        assertNull(Sonoma.getInstance().getFeatures());
    }

    @Test
    public void useDummyFeatureTest() {
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);

        /* Verify that single module has been loaded and configured */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Sonoma.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void useDummyFeatureTestSplitCall() {
        assertFalse(Sonoma.isInitialized());
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        assertTrue(Sonoma.isInitialized());
        Sonoma.start(DummyFeature.class);

        /* Verify that single module has been loaded and configured */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Sonoma.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void initAndStartTwiceTest() {
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        Sonoma.start(application, DUMMY_APP_SECRET, AnotherDummyFeature.class); //ignored

        /* Verify that single module has been loaded and configured */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Sonoma.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void initTwiceTest() {
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        Sonoma.initialize(application, DUMMY_APP_SECRET); //ignored
        Sonoma.start(DummyFeature.class);

        /* Verify that single module has been loaded and configured */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Sonoma.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }


    @Test
    public void startTwoFeaturesTest() {
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);

        /* Verify that the right amount of modules have been loaded and configured */
        assertEquals(2, Sonoma.getInstance().getFeatures().size());
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void startTwoFeaturesSplit() {
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        Sonoma.start(DummyFeature.class, AnotherDummyFeature.class);

        /* Verify that the right amount of modules have been loaded and configured */
        assertEquals(2, Sonoma.getInstance().getFeatures().size());
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void startTwoFeaturesSplitEvenMore() {
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        Sonoma.start(DummyFeature.class);
        Sonoma.start(AnotherDummyFeature.class);

        /* Verify that the right amount of modules have been loaded and configured */
        assertEquals(2, Sonoma.getInstance().getFeatures().size());
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void startTwoFeaturesWithSomeInvalidReferences() {
        Sonoma.start(application, DUMMY_APP_SECRET, null, DummyFeature.class, null, InvalidFeature.class, AnotherDummyFeature.class, null);

        /* Verify that the right amount of modules have been loaded and configured */
        assertEquals(2, Sonoma.getInstance().getFeatures().size());
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void startTwoFeaturesWithSomeInvalidReferencesSplit() {
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        Sonoma.start(null, DummyFeature.class, null);
        Sonoma.start(InvalidFeature.class, AnotherDummyFeature.class, null);

        /* Verify that the right amount of modules have been loaded and configured */
        assertEquals(2, Sonoma.getInstance().getFeatures().size());
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Sonoma.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(Channel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void startFeatureTwice() {

        /* Start once. */
        Sonoma.initialize(application, DUMMY_APP_SECRET);
        Sonoma.start(DummyFeature.class);

        /* Check. */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Sonoma.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);

        /* Start twice, this call is ignored. */
        Sonoma.start(DummyFeature.class);

        /* Verify that single module has been loaded and configured (only once interaction). */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(Channel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void enableTest() {
        /* Start Sonoma SDK */
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        Channel channel = mock(Channel.class);
        Sonoma sonoma = Sonoma.getInstance();
        sonoma.setChannel(channel);

        /* Verify modules are enabled by default */
        Set<SonomaFeature> features = sonoma.getFeatures();
        assertTrue(Sonoma.isEnabled());
        DummyFeature dummyFeature = DummyFeature.getInstance();
        AnotherDummyFeature anotherDummyFeature = AnotherDummyFeature.getInstance();
        for (SonomaFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }

        /* Explicit set enabled should not change that */
        Sonoma.setEnabled(true);
        assertTrue(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature, never()).setInstanceEnabled(anyBoolean());
        verify(anotherDummyFeature, never()).setInstanceEnabled(anyBoolean());
        verify(channel).setEnabled(true);

        /* Verify disabling base disables all modules */
        Sonoma.setEnabled(false);
        assertFalse(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        verify(dummyFeature).setInstanceEnabled(false);
        verify(anotherDummyFeature).setInstanceEnabled(false);
        verify(application).unregisterActivityLifecycleCallbacks(dummyFeature);
        verify(application).unregisterActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel).setEnabled(false);

        /* Verify re-enabling base re-enables all modules */
        Sonoma.setEnabled(true);
        assertTrue(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature).setInstanceEnabled(true);
        verify(anotherDummyFeature).setInstanceEnabled(true);
        verify(application, times(2)).registerActivityLifecycleCallbacks(dummyFeature);
        verify(application, times(2)).registerActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel, times(2)).setEnabled(true);

        /* Verify that disabling one module leaves base and other modules enabled */
        dummyFeature.setInstanceEnabled(false);
        assertFalse(dummyFeature.isInstanceEnabled());
        assertTrue(Sonoma.isEnabled());
        assertTrue(anotherDummyFeature.isInstanceEnabled());

        /* Enable back via main class. */
        Sonoma.setEnabled(true);
        assertTrue(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature, times(2)).setInstanceEnabled(true);
        verify(anotherDummyFeature).setInstanceEnabled(true);
        verify(channel, times(3)).setEnabled(true);

        /* Enable feature after the SDK is disabled. */
        Sonoma.setEnabled(false);
        assertFalse(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        dummyFeature.setInstanceEnabled(true);
        assertFalse(dummyFeature.isInstanceEnabled());
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString());
        assertFalse(Sonoma.isEnabled());
        verify(channel, times(2)).setEnabled(false);

        /* Disable back via main class. */
        Sonoma.setEnabled(false);
        assertFalse(Sonoma.isEnabled());
        for (SonomaFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        verify(channel, times(3)).setEnabled(false);

        /* Check factories / channel only once interactions. */
        verify(dummyFeature).getLogFactories();
        verify(dummyFeature).onChannelReady(any(Context.class), any(Channel.class));
        verify(anotherDummyFeature).getLogFactories();
        verify(anotherDummyFeature).onChannelReady(any(Context.class), any(Channel.class));
    }

    @Test
    public void enableBeforeInitializedTest() {
        /* Test isEnabled and setEnabled before initialize */
        assertFalse(Sonoma.isEnabled());
        Sonoma.setEnabled(true);
        assertFalse(Sonoma.isEnabled());
        PowerMockito.verifyStatic(times(3));
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString());
    }

    @Test
    public void disablePersisted() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        Channel channel = mock(Channel.class);
        Sonoma sonoma = Sonoma.getInstance();
        sonoma.setChannel(channel);

        /* Verify modules are enabled by default but core is disabled. */
        assertFalse(Sonoma.isEnabled());
        for (SonomaFeature feature : sonoma.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application, never()).registerActivityLifecycleCallbacks(feature);
        }

        /* Verify we can enable back. */
        Sonoma.setEnabled(true);
        assertTrue(Sonoma.isEnabled());
        for (SonomaFeature feature : sonoma.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }
    }

    @Test
    public void disablePersistedAndDisable() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        Channel channel = mock(Channel.class);
        Sonoma sonoma = Sonoma.getInstance();
        sonoma.setChannel(channel);

        /* Its already disabled so disable should have no effect on core but should disable features. */
        Sonoma.setEnabled(false);
        assertFalse(Sonoma.isEnabled());
        for (SonomaFeature feature : sonoma.getFeatures()) {
            assertFalse(feature.isInstanceEnabled());
            verify(application, never()).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }

        /* Verify we can enable the core back, should have no effect on features except registering the application life cycle callbacks. */
        Sonoma.setEnabled(true);
        assertTrue(Sonoma.isEnabled());
        for (SonomaFeature feature : sonoma.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }
    }

    @Test
    public void invalidFeatureTest() {
        Sonoma.start(application, DUMMY_APP_SECRET, InvalidFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(NoSuchMethodException.class));
    }

    @Test
    public void nullApplicationTest() {
        Sonoma.start(null, DUMMY_APP_SECRET, DummyFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString());
    }

    @Test
    public void nullAppIdentifierTest() {
        Sonoma.start(application, null, DummyFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString());
    }

    @Test
    public void emptyAppIdentifierTest() {
        Sonoma.start(application, "", DummyFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void tooShortAppIdentifierTest() {
        Sonoma.start(application, "too-short", DummyFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void invalidAppIdentifierTest() {
        Sonoma.start(application, "123xyz12-3xyz-123x-yz12-3xyz123xyz12", DummyFeature.class);
        PowerMockito.verifyStatic();
        SonomaLog.error(eq(Sonoma.LOG_TAG), anyString(), any(NumberFormatException.class));
    }

    @Test
    public void duplicateFeatureTest() {
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class, DummyFeature.class);

        /* Verify that only one module has been loaded and configured */
        assertEquals(1, Sonoma.getInstance().getFeatures().size());
    }

    @Test
    public void getInstallIdBeforeStart() {
        assertNull(Sonoma.getInstallId());
    }

    @Test
    public void setWrapperSdkTest() throws Exception {

        /* Setup  mocking. */
        DefaultChannel channel = mock(DefaultChannel.class);
        whenNew(DefaultChannel.class).withAnyArguments().thenReturn(channel);
        mockStatic(DeviceInfoHelper.class);

        /* Call method. */
        WrapperSdk wrapperSdk = new WrapperSdk();
        Sonoma.setWrapperSdk(wrapperSdk);

        /* Check propagation. */
        verifyStatic();
        DeviceInfoHelper.setWrapperSdk(wrapperSdk);

        /* Since the channel was not created when setting wrapper, no need to refresh channel after start. */
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        verify(channel, never()).invalidateDeviceCache();

        /* Update wrapper SDK and check channel refreshed. */
        wrapperSdk = new WrapperSdk();
        Sonoma.setWrapperSdk(wrapperSdk);
        verify(channel).invalidateDeviceCache();
    }


    @Test
    public void setDefaultLogLevelRelease() {
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        verifyStatic(never());
        SonomaLog.setLogLevel(anyInt());
    }

    @Test
    public void setDefaultLogLevelDebug() {
        Constants.APPLICATION_DEBUGGABLE = true;
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        verifyStatic();
        SonomaLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void dontSetDefaultLogLevel() {
        Constants.APPLICATION_DEBUGGABLE = true;
        Sonoma.setLogLevel(android.util.Log.VERBOSE);
        verifyStatic();
        SonomaLog.setLogLevel(android.util.Log.VERBOSE);
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        verifyStatic(never());
        SonomaLog.setLogLevel(android.util.Log.WARN);
    }

    @Test
    public void setServerUrl() throws Exception {

        /* Change server URL before start. */
        DefaultChannel channel = mock(DefaultChannel.class);
        whenNew(DefaultChannel.class).withAnyArguments().thenReturn(channel);
        String serverUrl = "http://mock";
        Sonoma.setServerUrl(serverUrl);

        /* No effect for now. */
        verify(channel, never()).setServerUrl(serverUrl);

        /* Start should propagate the server url. */
        Sonoma.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        verify(channel).setServerUrl(serverUrl);

        /* Change it after, should work immediately. */
        serverUrl = "http://mock2";
        Sonoma.setServerUrl(serverUrl);
        verify(channel).setServerUrl(serverUrl);
    }

    private static class DummyFeature extends AbstractSonomaFeature {

        private static DummyFeature sharedInstance;

        @SuppressWarnings("WeakerAccess")
        public static DummyFeature getInstance() {
            if (sharedInstance == null) {
                sharedInstance = spy(new DummyFeature());
            }
            return sharedInstance;
        }

        @Override
        protected String getGroupName() {
            return "group_dummy";
        }
    }

    private static class AnotherDummyFeature extends AbstractSonomaFeature {

        private static AnotherDummyFeature sharedInstance;

        @SuppressWarnings("WeakerAccess")
        public static AnotherDummyFeature getInstance() {
            if (sharedInstance == null) {
                sharedInstance = spy(new AnotherDummyFeature());
            }
            return sharedInstance;
        }

        @Override
        public Map<String, LogFactory> getLogFactories() {
            HashMap<String, LogFactory> logFactories = new HashMap<>();
            logFactories.put("mock", mock(LogFactory.class));
            return logFactories;
        }

        @Override
        protected String getGroupName() {
            return "group_another_dummy";
        }
    }

    private static class InvalidFeature extends AbstractSonomaFeature {

        @Override
        protected String getGroupName() {
            return "group_invalid";
        }
    }
}
