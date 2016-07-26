package avalanche.core;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

import avalanche.core.channel.AvalancheChannel;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
public class AbstractAvalancheFeatureTest {

    private AbstractAvalancheFeature feature;

    @Before
    public void setUp() {
        feature = new AbstractAvalancheFeature() {
            @Override
            protected String getGroupName() {
                return "group_test";
            }
        };
    }

    @Test
    public void onActivityCreated() {
        feature.onActivityCreated(null, null);
    }

    @Test
    public void onActivityStarted() {
        feature.onActivityStarted(null);
    }

    @Test
    public void onActivityResumed() {
        feature.onActivityResumed(null);
    }

    @Test
    public void onActivityPaused() {
        feature.onActivityPaused(null);
    }

    @Test
    public void onActivityStopped() {
        feature.onActivityStopped(null);
    }

    @Test
    public void onActivitySaveInstanceState() {
        feature.onActivitySaveInstanceState(null, null);
    }

    @Test
    public void onActivityDestroyed() {
        feature.onActivityDestroyed(null);
    }

    @Test
    public void enabled() {
        Assert.assertTrue(feature.isEnabled());
        feature.setEnabled(false);
        Assert.assertFalse(feature.isEnabled());
    }

    @Test
    public void getLogFactories() {
        Assert.assertNull(null, feature.getLogFactories());
    }

    @Test
    public void onChannelReady() {
        AvalancheChannel channel = Mockito.mock(AvalancheChannel.class);
        feature.onChannelReady(channel);

        Assert.assertSame(channel, feature.mChannel);
    }

    @Test
    public void getGroupName() {
        Assert.assertEquals("group_test", feature.getGroupName());
    }
}
