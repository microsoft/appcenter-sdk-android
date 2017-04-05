package com.microsoft.azure.mobile.analytics;

import android.content.Context;
import android.os.SystemClock;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.channel.AnalyticsListener;
import com.microsoft.azure.mobile.analytics.channel.SessionTracker;
import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.PageLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.StartSessionLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, StorageHelper.PreferencesStorage.class, MobileCenterLog.class, MobileCenter.class})
public class AnalyticsTest {

    private static final String ANALYTICS_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Analytics.getInstance().getServiceName();

    @Before
    public void setUp() {
        Analytics.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(MobileCenterLog.class);
        mockStatic(MobileCenter.class);
        when(MobileCenter.isEnabled()).thenReturn(true);

        /* First call to com.microsoft.azure.mobile.MobileCenter.isEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(ANALYTICS_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(ANALYTICS_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(ANALYTICS_ENABLED_KEY), anyBoolean());

        /* Pretend automatic page tracking is enabled by default, this will be the case if service becomes public. */
        // TODO remove that after service is public
        assertFalse(Analytics.isAutoPageTrackingEnabled());
        Analytics.setAutoPageTrackingEnabled(true);
    }

    @Test
    public void singleton() {
        Assert.assertSame(Analytics.getInstance(), Analytics.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Analytics.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(StartSessionLog.TYPE) instanceof StartSessionLogFactory);
        assertTrue(factories.remove(PageLog.TYPE) instanceof PageLogFactory);
        assertTrue(factories.remove(EventLog.TYPE) instanceof EventLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void notInit() {

        /* Just check log is discarded without throwing any exception. */
        Analytics.trackEvent("test");
        Analytics.trackEvent("test", new HashMap<String, String>());
        Analytics.trackPage("test");
        Analytics.trackPage("test", new HashMap<String, String>());

        verifyStatic(times(4));
        MobileCenterLog.error(eq(MobileCenter.LOG_TAG), anyString());
    }

    private void activityResumed(final String expectedName, android.app.Activity activity) {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        analytics.onActivityResumed(activity);
        analytics.onActivityPaused(activity);
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return expectedName.equals(pageLog.getName());
                }
                return false;
            }
        }), eq(analytics.getGroupName()));
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
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        analytics.onActivityResumed(new MyActivity());
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof StartSessionLog;
            }
        }), anyString());
        verify(channel, never()).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof PageLog;
            }
        }), anyString());
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
        }), eq(analytics.getGroupName()));
    }

    @Test
    public void trackEvent() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
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
        }), eq(analytics.getGroupName()));
    }

    @Test
    public void testTrackEvent() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        Analytics.trackEvent(null, null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent("", null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent(" ", null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent(generateString(257, '*'), null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent(generateString(256, '*'), null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent("eventName", new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(65, '*'), null);
            put("1", null);
            put("2", generateString(65, '*'));
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getProperties().size() == 0;
                }
                return false;
            }
        }), anyString());
        reset(channel);
        final String validMapItem = "valid";
        Analytics.trackEvent("eventName", new HashMap<String, String>() {{
            for (int i = 0; i < 10; i++) {
                put(validMapItem + i, validMapItem);
            }
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getProperties().size() == 5;
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void testTrackPage() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        Analytics.trackPage(null, null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage("", null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage(" ", null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage(generateString(257, '*'), null);
        verify(channel, never()).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage(generateString(256, '*'), null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage("pageName", new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(65, '*'), null);
            put("1", null);
            put("2", generateString(65, '*'));
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return pageLog.getProperties().size() == 0;
                }
                return false;
            }
        }), anyString());
        reset(channel);
        final String validMapItem = "valid";
        Analytics.trackPage("pageName", new HashMap<String, String>() {{
            for (int i = 0; i < 10; i++) {
                put(validMapItem + i, validMapItem);
            }
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return pageLog.getProperties().size() == 5;
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void setEnabled() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        assertTrue(Analytics.isEnabled());
        Analytics.setEnabled(true);
        assertTrue(Analytics.isEnabled());
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled());
        analytics.onStarted(mock(Context.class), "", channel);
        verify(channel).clear(analytics.getGroupName());
        verify(channel).removeGroup(eq(analytics.getGroupName()));
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());
        verifyNoMoreInteractions(channel);

        /* Enable back, testing double calls. */
        Analytics.setEnabled(true);
        assertTrue(Analytics.isEnabled());
        Analytics.setEnabled(true);
        assertTrue(Analytics.isEnabled());
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyInt(), anyInt(), any(Channel.GroupListener.class));
        verify(channel).addListener(any(SessionTracker.class));
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Disable again. */
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled());
        /* clear and removeGroup are being called in this test method. */
        verify(channel, times(2)).clear(analytics.getGroupName());
        verify(channel, times(2)).removeGroup(eq(analytics.getGroupName()));
        verify(channel).removeListener(any(SessionTracker.class));
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());
        verifyNoMoreInteractions(channel);

        /* Verify session state has been cleared. */
        verifyStatic();
        StorageHelper.PreferencesStorage.remove("sessions");
    }

    @Test
    public void startSessionAfterUserApproval() {

        /*
         * Disable analytics while in background to set up the initial condition
         * simulating the optin use case.
         */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        Analytics.setEnabled(false);

        /* App in foreground: no log yet, we are disabled. */
        analytics.onActivityResumed(new Activity());
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Enable: start session sent retroactively. */
        Analytics.setEnabled(true);
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof StartSessionLog;
            }
        }), eq(analytics.getGroupName()));
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof PageLog;
            }
        }), eq(analytics.getGroupName()));

        /* Go background. */
        analytics.onActivityPaused(new Activity());

        /* Disable/enable: nothing happens on background. */
        Analytics.setEnabled(false);
        Analytics.setEnabled(true);

        /* No additional log. */
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()));
    }

    @Test
    public void startSessionAfterUserApprovalWeakReference() {

        /*
         * Disable analytics while in background to set up the initial condition
         * simulating the optin use case.
         */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarted(mock(Context.class), "", channel);
        Analytics.setEnabled(false);

        /* App in foreground: no log yet, we are disabled. */
        analytics.onActivityResumed(new Activity());
        System.gc();
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Enable: start session not sent retroactively, weak reference lost. */
        Analytics.setEnabled(true);
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));
    }

    @Test
    public void testGetChannelListener() throws IOException, ClassNotFoundException {

        final EventLog testEventLog = new EventLog();
        testEventLog.setId(UUID.randomUUID());
        testEventLog.setName("name");
        final Exception testException = new Exception("test exception message");

        Analytics.setListener(new AnalyticsListener() {
            @Override
            public void onBeforeSending(Log log) {
                assertEquals(log, testEventLog);
            }

            @Override
            public void onSendingSucceeded(Log log) {
                assertEquals(log, testEventLog);
            }

            @Override
            public void onSendingFailed(Log log, Exception e) {
                assertEquals(log, testEventLog);
                assertEquals(e, testException);
            }
        });

        Channel.GroupListener listener = Analytics.getInstance().getChannelListener();
        listener.onBeforeSending(testEventLog);
        listener.onSuccess(testEventLog);
        listener.onFailure(testEventLog, testException);
    }

    @Test
    public void testAnalyticsListenerNull() {
        AnalyticsListener analyticsListener = mock(AnalyticsListener.class);
        Analytics.setListener(analyticsListener);
        Analytics.setListener(null);
        final EventLog testEventLog = new EventLog();
        testEventLog.setId(UUID.randomUUID());
        testEventLog.setName("name");
        final Exception testException = new Exception("test exception message");
        Channel.GroupListener listener = Analytics.getInstance().getChannelListener();
        listener.onBeforeSending(testEventLog);
        listener.onSuccess(testEventLog);
        listener.onFailure(testEventLog, testException);
        verify(analyticsListener, never()).onBeforeSending(any(EventLog.class));
        verify(analyticsListener, never()).onSendingSucceeded(any(EventLog.class));
        verify(analyticsListener, never()).onSendingFailed(any(EventLog.class), any(Exception.class));
    }

    /**
     * Generates string of arbitrary length with contents composed of single character.
     *
     * @param length length of the resulting string.
     * @param charToFill character to compose string of.
     * @return <code>String<code/> of desired length.
     */
    private String generateString(int length, char charToFill) {
        return new String(new char[length]).replace('\0', charToFill);
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
