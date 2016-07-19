package avalanche.analytics;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.HashMap;
import java.util.Map;

import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.LogFactory;

import static avalanche.core.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

@SuppressWarnings("WrongConstant")
public class AnalyticsTest {

    @Before
    public void setUp() {
        Analytics.unsetInstance();
    }

    @Test
    public void singleton() {
        Assert.assertSame(Analytics.getInstance(), Analytics.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Analytics.getInstance().getLogFactories();
        assertTrue(factories.remove(PageLog.TYPE) instanceof PageLogFactory);
        assertTrue(factories.remove(EventLog.TYPE) instanceof EventLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void notInit() {

        /* Just check log is discarded without throwing any exception. */
        Analytics.trackPage("test", new HashMap<String, String>());
    }

    private void activityResumed(final String expectedName, android.app.Activity activity) {
        Analytics analytics = Analytics.getInstance();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        analytics.onActivityResumed(activity);
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return expectedName.equals(pageLog.getName());
                }
                return false;
            }
        }), eq(ANALYTICS_GROUP));
    }

    @Test
    public void activityResumedWithSuffix() {
        activityResumed("My", new MyActivity());
    }

    @Test
    public void activityResumedNoSuffix() {
        activityResumed("SomeScreen", new SomeScreen());
    }

    @Test
    public void activityResumedNamedActivity() {
        activityResumed("Activity", new Activity());
    }

    @Test
    public void disableAutomaticPageTracking() {
        Analytics analytics = Analytics.getInstance();
        assertTrue(Analytics.isAutoPageTrackingEnabled());
        Analytics.setAutoPageTrackingEnabled(false);
        assertFalse(Analytics.isAutoPageTrackingEnabled());
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        analytics.onActivityResumed(new MyActivity());
        verify(channel, never()).enqueue(any(Log.class), anyString());
        Analytics.setAutoPageTrackingEnabled(true);
        assertTrue(Analytics.isAutoPageTrackingEnabled());
        analytics.onActivityResumed(new SomeScreen());
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
    public void trackEvent() {
        Analytics analytics = Analytics.getInstance();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.onChannelReady(channel);
        final String name = "testEvent";
        final HashMap<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        Analytics.trackEvent(name, properties);
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

    @Test
    public void setEnabled() {
        Analytics analytics = Analytics.getInstance();
        AvalancheChannel channel = mock(AvalancheChannel.class);
        analytics.setEnabled(false);
        analytics.onChannelReady(channel);
        Analytics.trackEvent("test", null);
        Analytics.trackPage("test", null);
        verifyZeroInteractions(channel);

        /* Enable back. */
        analytics.setEnabled(true);
        Analytics.trackEvent("test", null);
        Analytics.trackPage("test", null);
        verify(channel, times(2)).enqueue(any(Log.class), eq(ANALYTICS_GROUP));

        /* Disable again. */
        analytics.setEnabled(false);
        Analytics.trackEvent("test", null);
        Analytics.trackPage("test", null);
        verifyNoMoreInteractions(channel);
    }

    /**
     * Activity with page name automatically resolving to "My" (no "Activity" suffix).
     */
    private static class MyActivity extends android.app.Activity {
    }

    /**
     * Activity with page name automatically resolving to "SomeScreen".
     */
    private static class SomeScreen extends android.app.Activity {
    }

    /**
     * Activity with page name automatically resolving to "Activity", because name == suffix.
     */
    private static class Activity extends android.app.Activity {
    }
}
