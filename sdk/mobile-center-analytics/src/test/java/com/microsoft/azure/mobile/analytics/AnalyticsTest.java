package com.microsoft.azure.mobile.analytics;

import android.content.Context;
import android.os.SystemClock;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.MobileCenterHandler;
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
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.async.MobileCenterConsumer;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.mobile.test.TestUtils.generateString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemClock.class, StorageHelper.PreferencesStorage.class, MobileCenterLog.class, MobileCenter.class, HandlerUtils.class})
public class AnalyticsTest {

    private static final String ANALYTICS_ENABLED_KEY = PrefStorageConstants.KEY_ENABLED + "_" + Analytics.getInstance().getServiceName();

    @Mock
    private MobileCenterFuture<Boolean> mCoreEnabledFuture;

    @Mock
    private MobileCenterHandler mMobileCenterHandler;

    @Before
    public void setUp() {
        Analytics.unsetInstance();
        mockStatic(SystemClock.class);
        mockStatic(MobileCenterLog.class);
        mockStatic(MobileCenter.class);
        when(MobileCenter.isEnabled()).thenReturn(mCoreEnabledFuture);
        when(mCoreEnabledFuture.get()).thenReturn(true);
        Answer<Void> runNow = new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        };
        doAnswer(runNow).when(mMobileCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mockStatic(HandlerUtils.class);
        doAnswer(runNow).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

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

        /*
         * Before start, calling onActivityResume is ignored.
         * In reality it never happens, it means someone is messing with internals directly.
         */
        Analytics analytics = Analytics.getInstance();
        analytics.onActivityResumed(new Activity());
        assertNull(analytics.getCurrentActivity());
        verifyStatic();
        MobileCenterLog.error(anyString(), anyString());
        analytics.onActivityPaused(new Activity());
        verifyStatic(times(2));
        MobileCenterLog.error(anyString(), anyString());

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mMobileCenterHandler);
        analytics.onStarted(mock(Context.class), "", channel);

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
        analytics.onStarting(mMobileCenterHandler);
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
    public void testTrackEvent() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mMobileCenterHandler);
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
        final String maxName = generateString(Analytics.MAX_NAME_LENGTH, '*');
        Analytics.trackEvent(maxName + "*", null);
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getName().equals(maxName) && eventLog.getProperties() == null;
                }
                return false;
            }
        }), anyString());
        reset(channel);
        Analytics.trackEvent(maxName, null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackEvent("eventName", new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
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
        reset(channel);
        final String longerMapItem = generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        Analytics.trackEvent("eventName", new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    if (eventLog.getProperties().size() == 1) {
                        Map.Entry<String, String> entry = eventLog.getProperties().entrySet().iterator().next();
                        String truncatedMapItem = generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH, '*');
                        return entry.getKey().length() == Analytics.MAX_PROPERTY_ITEM_LENGTH && entry.getValue().length() == Analytics.MAX_PROPERTY_ITEM_LENGTH;
                    }
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void testTrackPage() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mMobileCenterHandler);
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
        final String maxName = generateString(Analytics.MAX_NAME_LENGTH, '*');
        Analytics.trackPage(maxName + "*", null);
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    return pageLog.getName().equals(maxName) && pageLog.getProperties() == null;
                }
                return false;
            }
        }), anyString());
        reset(channel);
        Analytics.trackPage(maxName, null);
        verify(channel, times(1)).enqueue(any(Log.class), anyString());
        reset(channel);
        Analytics.trackPage("pageName", new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
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
        reset(channel);
        final String longerMapItem = generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        Analytics.trackPage("pageName", new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }});
        verify(channel, times(1)).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof PageLog) {
                    PageLog pageLog = (PageLog) item;
                    if (pageLog.getProperties().size() == 1) {
                        Map.Entry<String, String> entry = pageLog.getProperties().entrySet().iterator().next();
                        String truncatedMapItem = generateString(Analytics.MAX_PROPERTY_ITEM_LENGTH, '*');
                        return entry.getKey().length() == Analytics.MAX_PROPERTY_ITEM_LENGTH && entry.getValue().length() == Analytics.MAX_PROPERTY_ITEM_LENGTH;
                    }
                }
                return false;
            }
        }), anyString());
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
        analytics.onStarting(mMobileCenterHandler);
        analytics.onStarted(mock(Context.class), "", channel);
        verify(channel).removeGroup(eq(analytics.getGroupName()));
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyLong(), anyInt(), any(Channel.GroupListener.class));
        verify(channel).addListener(any(Channel.Listener.class));

        /* Now we can see the service enabled. */
        assertTrue(Analytics.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Analytics.setEnabled(false).get();
        assertFalse(Analytics.isEnabled().get());
        verify(channel).removeListener(any(SessionTracker.class));
        verify(channel, times(2)).removeGroup(analytics.getGroupName());
        verify(channel).clear(analytics.getGroupName());
        verifyStatic();
        StorageHelper.PreferencesStorage.remove("sessions");

        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Enable again, verify the async behavior of setEnabled with the callback. */
        final CountDownLatch latch = new CountDownLatch(1);
        Analytics.setEnabled(true).thenAccept(new MobileCenterConsumer<Void>() {

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
        Analytics.trackPage("test");
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()));

        /* Disable again. */
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled().get());
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());

        /* No more log enqueued. */
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()));
    }

    @Test
    public void startSessionAfterUserApproval() {

        /*
         * Disable analytics while in background to set up the initial condition
         * simulating the optin use case.
         */
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mMobileCenterHandler);
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
        analytics.onStarting(mMobileCenterHandler);
        analytics.onStarted(mock(Context.class), "", channel);
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
    public void analyticsListener() throws IOException, ClassNotFoundException {
        AnalyticsListener listener = mock(AnalyticsListener.class);
        Analytics.setListener(listener);
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mMobileCenterHandler);
        analytics.onStarted(mock(Context.class), "", channel);
        final ArgumentCaptor<Channel.GroupListener> captor = ArgumentCaptor.forClass(Channel.GroupListener.class);
        verify(channel).addGroup(anyString(), anyInt(), anyLong(), anyInt(), captor.capture());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
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
