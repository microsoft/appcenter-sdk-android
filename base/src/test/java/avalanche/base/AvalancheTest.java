package avalanche.base;

import android.app.Application;
import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Set;

import avalanche.base.utils.Util;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Util.class, Constants.class})
public class AvalancheTest {

    private Application application;

    @Before
    public void setUp() {
        application = mock(Application.class);

        PowerMockito.mockStatic(Util.class);
        Mockito.when(Util.getAppIdentifier(Mockito.any(Context.class))).thenReturn("");

        PowerMockito.mockStatic(Constants.class);
    }

    @Test
    public void avalancheInstanceTest() {
        assertNotNull(Avalanche.getSharedInstance());
    }

    @Test
    public void avalancheUseDefaultFeaturesTest() {
        Avalanche.useFeatures(application);

        assertEquals(0, Avalanche.getSharedInstance().getFeatures().size());
        assertEquals(application, Avalanche.getSharedInstance().getApplication());
    }

    @Test
    public void avalancheUseDummyFeatureTest() {
        Avalanche.useFeatures(application, DummyFeature.class);

        assertEquals(1, Avalanche.getSharedInstance().getFeatures().size());
        assertTrue(Avalanche.getSharedInstance().getFeatures().contains(DummyFeature.getInstance()));
    }

    @Test
    public void avalancheUseDummyFeaturesTest() {
        Avalanche.useFeatures(application, DummyFeature.class, AnotherDummyFeature.class);

        assertEquals(2, Avalanche.getSharedInstance().getFeatures().size());
    }

    @Test
    public void avalancheAddFeaturesTest() {
        Avalanche.useFeatures(application);

        assertEquals(0, Avalanche.getSharedInstance().getFeatures().size());

        Avalanche.getSharedInstance().addFeature(DummyFeature.getInstance());
        assertEquals(1, Avalanche.getSharedInstance().getFeatures().size());
        assertTrue(Avalanche.getSharedInstance().getFeatures().contains(DummyFeature.getInstance()));

        Avalanche.getSharedInstance().addFeature(DummyFeature.getInstance());
        assertEquals(1, Avalanche.getSharedInstance().getFeatures().size());

        Avalanche.getSharedInstance().addFeature(AnotherDummyFeature.getInstance());
        assertEquals(2, Avalanche.getSharedInstance().getFeatures().size());
        assertTrue(Avalanche.getSharedInstance().getFeatures().contains(DummyFeature.getInstance()));
        assertTrue(Avalanche.getSharedInstance().getFeatures().contains(AnotherDummyFeature.getInstance()));
    }

    @Test
    public void avalancheFeaturesEnableTest() {
        Avalanche.useFeatures(application, DummyFeature.class, AnotherDummyFeature.class);

        Avalanche avalanche = Avalanche.getSharedInstance();
        Set<AvalancheFeature> features = avalanche.getFeatures();

        assertTrue(avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }

        avalanche.setEnabled(false);

        assertFalse(avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertFalse(feature.isEnabled());
        }

        avalanche.setEnabled(true);
        assertTrue(avalanche.isEnabled());
        for (AvalancheFeature feature : features) {
            assertTrue(feature.isEnabled());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void avalancheInvalidFeatureTest() {
        Avalanche.useFeatures(application, InvalidFeature.class);
    }

    static class DummyFeature extends DefaultAvalancheFeature {

        private static DummyFeature sharedInstance = null;

        public static DummyFeature getInstance() {
            if (sharedInstance == null) {
                sharedInstance = new DummyFeature();
            }
            return sharedInstance;
        }
    }

    static class AnotherDummyFeature extends DefaultAvalancheFeature {

        private static AnotherDummyFeature sharedInstance;

        public static AnotherDummyFeature getInstance() {
            if (sharedInstance == null) {
                sharedInstance = new AnotherDummyFeature();
            }
            return sharedInstance;
        }
    }

    static class InvalidFeature extends DefaultAvalancheFeature {

    }


}
