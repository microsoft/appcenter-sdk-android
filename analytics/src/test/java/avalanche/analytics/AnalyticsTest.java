package avalanche.analytics;

import android.app.Activity;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.Map;

import avalanche.analytics.ingestion.models.EndSessionLog;
import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.json.EndSessionLogFactory;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.base.channel.AvalancheChannel;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.json.LogFactory;

import static avalanche.base.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AnalyticsTest {

    @Test
    public void singleton() {
        Assert.assertNotSame(new Analytics(), Analytics.getInstance());
        Assert.assertSame(Analytics.getInstance(), Analytics.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = new Analytics().getLogFactories();
        assertEquals(3, factories.size());
        assertTrue(factories.get(PageLog.TYPE) instanceof PageLogFactory);
        assertTrue(factories.get(EventLog.TYPE) instanceof EventLogFactory);
        assertTrue(factories.get(EndSessionLog.TYPE) instanceof EndSessionLogFactory);
    }

    @Test
    public void activityResumedNoSuffix() {
        Analytics analytics = new Analytics();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        analytics.onActivityResumed(new MyActivity());
        //noinspection WrongConstant (well its not a wrong constant but something is odd with compiler here)
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return "My".equals(pageLog.getName());
                }
                return false;
            }
        }), eq(ANALYTICS_GROUP));
    }

    @Test
    public void activityResumedWithSuffix() {
        Analytics analytics = new Analytics();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        analytics.onActivityResumed(new SomeScreen());
        //noinspection WrongConstant (well its not a wrong constant but something is odd with compiler here)
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return "SomeScreen".equals(pageLog.getName());
                }
                return false;
            }
        }), eq(ANALYTICS_GROUP));
    }

    @Test
    public void sendEvent() {
        Analytics analytics = new Analytics();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        final String name = "testEvent";
        final HashMap<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        analytics.sendEvent(name, properties);
        //noinspection WrongConstant (well its not a wrong constant but something is odd with compiler here)
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return name.equals(eventLog.getName()) && properties.equals(eventLog.getProperties()) && eventLog.getId() != null;
                }
                return false;
            }
        }), eq(ANALYTICS_GROUP));
    }

    private static class SomeScreen extends Activity {
    }

    private static class MyActivity extends Activity {
    }
}
