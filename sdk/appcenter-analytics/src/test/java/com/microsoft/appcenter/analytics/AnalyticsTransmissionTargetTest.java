/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AppCenterHandler;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Extensions;
import com.microsoft.appcenter.ingestion.models.one.ProtocolExtension;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.Flags.CRITICAL;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.analytics.Analytics.ANALYTICS_CRITICAL_GROUP;
import static com.microsoft.appcenter.analytics.Analytics.ANALYTICS_GROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class AnalyticsTransmissionTargetTest extends AbstractAnalyticsTest {

    @Mock
    private Channel mChannel;

    @Before
    public void setUp() {

        /* Start. */
        super.setUp();
        Analytics analytics = Analytics.getInstance();
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), mChannel, null, null, false);
        AnalyticsTransmissionTarget.sAuthenticationProvider = null;
    }

    @Test
    public void testGetTransmissionTarget() {
        assertNotNull(Analytics.getTransmissionTarget("token"));
    }

    @Test
    public void testGetTransmissionTargetWithNullToken() {
        mockStatic(AppCenterLog.class);
        assertNull(Analytics.getTransmissionTarget(null));

        /* Verify log. */
        verifyStatic();
        AppCenterLog.error(anyString(), contains("Transmission target token may not be null or empty."));
    }

    @Test
    public void testGetTransmissionTargetWithEmptyToken() {
        mockStatic(AppCenterLog.class);
        assertNull(Analytics.getTransmissionTarget(""));

        /* Verify log. */
        verifyStatic();
        AppCenterLog.error(anyString(), contains("Transmission target token may not be null or empty."));
    }

    @Test
    public void trackEventWithTransmissionTargetFromApp() {
        testTrackEventWithTransmissionTarget("defaultToken", true);
    }

    @Test
    public void trackEventWithTransmissionTargetFromLibrary() {
        testTrackEventWithTransmissionTarget(null, false);
    }

    private void testTrackEventWithTransmissionTarget(final String defaultToken, boolean startFromApp) {

        /* Overwrite setup for this test. */
        Analytics.unsetInstance();
        Analytics analytics = Analytics.getInstance();
        mChannel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), mChannel, null, defaultToken, startFromApp);
        final AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("token");
        assertNotNull(target);

        /* Getting a reference to the same target a second time actually returns the same. */
        assertSame(target, Analytics.getTransmissionTarget("token"));

        /* Track event with default transmission target. */
        Analytics.trackEvent("name");
        if (startFromApp) {
            verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

                @Override
                public boolean matches(Object item) {
                    if (item instanceof EventLog) {
                        EventLog eventLog = (EventLog) item;
                        boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getTypedProperties() == null;
                        boolean tokenMatch;
                        boolean tagMatch;
                        if (defaultToken != null) {
                            tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains(defaultToken);
                            tagMatch = Analytics.getInstance().mDefaultTransmissionTarget.equals(eventLog.getTag());
                        } else {
                            tokenMatch = eventLog.getTransmissionTargetTokens().isEmpty();
                            tagMatch = eventLog.getTag() == null;
                        }
                        return nameAndPropertiesMatch && tokenMatch && tagMatch;
                    }
                    return false;
                }
            }), anyString(), eq(DEFAULTS));
        } else {
            verify(mChannel, never()).enqueue(isA(EventLog.class), anyString(), anyInt());
        }
        reset(mChannel);

        /* Track event via transmission target method. */
        target.trackEvent("name");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getTypedProperties() == null;
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token");
                    boolean tagMatch = target.equals(eventLog.getTag());
                    return nameAndPropertiesMatch && tokenMatch && tagMatch;
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));
        reset(mChannel);

        /* Track event via another transmission target method with properties. */
        Analytics.getTransmissionTarget("token2").trackEvent("name", new HashMap<String, String>() {{
            put("valid", "valid");
        }});
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    boolean nameMatches = eventLog.getName().equals("name");
                    List<TypedProperty> typedProperties = new ArrayList<>();
                    StringTypedProperty stringTypedProperty = new StringTypedProperty();
                    stringTypedProperty.setName("valid");
                    stringTypedProperty.setValue("valid");
                    typedProperties.add(stringTypedProperty);
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token2");
                    boolean tagMatch = Analytics.getTransmissionTarget("token2").equals(eventLog.getTag());
                    return nameMatches && tokenMatch && tagMatch && typedProperties.equals(eventLog.getTypedProperties());
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));
        reset(mChannel);

        /* Create a child transmission target and track event. */
        final AnalyticsTransmissionTarget childTarget = target.getTransmissionTarget("token3");
        childTarget.trackEvent("name");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getTypedProperties() == null;
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token3");
                    boolean tagMatch = childTarget.equals(eventLog.getTag());
                    return nameAndPropertiesMatch && tokenMatch && tagMatch;
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));
        reset(mChannel);

        /* Another child transmission target with the same token should be the same instance. */
        assertSame(childTarget, target.getTransmissionTarget("token3"));
    }

    @Test
    public void setEnabled() {

        /* Create a transmission target and assert that it's enabled by default. */
        AnalyticsTransmissionTarget transmissionTarget = Analytics.getTransmissionTarget("test");
        assertTrue(transmissionTarget.isEnabledAsync().get());
        transmissionTarget.trackEvent("eventName1");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getName().equals("eventName1");
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));

        /* Set enabled to false and assert that it cannot track event. */
        transmissionTarget.setEnabledAsync(false).get();
        assertFalse(transmissionTarget.isEnabledAsync().get());
        transmissionTarget.trackEvent("eventName2");
        verify(mChannel, never()).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getName().equals("eventName2");
                }
                return false;
            }
        }), anyString(), anyInt());
    }

    @Test
    public void setEnabledOnParent() {

        /* Create a transmission target and its child. */
        AnalyticsTransmissionTarget parentTransmissionTarget = Analytics.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget childTransmissionTarget = parentTransmissionTarget.getTransmissionTarget("child");

        /* Set enabled to false on parent and child should also have set enabled to false. */
        parentTransmissionTarget.setEnabledAsync(false).get();
        assertFalse(parentTransmissionTarget.isEnabledAsync().get());
        assertFalse(childTransmissionTarget.isEnabledAsync().get());
        childTransmissionTarget.trackEvent("eventName1");
        verify(mChannel, never()).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getName().equals("eventName1");
                }
                return false;
            }
        }), anyString(), anyInt());

        /* Set enabled to true on parent. Verify that child can track event. */
        parentTransmissionTarget.setEnabledAsync(true);
        childTransmissionTarget.trackEvent("eventName2");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    return eventLog.getName().equals("eventName2");
                }
                return false;
            }
        }), anyString(), eq(DEFAULTS));
    }

    @Test
    public void setEnabledOnChild() {

        /* Create a transmission target and its child. */
        AnalyticsTransmissionTarget parentTransmissionTarget = Analytics.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget childTransmissionTarget = parentTransmissionTarget.getTransmissionTarget("child");

        /* Set enabled to false on parent. When try to set enabled to true on child, it should stay false. */
        parentTransmissionTarget.setEnabledAsync(false).get();
        childTransmissionTarget.setEnabledAsync(true);
        assertFalse(childTransmissionTarget.isEnabledAsync().get());
        childTransmissionTarget.trackEvent("eventName");
        verify(mChannel, never()).enqueue(any(Log.class), anyString(), anyInt());
    }

    @Test
    public void disableAnalytics() {

        /* Set analytics to disabled. */
        Analytics.setEnabled(false);

        /* Create grand parent, parent and child transmission targets and verify that they're all disabled. */
        AnalyticsTransmissionTarget grandParentTarget = Analytics.getTransmissionTarget("grandParent");
        AnalyticsTransmissionTarget parentTarget = grandParentTarget.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget childTarget = parentTarget.getTransmissionTarget("child");
        assertFalse(grandParentTarget.isEnabledAsync().get());
        assertFalse(parentTarget.isEnabledAsync().get());
        assertFalse(childTarget.isEnabledAsync().get());

        /* Enable analytics and verify that they're now all enabled. */
        Analytics.setEnabled(true);
        assertTrue(grandParentTarget.isEnabledAsync().get());
        assertTrue(parentTarget.isEnabledAsync().get());
        assertTrue(childTarget.isEnabledAsync().get());
    }

    @Test
    public void createChildrenAfterDisabling() {

        /* Disable grandparent. */
        AnalyticsTransmissionTarget grandParentTarget = Analytics.getTransmissionTarget("grandParent");
        grandParentTarget.setEnabledAsync(false).get();

        /* Create a parent and child after. */
        AnalyticsTransmissionTarget parentTarget = grandParentTarget.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget childTarget = parentTarget.getTransmissionTarget("child");

        /* Check everything is disabled. */
        assertFalse(grandParentTarget.isEnabledAsync().get());
        assertFalse(parentTarget.isEnabledAsync().get());
        assertFalse(childTarget.isEnabledAsync().get());
    }

    @Test
    public void addAuthenticationProvider() {

        /* Passing null does not do anything and does not crash. */
        AnalyticsTransmissionTarget.addAuthenticationProvider(null);
        assertNull(AnalyticsTransmissionTarget.sAuthenticationProvider);

        /* Build an object now for the parameter. */
        AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);
        assertNull(AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* Set type. */
        when(authenticationProvider.getType()).thenReturn(AuthenticationProvider.Type.MSA_COMPACT);
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);
        assertNull(AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* Set ticket key. */
        when(authenticationProvider.getTicketKey()).thenReturn("key1");
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);
        assertNull(AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* Set token provider. */
        when(authenticationProvider.getTokenProvider()).thenReturn(mock(AuthenticationProvider.TokenProvider.class));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);
        assertEquals(authenticationProvider, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider).acquireTokenAsync();

        /* Replace provider with invalid parameters does not update. */
        AnalyticsTransmissionTarget.addAuthenticationProvider(null);
        AuthenticationProvider authenticationProvider2 = mock(AuthenticationProvider.class);
        when(authenticationProvider2.getType()).thenReturn(AuthenticationProvider.Type.MSA_COMPACT);
        when(authenticationProvider2.getTicketKey()).thenReturn("key2");
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider2);
        assertEquals(authenticationProvider, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider2, never()).acquireTokenAsync();

        /* Replace with valid provider. */
        when(authenticationProvider2.getTokenProvider()).thenReturn(mock(AuthenticationProvider.TokenProvider.class));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider2);
        assertEquals(authenticationProvider2, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider2).acquireTokenAsync();
    }

    @Test
    public void addTicketToLogBeforeStart() {

        /* Simulate not started. */
        Analytics.unsetInstance();
        when(AppCenter.isConfigured()).thenReturn(false);

        /* No actions are prepared without authentication provider. */
        CommonSchemaLog log = new CommonSchemaEventLog();
        AnalyticsTransmissionTarget.getChannelListener().onPreparingLog(log, "test");

        /* Add authentication provider before start. */
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        AuthenticationProvider authenticationProvider = spy(new AuthenticationProvider(AuthenticationProvider.Type.MSA_COMPACT, "key1", tokenProvider));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);
        assertEquals(authenticationProvider, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider).acquireTokenAsync();

        /* Start analytics. */
        when(AppCenter.isConfigured()).thenReturn(true);
        Analytics analytics = Analytics.getInstance();
        AppCenterHandler handler = mock(AppCenterHandler.class);
        ArgumentCaptor<Runnable> normalRunnable = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> disabledRunnable = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(handler).post(normalRunnable.capture(), disabledRunnable.capture());
        analytics.onStarting(handler);
        analytics.onStarted(mock(Context.class), mChannel, null, null, false);

        /* No actions are prepared with no CommonSchemaLog. */
        AnalyticsTransmissionTarget.getChannelListener().onPreparingLog(mock(Log.class), "test");
        verify(authenticationProvider, never()).checkTokenExpiry();

        /* Call prepare log. */
        final ProtocolExtension protocol = new ProtocolExtension();
        log.setExt(new Extensions() {{
            setProtocol(protocol);
        }});
        AnalyticsTransmissionTarget.getChannelListener().onPreparingLog(log, "test");

        /* Verify log. */
        assertEquals(Collections.singletonList(authenticationProvider.getTicketKeyHash()), protocol.getTicketKeys());

        /* And that we check expiry. */
        verify(authenticationProvider).checkTokenExpiry();
    }

    @Test
    public void updateAuthProviderAndLog() {

        /* When we enqueue app center log from track event. */
        final List<CommonSchemaLog> sentLogs = new ArrayList<>();
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) {
                ProtocolExtension protocol = new ProtocolExtension();
                Extensions ext = new Extensions();
                ext.setProtocol(protocol);
                CommonSchemaLog log = new CommonSchemaEventLog();
                log.setExt(ext);
                sentLogs.add(log);

                /* Call the listener after conversion of common schema for authentication decoration. */
                AnalyticsTransmissionTarget.getChannelListener().onPreparingLog(log, "test");
                return null;
            }
        }).when(mChannel).enqueue(any(Log.class), anyString(), anyInt());

        /* Start analytics and simulate background thread handler (we hold the thread command and run it in the test). */
        Analytics analytics = Analytics.getInstance();
        AppCenterHandler handler = mock(AppCenterHandler.class);
        ArgumentCaptor<Runnable> backgroundRunnable = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(handler).post(backgroundRunnable.capture(), any(Runnable.class));
        analytics.onStarting(handler);
        analytics.onStarted(mock(Context.class), mChannel, null, "test", true);

        /* Add first authentication provider. */
        AuthenticationProvider.TokenProvider tokenProvider1 = mock(AuthenticationProvider.TokenProvider.class);
        AuthenticationProvider authenticationProvider1 = spy(new AuthenticationProvider(AuthenticationProvider.Type.MSA_COMPACT, "key1", tokenProvider1));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider1);

        /* Check provider updated in background thread only when AppCenter is configured/started. */
        assertNull(AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider1, never()).acquireTokenAsync();
        assertNotNull(backgroundRunnable.getValue());

        /* Run background thread. */
        backgroundRunnable.getValue().run();

        /* Check update. */
        assertEquals(authenticationProvider1, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider1).acquireTokenAsync();

        /* Track an event. */
        Analytics.trackEvent("test1");
        Runnable trackEvent1Command = backgroundRunnable.getValue();

        /* Update authentication provider before the commands run and track a second event. */
        AuthenticationProvider.TokenProvider tokenProvider2 = mock(AuthenticationProvider.TokenProvider.class);
        AuthenticationProvider authenticationProvider2 = spy(new AuthenticationProvider(AuthenticationProvider.Type.MSA_COMPACT, "key2", tokenProvider2));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider2);
        Runnable addAuthProvider2Command = backgroundRunnable.getValue();
        Analytics.trackEvent("test2");
        Runnable trackEvent2Command = backgroundRunnable.getValue();

        /* Simulate background thread doing everything in a sequence. */
        trackEvent1Command.run();
        addAuthProvider2Command.run();
        trackEvent2Command.run();

        /* Verify first log has first ticket. */
        assertEquals(Collections.singletonList(authenticationProvider1.getTicketKeyHash()), sentLogs.get(0).getExt().getProtocol().getTicketKeys());

        /* And that we checked expiry. */
        verify(authenticationProvider1).checkTokenExpiry();

        /* Verify second log has the second ticket. */
        assertEquals(Collections.singletonList(authenticationProvider2.getTicketKeyHash()), sentLogs.get(1).getExt().getProtocol().getTicketKeys());

        /* And that we checked expiry. */
        verify(authenticationProvider2).checkTokenExpiry();
    }

    @Test
    public void registerCallbackWhenDisabledWorks() {

        /* Simulate disabling and background thread. */
        Analytics analytics = Analytics.getInstance();
        AppCenterHandler handler = mock(AppCenterHandler.class);
        ArgumentCaptor<Runnable> backgroundRunnable = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> disabledRunnable = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(handler).post(backgroundRunnable.capture(), disabledRunnable.capture());
        analytics.onStarting(handler);
        analytics.onStarted(mock(Context.class), mChannel, null, "test", true);

        /* Disable. */
        Analytics.setEnabled(false);
        backgroundRunnable.getValue().run();

        /* Add authentication provider while disabled. */
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        AuthenticationProvider authenticationProvider = spy(new AuthenticationProvider(AuthenticationProvider.Type.MSA_COMPACT, "key1", tokenProvider));
        AnalyticsTransmissionTarget.addAuthenticationProvider(authenticationProvider);

        /* Unlock command. */
        disabledRunnable.getValue().run();

        /* Verify update while disabled. */
        assertEquals(authenticationProvider, AnalyticsTransmissionTarget.sAuthenticationProvider);
        verify(authenticationProvider).acquireTokenAsync();

        /* Enable. */
        Analytics.setEnabled(true);
        disabledRunnable.getValue().run();

        /* Call prepare log. */
        ProtocolExtension protocol = new ProtocolExtension();
        Extensions ext = new Extensions();
        ext.setProtocol(protocol);
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(ext);
        AnalyticsTransmissionTarget.getChannelListener().onPreparingLog(log, "test");

        /* Verify log. */
        assertEquals(Collections.singletonList(authenticationProvider.getTicketKeyHash()), protocol.getTicketKeys());

        /* And that we check expiry. */
        verify(authenticationProvider).checkTokenExpiry();
    }

    @Test
    public void pauseResume() {

        /* Create a parent and child targets to test calls are not inherited. */
        AnalyticsTransmissionTarget parent = Analytics.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Call resume while not paused is only checked by channel so call is forwarded. */
        child.resume();
        verify(mChannel).resumeGroup(ANALYTICS_GROUP, "child");
        verify(mChannel).resumeGroup(ANALYTICS_CRITICAL_GROUP, "child");
        reset(mChannel);

        /* Test pause. */
        parent.pause();
        verify(mChannel).pauseGroup(ANALYTICS_GROUP, "parent");
        verify(mChannel, never()).pauseGroup(ANALYTICS_GROUP, "child");
        verify(mChannel, never()).pauseGroup(ANALYTICS_CRITICAL_GROUP, "child");

        /* We can call it twice, double calls are checked by channel. */
        parent.pause();
        verify(mChannel, times(2)).pauseGroup(ANALYTICS_GROUP, "parent");

        /* Test resume. */
        parent.resume();
        verify(mChannel).resumeGroup(ANALYTICS_GROUP, "parent");
        verify(mChannel, never()).resumeGroup(ANALYTICS_GROUP, "child");
        verify(mChannel, never()).resumeGroup(ANALYTICS_CRITICAL_GROUP, "child");

        /* We can call it twice, double calls are checked by channel. */
        parent.resume();
        verify(mChannel, times(2)).resumeGroup(ANALYTICS_GROUP, "parent");

        /* Disable analytics. */
        Analytics.setEnabled(false).get();
        reset(mChannel);

        /* We cannot call channel while disabled. */
        parent.pause();
        parent.resume();
        verify(mChannel, never()).pauseGroup(anyString(), anyString());
        verify(mChannel, never()).resumeGroup(anyString(), anyString());
    }

    @Test
    public void trackEventWithNormalPersistenceFlag() {
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("token");
        target.trackEvent("eventName1", (Map<String, String>) null, NORMAL);
        target.trackEvent("eventName2", (EventProperties) null, NORMAL);
        verify(mChannel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(NORMAL));
    }

    @Test
    public void trackEventWithNormalCriticalPersistenceFlag() {
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("token");
        target.trackEvent("eventName1", (Map<String, String>) null, CRITICAL);
        target.trackEvent("eventName2", (EventProperties) null, CRITICAL);
        verify(mChannel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(CRITICAL));
    }

    @Test
    public void trackEventWithInvalidFlags() {
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("token");
        target.trackEvent("eventName1", (Map<String, String>) null, 0x03);
        target.trackEvent("eventName2", (EventProperties) null, 0x03);
        verify(mChannel, times(2)).enqueue(isA(EventLog.class), anyString(), eq(DEFAULTS));
        verifyStatic(times(2));
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());
    }
}
