package avalanche.core;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("unused")
public class AvalancheFeatureTest {

    @Test
    public void featureEnabledTest() {

        AvalancheFeature feature = new TestAvalancheFeature();
        // Feature should be enabled by default
        assertTrue(feature.isEnabled());

        // Test disabling the feature
        feature.setEnabled(false);
        assertFalse(feature.isEnabled());

        // Test re-enabling the feature
        feature.setEnabled(true);
        assertTrue(feature.isEnabled());
    }

    static class TestAvalancheFeature extends AbstractAvalancheFeature {

    }

}
