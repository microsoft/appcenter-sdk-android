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
import static junit.framework.Assert.assertNotNull;
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

        Avalanche avalanche = Avalanche.getSharedInstance();
        Set<AvalancheFeature> features = avalanche.getFeatures();

        assertNotNull(features);
        assertEquals(0, features.size());
    }

    @Test
    public void avalancheUseDummyFeatureTest() {
        Avalanche.useFeatures(application, DummyFeature.class);

        Avalanche avalanche = Avalanche.getSharedInstance();
        Set<AvalancheFeature> features = avalanche.getFeatures();

        assertNotNull(features);
        assertEquals(1, features.size());
    }

    @Test
    public void avalancheUseDummyFeaturesTest() {
        Avalanche.useFeatures(application, DummyFeature.class, AnotherDummyFeature.class);

        Avalanche avalanche = Avalanche.getSharedInstance();
        Set<AvalancheFeature> features = avalanche.getFeatures();

        assertNotNull(features);
        assertEquals(2, features.size());
    }

    static class DummyFeature extends DefaultAvalancheFeature {

        public static DummyFeature getInstance() {
            return new DummyFeature();
        }
    }

    static class AnotherDummyFeature extends DefaultAvalancheFeature {

        public static AnotherDummyFeature getInstance() {
            return new AnotherDummyFeature();
        }
    }


}
