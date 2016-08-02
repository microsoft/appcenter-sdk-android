package avalanche.core;

import android.app.Application;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.IdHelper;
import avalanche.core.utils.StorageHelper;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({Constants.class, AvalancheLog.class, StorageHelper.class, IdHelper.class})
public class AvalancheTest {

    private static final String DUMMY_APP_KEY = "123e4567-e89b-12d3-a456-426655440000";

    private Application application;

    @Before
    public void setUp() {
        Avalanche.unsetInstance();
        DummyFeature.sharedInstance = null;
        AnotherDummyFeature.sharedInstance = null;

        application = mock(Application.class);
        when(application.getApplicationContext()).thenReturn(application);

        PowerMockito.mockStatic(Constants.class);
        PowerMockito.mockStatic(AvalancheLog.class);
        PowerMockito.mockStatic(StorageHelper.class);
        PowerMockito.mockStatic(IdHelper.class);
    }

    @Test
    public void singleton() {
        assertNotNull(Avalanche.getInstance());
        assertSame(Avalanche.getInstance(), Avalanche.getInstance());
    }

    @Test
    public void nullVarargClass() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, (Class<? extends AvalancheFeature>) null);

        // Verify that no modules have been auto-loaded since none are configured for this
        assertEquals(0, Avalanche.getInstance().getFeatures().size());
        assertEquals(application, Avalanche.getInstance().getApplication());
    }

    @Test
    public void nullVarargFeatures() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, (AvalancheFeature) null);

        // Verify that no modules have been auto-loaded since none are configured for this
        assertEquals(0, Avalanche.getInstance().getFeatures().size());
        assertEquals(application, Avalanche.getInstance().getApplication());
    }

    @Test
    public void avalancheUseDummyFeatureTest() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, DummyFeature.class);

        // Verify that single module has been loaded and configured
        assertEquals(1, Avalanche.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Avalanche.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(notNull(AvalancheChannel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void avalancheUseFeaturesTwiceTest() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, DummyFeature.class);
        Avalanche.useFeatures(application, DUMMY_APP_KEY, AnotherDummyFeature.class); //ignored

        // Verify that single module has been loaded and configured
        assertEquals(1, Avalanche.getInstance().getFeatures().size());
        DummyFeature feature = DummyFeature.getInstance();
        assertTrue(Avalanche.getInstance().getFeatures().contains(feature));
        verify(feature).getLogFactories();
        verify(feature).onChannelReady(notNull(AvalancheChannel.class));
        verify(application).registerActivityLifecycleCallbacks(feature);
    }

    @Test
    public void avalancheUseDummyFeaturesTest() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, DummyFeature.class, AnotherDummyFeature.class);

        // Verify that the right amount of modules have been loaded and configured
        assertEquals(2, Avalanche.getInstance().getFeatures().size());
        {
            assertTrue(Avalanche.getInstance().getFeatures().contains(DummyFeature.getInstance()));
            verify(DummyFeature.getInstance()).getLogFactories();
            verify(DummyFeature.getInstance()).onChannelReady(notNull(AvalancheChannel.class));
            verify(application).registerActivityLifecycleCallbacks(DummyFeature.getInstance());
        }
        {
            assertTrue(Avalanche.getInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
            verify(AnotherDummyFeature.getInstance()).getLogFactories();
            verify(AnotherDummyFeature.getInstance()).onChannelReady(notNull(AvalancheChannel.class));
            verify(application).registerActivityLifecycleCallbacks(AnotherDummyFeature.getInstance());
        }
    }

    @Test
    public void avalancheFeaturesEnableTest() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, DummyFeature.class, AnotherDummyFeature.class);
        AvalancheChannel channel = mock(AvalancheChannel.class);
        Avalanche avalanche = Avalanche.getInstance();
        avalanche.setChannel(channel);

        // Verify modules are enabled by default
        Set<AvalancheFeature> features = avalanche.getFeatures();
        assertTrue(Avalanche.isEnabled());
        DummyFeature dummyFeature = DummyFeature.getInstance();
        AnotherDummyFeature anotherDummyFeature = AnotherDummyFeature.getInstance();
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }

        // Explicit set enabled should not change that
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }
        verify(dummyFeature, never()).setEnabled(anyBoolean());
        verify(anotherDummyFeature, never()).setEnabled(anyBoolean());
        verify(channel).setEnabled(true);

        // Verify disabling base disables all modules
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isEnabled());
        }
        verify(dummyFeature).setEnabled(false);
        verify(anotherDummyFeature).setEnabled(false);
        verify(application).unregisterActivityLifecycleCallbacks(dummyFeature);
        verify(application).unregisterActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel).setEnabled(false);

        // Verify re-enabling base re-enables all modules
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }
        verify(dummyFeature).setEnabled(true);
        verify(anotherDummyFeature).setEnabled(true);
        verify(application, times(2)).registerActivityLifecycleCallbacks(dummyFeature);
        verify(application, times(2)).registerActivityLifecycleCallbacks(anotherDummyFeature);
        verify(channel, times(2)).setEnabled(true);

        // Verify that disabling one module leaves base and other modules enabled
        dummyFeature.setEnabled(false);
        assertFalse(dummyFeature.isEnabled());
        assertTrue(Avalanche.isEnabled());
        assertTrue(anotherDummyFeature.isEnabled());

        /* Enable back via main class. */
        Avalanche.setEnabled(true);
        assertTrue(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }
        verify(dummyFeature, times(2)).setEnabled(true);
        verify(anotherDummyFeature).setEnabled(true);
        verify(channel, times(3)).setEnabled(true);

        /* Enable 1 feature only after disable all. */
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isEnabled());
        }
        dummyFeature.setEnabled(true);
        assertTrue(dummyFeature.isEnabled());
        assertFalse(Avalanche.isEnabled());
        assertFalse(anotherDummyFeature.isEnabled());
        verify(channel, times(2)).setEnabled(false);

        /* Disable back via main class. */
        Avalanche.setEnabled(false);
        assertFalse(Avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isEnabled());
        }
        verify(channel, times(3)).setEnabled(false);

        /* Check factories / channel only once interactions. */
        verify(dummyFeature).getLogFactories();
        verify(dummyFeature).onChannelReady(any(AvalancheChannel.class));
        verify(anotherDummyFeature).getLogFactories();
        verify(anotherDummyFeature).onChannelReady(any(AvalancheChannel.class));
    }

    @Test
    public void avalancheInvalidFeatureTest() {
        Avalanche.useFeatures(application, DUMMY_APP_KEY, InvalidFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(NoSuchMethodException.class));
    }

    @Test
    public void avalancheNullApplicationTest() {
        Avalanche.useFeatures(null, DUMMY_APP_KEY, DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString());
    }

    @Test
    public void avalancheNullAppIdentifierTest() {
        Avalanche.useFeatures(application, null, DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString());
    }

    @Test
    public void avalancheEmptyAppIdentifierTest() {
        Avalanche.useFeatures(application, "", DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void avalancheTooShortAppIdentifierTest() {
        Avalanche.useFeatures(application, "too-short", DummyFeature.class);
        PowerMockito.verifyStatic();
        AvalancheLog.error(anyString(), any(IllegalArgumentException.class));
    }

    @Test
    public void avalancheInvalidAppIdentifierTest() {
        Avalanche.useFeatures(application, "123xyz12-3xyz-123x-yz12-3xyz123xyz12", DummyFeature.class);
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
