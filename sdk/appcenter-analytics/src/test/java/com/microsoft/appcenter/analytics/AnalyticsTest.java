package com.microsoft.appcenter.analytics;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.channel.AnalyticsListener;
import com.microsoft.appcenter.analytics.channel.AnalyticsValidator;
import com.microsoft.appcenter.analytics.channel.SessionTracker;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.json.CommonSchemaEventLogFactory;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class AnalyticsTest extends AbstractAnalyticsTest {

    @Test
    public void singleton() {
        Assert.assertSame(Analytics.getInstance(), Analytics.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertFalse(Analytics.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Analytics.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(StartSessionLog.TYPE) instanceof StartSessionLogFactory);
        assertTrue(factories.remove(PageLog.TYPE) instanceof PageLogFactory);
        assertTrue(factories.remove(EventLog.TYPE) instanceof EventLogFactory);
        assertTrue(factories.remove(CommonSchemaEventLog.TYPE) instanceof CommonSchemaEventLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void notInit() {

        /* Just check log is discarded without throwing any exception. */
        Analytics.trackEvent("test");
        Analytics.trackEvent("test", new HashMap<String, String>());
        Analytics.trackPage("test");
        Analytics.trackPage("test", new HashMap<String, String>());
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("t1");
        target.trackEvent("test");
        target.trackEvent("test", new HashMap<String, String>());
        target.getTransmissionTarget("t2").trackEvent("test");
        target.getTransmissionTarget("t2").trackEvent("test", new HashMap<String, String>());

        /* Verify we just get an error every time. */
        verifyStatic(times(8));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    private void activityResumed(final String expectedName, android.app.Activity activity) {

        /*
         * Before start, calling onActivityResume is ignored.
         * In reality it never happens, it means someone is messing with internals directly.
         */
        Analytics analytics = Analytics.getInstance();
        analytics.onActivityResumed(new Activity());
        assertNull(analytics.getCurrentActivity());
        verifyStatic();
        AppCenterLog.error(anyString(), anyString());
        analytics.onActivityPaused(new Activity());
        verifyStatic(times(2));
        AppCenterLog.error(anyString(), anyString());

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Test resume/pause. */
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
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
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
    public void trackEventFromApp() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackEvent("eventName");
        verify(channel).enqueue(isA(EventLog.class), anyString());
    }

    @Test
    public void trackEventFromLibrary() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, null, null, false);

        /* Static track call forbidden if app didn't start Analytics. */
        Analytics.trackEvent("eventName");
        verify(channel, never()).enqueue(isA(EventLog.class), anyString());

        /* It works from a target. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("t");
        target.trackEvent("eventName");
        verify(channel).enqueue(isA(EventLog.class), anyString());

        /* It works from a child target. */
        target.getTransmissionTarget("t2").trackEvent("eventName");
        verify(channel, times(2)).enqueue(isA(EventLog.class), anyString());
    }

    @Test
    public void trackPageFromApp() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackPage("pageName");
        verify(channel).enqueue(isA(PageLog.class), anyString());
    }

    @Test
    public void trackPageFromLibrary() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, false);

        /* Page tracking does not work from library. */
        Analytics.trackPage("pageName");
        verify(channel, never()).enqueue(isA(PageLog.class), anyString());
    }

    @Test
    public void setEnabled() throws InterruptedException {

        /* Before start it does not work to change state, it's disabled. */
        Analytics analytics = Analytics.getInstance();
        Analytics.setEnabled(true);
        assertFalse(Analytics.isEnabled().get());
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled().get());

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).removeGroup(eq(analytics.getGroupName()));
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).addListener(isA(AnalyticsValidator.class));

        /* Now we can see the service enabled. */
        assertTrue(Analytics.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Analytics.setEnabled(false).get();
        assertFalse(Analytics.isEnabled().get());
        verify(channel).removeListener(isA(SessionTracker.class));
        verify(channel).removeListener(isA(AnalyticsValidator.class));
        verify(channel, times(2)).removeGroup(analytics.getGroupName());
        verify(channel).clear(analytics.getGroupName());
        verifyStatic();
        StorageHelper.PreferencesStorage.remove("sessions");

        /* Now try to use all methods. Should not work. */
        Analytics.trackEvent("test");
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("t1");
        target.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Enable again, verify the async behavior of setEnabled with the callback. */
        final CountDownLatch latch = new CountDownLatch(1);
        Analytics.setEnabled(true).thenAccept(new AppCenterConsumer<Void>() {

            @Override
            public void accept(Void aVoid) {
                latch.countDown();
            }
        });
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS));
        assertTrue(Analytics.isEnabled().get());

        /* Test double call to setEnabled true. */
        Analytics.setEnabled(true);
        assertTrue(Analytics.isEnabled().get());
        Analytics.trackEvent("test");
        target.trackEvent("test");
        target.getTransmissionTarget("t2").trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, times(4)).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Disable again. */
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled().get());
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());

        /* No more log enqueued. */
        verify(channel, times(4)).enqueue(any(Log.class), eq(analytics.getGroupName()));
    }

    @Test
    public void disablePersisted() {
        when(StorageHelper.PreferencesStorage.getBoolean(ANALYTICS_ENABLED_KEY, true)).thenReturn(false);
        Analytics analytics = Analytics.getInstance();

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void startSessionAfterUserApproval() {

        /*
         * Disable analytics while in background to set up the initial condition
         * simulating the opt-in use case.
         */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
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
         * simulating the opt-in use case.
         */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.setEnabled(false);

        /* App in foreground: no log yet, we are disabled. */
        analytics.onActivityResumed(new Activity());
        analytics.getCurrentActivity().clear();
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Enable: start session not sent retroactively, weak reference lost. */
        Analytics.setEnabled(true);
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));
    }

    @Test
    public void analyticsListener() {
        AnalyticsListener listener = mock(AnalyticsListener.class);
        Analytics.setListener(listener);
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        final ArgumentCaptor<Channel.GroupListener> captor = ArgumentCaptor.forClass(Channel.GroupListener.class);
        verify(channel).addGroup(anyString(), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), captor.capture());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                captor.getValue().onBeforeSending((Log) invocation.getArguments()[0]);
                captor.getValue().onSuccess((Log) invocation.getArguments()[0]);
                captor.getValue().onFailure((Log) invocation.getArguments()[0], new Exception());
                return null;
            }
        }).when(channel).enqueue(any(Log.class), anyString());
        Analytics.trackEvent("name");
        verify(listener).onBeforeSending(notNull(Log.class));
        verify(listener).onSendingSucceeded(notNull(Log.class));
        verify(listener).onSendingFailed(notNull(Log.class), notNull(Exception.class));
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

    @Test
    public void appOnlyFeatures() {

        /* Start from library. */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, null, null, false);

        /* Session tracker not initialized. */
        verify(channel, never()).addListener(isA(SessionTracker.class));

        /* No page tracking either. */
        SomeScreen activity = new SomeScreen();
        analytics.onActivityResumed(activity);
        analytics.onActivityPaused(activity);
        analytics.onActivityResumed(new MyActivity());
        verify(channel, never()).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()));
        verify(channel, never()).enqueue(isA(PageLog.class), eq(analytics.getGroupName()));

        /* Even when switching states. */
        Analytics.setEnabled(false);
        Analytics.setEnabled(true);
        verify(channel, never()).addListener(isA(SessionTracker.class));
        verify(channel, never()).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()));
        verify(channel, never()).enqueue(isA(PageLog.class), eq(analytics.getGroupName()));

        /* Now start app, no secret needed. */
        analytics.onConfigurationUpdated(null, null);

        /* Session tracker is started now. */
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).enqueue(isA(StartSessionLog.class), anyString());

        /* Verify last page tracked as still in foreground. */
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return "My".equals(pageLog.getName());
                }
                return false;
            }
        }), eq(analytics.getGroupName()));

        /* Check that was the only page sent. */
        verify(channel).enqueue(isA(PageLog.class), eq(analytics.getGroupName()));

        /* Session tracker removed if disabled. */
        Analytics.setEnabled(false);
        verify(channel).removeListener(isA(SessionTracker.class));

        /* And added again on enabling again. Page tracked again. */
        Analytics.setEnabled(true);
        verify(channel, times(2)).addListener(isA(SessionTracker.class));
        verify(channel, times(2)).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()));
        verify(channel, times(2)).enqueue(isA(PageLog.class), eq(analytics.getGroupName()));
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
