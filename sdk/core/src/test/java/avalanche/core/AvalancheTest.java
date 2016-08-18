package avalanche.core;

import android.app.Application;
import android.content.ContentValues;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.IdHelper;
import avalanche.core.utils.StorageHelper;

import static avalanche.core.utils.PrefStorageConstants.KEY_ENABLED;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
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

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Constants.class, AvalancheLog.class, StorageHelper.class, StorageHelper.PreferencesStorage.class, IdHelper.class, StorageHelper.DatabaseStorage.class})
public class AvalancheTest {

    private static final String DUMMY_APP_SECRET = "123e4567-e89b-12d3-a456-426655440000";

    private Application application;

    @Mock
    private Iterator<ContentValues> mDataBaseScannerIterator;

    @Before
    public void setUp() {
        Avalanche.unsetInstance();
        DummyFeature.sharedInstance = null;
        AnotherDummyFeature.sharedInstance = null;

        application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);

        mockStatic(Constants.class);
        mockStatic(AvalancheLog.class);
        mockStatic(StorageHelper.class);
        mockStatic(StorageHelper.PreferencesStorage.class);
        mockStatic(IdHelper.class);
        mockStatic(StorageHelper.DatabaseStorage.class);

        /* First call to avalanche.isInstanceEnabled shall return true, initial state. */
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

    @Test
    public void singleton() {
        assertNotNull(Avalanche.getInstance());
        assertSame(Avalanche.getInstance(), Avalanche.getInstance());
    }

    @Test
    public void nullVarargClass() {
        Avalanche.start(application, DUMMY_APP_SECRET, (Class<? extends AvalancheFeature>) null);

        // Verify that no modules have been auto-loaded since none are configured for this
        assertEquals(0, Avalanche.getInstance().getFeatures().size());
        assertEquals(application, Avalanche.getInstance().getApplication());
    }

    @Test
    public void nullVarargFeatures() {
        Avalanche.start(application, DUMMY_APP_SECRET, (AvalancheFeature) null);

        // Verify that no modules have been auto-loaded since none are configured for this
        assertEquals(0, Avalanche.getInstance().getFeatures().size());
        assertEquals(application, Avalanche.getInstance().getApplication());
    }

    @Test
    public void avalancheUseDummyFeatureTest() {
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class);

