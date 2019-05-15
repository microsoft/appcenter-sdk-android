/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Flags;
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
import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.context.UserIdContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.analytics.Analytics.ANALYTICS_CRITICAL_GROUP;
import static com.microsoft.appcenter.analytics.Analytics.ANALYTICS_GROUP;
import static com.microsoft.appcenter.analytics.Analytics.MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS;
import static com.microsoft.appcenter.analytics.Analytics.MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
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
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class AnalyticsTest extends AbstractAnalyticsTest {

    @After
    public void resetUserId() {
        UserIdContext.unsetInstance();
    }

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
        Analytics.trackEvent("test", (Map<String, String>) null);
        Analytics.trackEvent("test", (Map<String, String>) null, 0);
        Analytics.trackEvent("test", (EventProperties) null);
        Analytics.trackEvent("test", (EventProperties) null, 0);
        Analytics.trackPage("test");
        Analytics.trackPage("test", new HashMap<String, String>());
        Analytics.trackPage("test", null);

        /* Verify we just get an error every time. */
        verifyStatic(times(9));
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
        }), eq(analytics.getGroupName()), eq(DEFAULTS));
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
        }), anyString(), eq(DEFAULTS));
        verify(channel, never()).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof PageLog;
            }
        }), anyString(), anyInt());
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
        }), eq(analytics.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void trackEventFromAppWithoutProperties() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Send event without properties. */
        Analytics.trackEvent("eventName");
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertNull(argumentCaptor.getValue().getTypedProperties());
    }

    @Test
    public void trackEventFromAppWithNullMapProperty() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Send event with empty Map properties. */
        Analytics.trackEvent("eventName", (Map<String, String>) null);
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertNull(argumentCaptor.getValue().getTypedProperties());
    }

    @Test
    public void trackEventFromAppWithEmptyMapProperty() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Send event with empty Map properties. */
        Analytics.trackEvent("eventName", new HashMap<String, String>());
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertEquals(Collections.emptyList(), argumentCaptor.getValue().getTypedProperties());
    }

    @Test
    public void trackEventFromAppWithMapProperties() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Send event with non-empty Map properties. */
        Analytics.trackEvent("eventName", new HashMap<String, String>() {{
            put("name", "value");
        }});
        StringTypedProperty stringProperty = new StringTypedProperty();
        stringProperty.setName("name");
        stringProperty.setValue("value");
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertEquals(Collections.<TypedProperty>singletonList(stringProperty), argumentCaptor.getValue().getTypedProperties());
    }

    @Test
    public void trackEventFromAppWithEmptyEventProperties() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Send event with empty EventProperties. */
        Analytics.trackEvent("eventName", new EventProperties());
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertEquals(Collections.emptyList(), argumentCaptor.getValue().getTypedProperties());
    }

    @Test
    public void trackEventFromAppWithEventProperties() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        ArgumentCaptor<EventLog> argumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);

        /* Prepare typed properties. */
        Date date = new Date();
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName("n0");
        stringTypedProperty.setValue("value");
        DateTimeTypedProperty dateTimeTypedProperty = new DateTimeTypedProperty();
        dateTimeTypedProperty.setName("n1");
        dateTimeTypedProperty.setValue(date);
        LongTypedProperty longTypedProperty = new LongTypedProperty();
        longTypedProperty.setName("n2");
        longTypedProperty.setValue(0);
        DoubleTypedProperty doubleTypedProperty = new DoubleTypedProperty();
        doubleTypedProperty.setName("n3");
        doubleTypedProperty.setValue(0);
        BooleanTypedProperty booleanTypedProperty = new BooleanTypedProperty();
        booleanTypedProperty.setName("n4");
        booleanTypedProperty.setValue(true);

        /* Send event with non-empty EventProperties. */
        EventProperties eventProperties = new EventProperties();
        eventProperties.set("n0", "value");
        eventProperties.set("n1", date);
        eventProperties.set("n2", 0L);
        eventProperties.set("n3", 0d);
        eventProperties.set("n4", true);
        Analytics.trackEvent("eventName", eventProperties);
        verify(channel).enqueue(argumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNotNull(argumentCaptor.getValue());
        assertEquals("eventName", argumentCaptor.getValue().getName());
        assertEquals(stringTypedProperty, argumentCaptor.getValue().getTypedProperties().get(0));
        assertEquals(dateTimeTypedProperty, argumentCaptor.getValue().getTypedProperties().get(1));
        assertEquals(longTypedProperty, argumentCaptor.getValue().getTypedProperties().get(2));
        assertEquals(doubleTypedProperty, argumentCaptor.getValue().getTypedProperties().get(3));
        assertEquals(booleanTypedProperty, argumentCaptor.getValue().getTypedProperties().get(4));
    }

    @Test
    public void trackEventWithNormalPersistenceFlag() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackEvent("eventName1", (Map<String, String>) null, NORMAL);
        Analytics.trackEvent("eventName2", (EventProperties) null, NORMAL);
        verify(channel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(NORMAL));
    }

    @Test
    public void trackEventWithNormalCriticalPersistenceFlag() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackEvent("eventName1", (Map<String, String>) null, CRITICAL);
        Analytics.trackEvent("eventName2", (EventProperties) null, CRITICAL);
        verify(channel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(CRITICAL));
    }

    @Test
    public void trackEventWithInvalidFlags() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackEvent("eventName1", (Map<String, String>) null, 0x03);
        Analytics.trackEvent("eventName2", (EventProperties) null, 0x03);
        verify(channel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(DEFAULTS));
        verifyStatic(times(2));
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void trackEventWithUserIdWhenConfiguredForTarget() {
        UserIdContext.getInstance().setUserId("c:alice");
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, null, "target", true);
        Analytics.trackEvent("eventName1");
        ArgumentCaptor<EventLog> eventLogArgumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        verify(channel).enqueue(eventLogArgumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertEquals("c:alice", eventLogArgumentCaptor.getValue().getUserId());
    }

    @Test
    public void trackEventWithoutUserIdWhenConfiguredForAppSecretOnly() {
        UserIdContext.getInstance().setUserId("alice");
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "appSecret", null, true);
        Analytics.trackEvent("eventName1");
        ArgumentCaptor<EventLog> eventLogArgumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        verify(channel).enqueue(eventLogArgumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertNull(eventLogArgumentCaptor.getValue().getUserId());
    }

    @Test
    public void trackEventWithoutUserIdWhenConfiguredForBothSecrets() {
        UserIdContext.getInstance().setUserId("c:alice");
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "appSecret", "target", true);
        Analytics.trackEvent("eventName1");
        ArgumentCaptor<EventLog> eventLogArgumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        verify(channel).enqueue(eventLogArgumentCaptor.capture(), anyString(), eq(DEFAULTS));
        assertEquals("c:alice", eventLogArgumentCaptor.getValue().getUserId());
    }

    @Test
    public void trackEventFromLibrary() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, null, null, false);

        /* Static track call forbidden if app didn't start Analytics. */
        Analytics.trackEvent("eventName");
        verify(channel, never()).enqueue(isA(EventLog.class), anyString(), anyInt());

        /* It works from a target. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("t");
        target.trackEvent("eventName");
        verify(channel).enqueue(isA(EventLog.class), anyString(), eq(DEFAULTS));

        /* It works from a child target. */
        target.getTransmissionTarget("t2").trackEvent("eventName");
        verify(channel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(DEFAULTS));
    }

    @Test
    public void trackPageFromApp() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        Analytics.trackPage("pageName");
        verify(channel).enqueue(isA(PageLog.class), anyString(), eq(DEFAULTS));
    }

    @Test
    public void trackPageFromLibrary() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, false);

        /* Page tracking does not work from library. */
        Analytics.trackPage("pageName");
        verify(channel, never()).enqueue(isA(PageLog.class), anyString(), anyInt());
    }

    @Test
    public void setEnabled() throws InterruptedException {
        Channel channel = mock(Channel.class);
        final ArgumentCaptor<String> captorGroupName = ArgumentCaptor.forClass(String.class);

        /* Before start it does not work to change state, it's disabled. */
        Analytics analytics = Analytics.getInstance();
        Analytics.setEnabled(true);
        assertFalse(Analytics.isEnabled().get());
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled().get());

        /* Start. */
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).removeGroup(eq(analytics.getGroupName()));
        verify(channel).addGroup(eq(ANALYTICS_CRITICAL_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addGroup(eq(ANALYTICS_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).addListener(isA(AnalyticsValidator.class));
        verify(channel).addListener(isA(AnalyticsTransmissionTarget.getChannelListener().getClass()));

        /* Now we can see the service enabled. */
        assertTrue(Analytics.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Analytics.setEnabled(false).get();
        assertFalse(Analytics.isEnabled().get());
        verify(channel).removeListener(isA(SessionTracker.class));
        verify(channel).removeListener(isA(AnalyticsValidator.class));
        verify(channel).removeListener(isA(AnalyticsTransmissionTarget.getChannelListener().getClass()));
        verify(channel).removeGroup(eq(ANALYTICS_CRITICAL_GROUP));
        verify(channel, times(2)).removeGroup(eq(ANALYTICS_GROUP));
        verify(channel).clear(analytics.getGroupName());
        verifyStatic();
        SharedPreferencesManager.remove("sessions");

        /* Now try to use all methods. Should not work. */
        Analytics.trackEvent("test");
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("t1");
        target.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());

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
        verify(channel, times(4)).enqueue(any(Log.class), eq(analytics.getGroupName()), eq(DEFAULTS));

        /* Disable again. */
        Analytics.setEnabled(false);
        assertFalse(Analytics.isEnabled().get());
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        analytics.onActivityResumed(new Activity());
        analytics.onActivityPaused(new Activity());

        /* No more log enqueued. */
        verify(channel, times(4)).enqueue(any(Log.class), eq(analytics.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(ANALYTICS_ENABLED_KEY, true)).thenReturn(false);
        Analytics analytics = Analytics.getInstance();

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void notSendingLogsOnPause() {

        /* Before start it does not work to change state, it's disabled. */
        Analytics analytics = Analytics.getInstance();

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).addListener(isA(AnalyticsValidator.class));

        /* Pause Analytics. */
        Analytics.pause();

        /* Check if Analytics group is paused. */
        verify(channel).pauseGroup(ANALYTICS_GROUP, null);
        verify(channel).pauseGroup(ANALYTICS_CRITICAL_GROUP, null);

        /* Send logs to verify the logs are enqueued after pause. */
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()), eq(DEFAULTS));

        /* Resume Analytics. */
        Analytics.resume();

        /* Check if Analytics group is resumed. */
        verify(channel).resumeGroup(ANALYTICS_GROUP, null);
        verify(channel).resumeGroup(ANALYTICS_CRITICAL_GROUP, null);

        /* Send logs to verify the logs are enqueued after resume. */
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, times(4)).enqueue(any(Log.class), eq(analytics.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void pauseResumeWhileDisabled() {

        /* Before start it does not work to change state, it's disabled. */
        Analytics analytics = Analytics.getInstance();

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).addListener(isA(AnalyticsValidator.class));

        /* Disable and pause Analytics. */
        Analytics.setEnabled(false);
        Analytics.pause();

        /* Check if Analytics group is paused even while disabled. */
        verify(channel, never()).pauseGroup(ANALYTICS_GROUP, null);
        verify(channel, never()).pauseGroup(ANALYTICS_CRITICAL_GROUP, null);

        /* Send logs to verify the logs are enqueued after pause. */
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());

        /* Resume Analytics. */
        Analytics.resume();

        /* Check if Analytics group is resumed even while paused. */
        verify(channel, never()).resumeGroup(ANALYTICS_CRITICAL_GROUP, null);
        verify(channel, never()).resumeGroup(ANALYTICS_GROUP, null);

        /* Send logs to verify the logs are enqueued after resume. */
        Analytics.trackEvent("test");
        Analytics.trackPage("test");
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());
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
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());

        /* Enable: start session sent retroactively. */
        Analytics.setEnabled(true);
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof StartSessionLog;
            }
        }), eq(analytics.getGroupName()), eq(DEFAULTS));
        verify(channel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof PageLog;
            }
        }), eq(analytics.getGroupName()), eq(DEFAULTS));

        /* Go background. */
        analytics.onActivityPaused(new Activity());

        /* Disable/enable: nothing happens on background. */
        Analytics.setEnabled(false);
        Analytics.setEnabled(true);

        /* No additional log. */
        verify(channel, times(2)).enqueue(any(Log.class), eq(analytics.getGroupName()), eq(DEFAULTS));
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
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());

        /* Enable: start session not sent retroactively, weak reference lost. */
        Analytics.setEnabled(true);
        verify(channel, never()).enqueue(any(Log.class), eq(analytics.getGroupName()), anyInt());
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
        verify(channel, times(2)).addGroup(anyString(), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), captor.capture());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                captor.getValue().onBeforeSending((Log) invocation.getArguments()[0]);
                captor.getValue().onSuccess((Log) invocation.getArguments()[0]);
                captor.getValue().onFailure((Log) invocation.getArguments()[0], new Exception());
                return null;
            }
        }).when(channel).enqueue(any(Log.class), anyString(), anyInt());
        Analytics.trackEvent("name");
        verify(listener).onBeforeSending(notNull(Log.class));
        verify(listener).onSendingSucceeded(notNull(Log.class));
        verify(listener).onSendingFailed(notNull(Log.class), notNull(Exception.class));
    }

    @Test
    public void analyticsListenerCriticalEvent() {
        AnalyticsListener listener = mock(AnalyticsListener.class);
        Analytics.setListener(listener);
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).addGroup(eq(ANALYTICS_CRITICAL_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addGroup(eq(ANALYTICS_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        Analytics.trackEvent("name", generateEventProperties(), Flags.CRITICAL);
        verify(channel).enqueue(any(Log.class), eq(ANALYTICS_CRITICAL_GROUP), eq(Flags.CRITICAL));
    }

    @Test
    public void analyticsListenerNormalEvent() {
        AnalyticsListener listener = mock(AnalyticsListener.class);
        Analytics.setListener(listener);
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).addGroup(eq(ANALYTICS_CRITICAL_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(channel).addGroup(eq(ANALYTICS_GROUP), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        Analytics.trackEvent("name", generateEventProperties(), Flags.NORMAL);
        verify(channel).enqueue(any(Log.class), eq(ANALYTICS_GROUP), eq(Flags.NORMAL));
    }

    private EventProperties generateEventProperties() {

        /* Send event with non-empty EventProperties. */
        EventProperties eventProperties = new EventProperties();
        eventProperties.set("n0", "value");
        eventProperties.set("n1", new Date());
        eventProperties.set("n2", 0L);
        eventProperties.set("n3", 0d);
        eventProperties.set("n4", true);
        return eventProperties;
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
        verify(channel, never()).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()), anyInt());
        verify(channel, never()).enqueue(isA(PageLog.class), eq(analytics.getGroupName()), anyInt());

        /* Even when switching states. */
        Analytics.setEnabled(false);
        Analytics.setEnabled(true);
        verify(channel, never()).addListener(isA(SessionTracker.class));
        verify(channel, never()).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()), anyInt());
        verify(channel, never()).enqueue(isA(PageLog.class), eq(analytics.getGroupName()), anyInt());

        /* Now start app, no secret needed. */
        analytics.onConfigurationUpdated(null, null);

        /* Session tracker is started now. */
        verify(channel).addListener(isA(SessionTracker.class));
        verify(channel).enqueue(isA(StartSessionLog.class), anyString(), eq(DEFAULTS));

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
        }), eq(analytics.getGroupName()), eq(DEFAULTS));

        /* Check that was the only page sent. */
        verify(channel).enqueue(isA(PageLog.class), eq(analytics.getGroupName()), eq(DEFAULTS));

        /* Session tracker removed if disabled. */
        Analytics.setEnabled(false);
        verify(channel).removeListener(isA(SessionTracker.class));

        /* And added again on enabling again. Page tracked again. */
        Analytics.setEnabled(true);
        verify(channel, times(2)).addListener(isA(SessionTracker.class));
        verify(channel, times(2)).enqueue(isA(StartSessionLog.class), eq(analytics.getGroupName()), eq(DEFAULTS));
        verify(channel, times(2)).enqueue(isA(PageLog.class), eq(analytics.getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void createTransmissionTargetBeforeStart() {

        /* Given app center not configured. */
        mockStatic(AppCenterLog.class);
        when(AppCenter.isConfigured()).thenReturn(false);

        /* When creating a target, it returns null. */
        assertNull(Analytics.getTransmissionTarget("t1"));

        /* And prints an error. */
        verifyStatic();
        AppCenterLog.error(anyString(), contains("AppCenter is not configured"));
    }

    @Test
    public void unableToSetTransmissionIntervalMoreThanMaximum() {
        Analytics analytics = Analytics.getInstance();
        boolean result = Analytics.setTransmissionInterval(MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS + 1);
        assertFalse(result);
    }

    @Test
    public void setTransmissionInterval() {
        Analytics analytics = Analytics.getInstance();
        boolean result = Analytics.setTransmissionInterval(MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS + 1);
        assertTrue(result);
        assertEquals(TimeUnit.MILLISECONDS.toSeconds(Analytics.getInstance().getTriggerInterval()),MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS + 1);
    }

    @Test
    public void unableToSetTransmissionIntervalLessThanMinimum() {
        Analytics analytics = Analytics.getInstance();
        boolean result = Analytics.setTransmissionInterval(MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS - 1);
        assertFalse(result);
    }

    @Test
    public void unableToSetTransmissionIntervalAfterStart() {
        Analytics analytics = Analytics.getInstance();
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, null, null, false);
        boolean result = Analytics.setTransmissionInterval(MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS + 1);
        assertFalse(result);
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