        // Verify that single module has been loaded and configured
        assertEquals(1, Avalanche.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Avalanche.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(AvalancheChannel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void avalancheUseFeaturesTwiceTest() {
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class);
        Avalanche.start(application, DUMMY_APP_SECRET, AnotherDummyFeature.class); //ignored

        // Verify that single module has been loaded and configured
        assertEquals(1, Avalanche.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Avalanche.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(any(Context.class), notNull(AvalancheChannel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void avalancheUseDummyFeaturesTest() {
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);

        // Verify that the right amount of modules have been loaded and configured
        assertEquals(2, Avalanche.getInstance().getFeatures().size());
        {
            assertTrue(Avalanche.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(AvalancheChannel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Avalanche.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(any(Context.class), notNull(AvalancheChannel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void avalancheFeaturesEnableTest() {
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Avalanche avalanche = Avalanche.getInstance();
        avalanche.setChannel(channel);

        // Verify modules are enabled by default
        Set<AvalancheFeature> features = avalanche.getFeatures();
        assertTrue(Avalanche.isEnabled());
        DummyFeature dummyFeature = DummyFeature.getInstance();
        AnotherDummyFeature anotherDummyFeature = AnotherDummyFeature.getInstance();
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }

        // Explicit set enabled should not change that
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature, never()).setInstanceEnabled(anyBoolean());
        verify(anotherDummyFeature, never()).setInstanceEnabled(anyBoolean());
        verify(channel).setEnabled(true);

        // Verify disabling base disables all modules
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        verify(dummyFeature).setInstanceEnabled(false);
        verify(anotherDummyFeature).setInstanceEnabled(false);
        verify(application).unregisterActivityLifecycleCallbacks(dummyFeature);
        verify(application).unregisterActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel).setEnabled(false);

        // Verify re-enabling base re-enables all modules
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature).setInstanceEnabled(true);
        verify(anotherDummyFeature).setInstanceEnabled(true);
        verify(application, times(2)).registerActivityLifecycleCallbacks(dummyFeature);
        verify(application, times(2)).registerActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel, times(2)).setEnabled(true);

        // Verify that disabling one module leaves base and other modules enabled
        dummyFeature.setInstanceEnabled(false);
        assertFalse(dummyFeature.isInstanceEnabled());
        assertTrue(Avalanche.isEnabled());
        assertTrue(anotherDummyFeature.isInstanceEnabled());

        /* Enable back via main class. */
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isInstanceEnabled());
        }
        verify(dummyFeature, times(2)).setInstanceEnabled(true);
        verify(anotherDummyFeature).setInstanceEnabled(true);
        verify(channel, times(3)).setEnabled(true);

        /* Enable 1 feature only after disable all. */
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        dummyFeature.setInstanceEnabled(true);
        assertTrue(dummyFeature.isInstanceEnabled());
        assertFalse(Avalanche.isEnabled());
        assertFalse(anotherDummyFeature.isInstanceEnabled());
        verify(channel, times(2)).setEnabled(false);

        /* Disable back via main class. */
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isInstanceEnabled());
        }
        verify(channel, times(3)).setEnabled(false);

        /* Check factories / channel only once interactions. */
        verify(dummyFeature).getLogFactories();
        verify(dummyFeature).onChannelReady(any(Context.class), any(AvalancheChannel.class));
        verify(anotherDummyFeature).getLogFactories();
        verify(anotherDummyFeature).onChannelReady(any(Context.class), any(AvalancheChannel.class));
    }

    @Test
    public void disablePersisted() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Avalanche avalanche = Avalanche.getInstance();
        avalanche.setChannel(channel);

        /* Verify modules are enabled by default but core is disabled. */
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : avalanche.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application, never()).registerActivityLifecycleCallbacks(feature);
        }

        /* Verify we can enable back. */
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : avalanche.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }
    }

    @Test
    public void disablePersistedAndDisable() {
        when(StorageHelper.PreferencesStorage.getBoolean(KEY_ENABLED, true)).thenReturn(false);
        Avalanche.start(application, DUMMY_APP_SECRET, DummyFeature.class, AnotherDummyFeature.class);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Avalanche avalanche = Avalanche.getInstance();
        avalanche.setChannel(channel);

        /* Its already disabled so disable should have no effect on core but should disable features. */
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : avalanche.getFeatures()) {
            assertFalse(feature.isInstanceEnabled());
            verify(application, never()).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }

        /* Verify we can enable the core back, should have no effect on features except registering the application life cycle callbacks. */
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : avalanche.getFeatures()) {
            assertTrue(feature.isInstanceEnabled());
            verify(application).registerActivityLifecycleCallbacks(feature);
            verify(application, never()).unregisterActivityLifecycleCallbacks(feature);
        }
    }

    @Test
    public void avalancheInvalidFeatureTest() {
        Avalanche.start(application, DUMMY_APP_SECRET, InvalidFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(NoSuchMethodException.class));
    }

    @Test
    public void avalancheNullApplicationTest() {
        Avalanche.start(null, DUMMY_APP_SECRET, DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString());
    }

    @Test
    public void avalancheNullAppIdentifierTest() {
        Avalanche.start(application, null, DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString());
    }

    @Test
    public void avalancheEmptyAppIdentifierTest() {
        Avalanche.start(application, "", DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void avalancheTooShortAppIdentifierTest() {
        Avalanche.start(application, "too-short", DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void avalancheInvalidAppIdentifierTest() {
        Avalanche.start(application, "123xyz12-3xyz-123x-yz12-3xyz123xyz12", DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(NumberFormatException.class));
    }

    private static class DummyFeature extends AbstractAvalancheFeature {

        private static DummyFeature sharedInstance;

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

    private static class AnotherDummyFeature extends AbstractAvalancheFeature {

        private static AnotherDummyFeature sharedInstance;

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

    private static class InvalidFeature extends AbstractAvalancheFeature {

        @Override
        protected String getGroupName() {
            return "group_invalid";
        }
    }
}
