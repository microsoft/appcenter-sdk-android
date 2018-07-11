package com.microsoft.appcenter.analytics;

import android.content.Context;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

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
    }

    @Test
    public void testGetTransmissionTarget() {
        assertNull(Analytics.getTransmissionTarget(""));
        assertNotNull(Analytics.getTransmissionTarget("token"));
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
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("token");
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
                        boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getProperties() == null;
                        boolean tokenMatch;
                        if (defaultToken != null) {
                            tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains(defaultToken);
                        } else {
                            tokenMatch = eventLog.getTransmissionTargetTokens().isEmpty();
                        }
                        return nameAndPropertiesMatch && tokenMatch;
                    }
                    return false;
                }
            }), anyString());
        } else {
            verify(mChannel, never()).enqueue(isA(EventLog.class), anyString());
        }
        reset(mChannel);

        /* Track event via transmission target method. */
        target.trackEvent("name");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getProperties() == null;
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
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
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getProperties().size() == 1 && "valid".equals(eventLog.getProperties().get("valid"));
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token2");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
        reset(mChannel);

        /* Create a child transmission target and track event. */
        AnalyticsTransmissionTarget childTarget = target.getTransmissionTarget("token3");
        childTarget.trackEvent("name");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("name") && eventLog.getProperties() == null;
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("token3");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
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
        }), anyString());

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
        }), anyString());
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
        }), anyString());

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
        }), anyString());
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
        verify(mChannel, never()).enqueue(any(Log.class), anyString());
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
    public void setCommonEventProperties() {

        /* Create transmission target and add property. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.setEventProperty("key", "value");

        /* Track event without property. */
        target.trackEvent("eventName");
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    Map<String, String> expectedProperties = new HashMap<>();
                    expectedProperties.put("key", "value");
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("eventName") && expectedProperties.equals(eventLog.getProperties());
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("test");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void setAndRemoveCommonEventPropertiesWithMerge() {

        /* Create transmission target and add 2 properties (1 overwritten). */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.setEventProperty("key1", "value1");
        target.setEventProperty("key2", "ignore");
        target.setEventProperty("remove", "ignore");

        /* Remove some properties. */
        target.removeEventProperty("remove");
        target.removeEventProperty("notFound");

        /* Prepare properties. */
        Map<String, String> properties = new HashMap<>();
        properties.put("key2", "value2");
        properties.put("key3", "value3");

        /* Track event with extra properties. */
        target.trackEvent("eventName", properties);
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    Map<String, String> expectedProperties = new HashMap<>();
                    expectedProperties.put("key1", "value1");
                    expectedProperties.put("key2", "value2");
                    expectedProperties.put("key3", "value3");
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("eventName") && expectedProperties.equals(eventLog.getProperties());
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("test");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void trackEventWithEmptyProperties() {

        /* Create transmission target. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");

        /* Track event with empty properties. */
        target.trackEvent("eventName", Collections.<String, String>emptyMap());
        verify(mChannel).enqueue(argThat(new ArgumentMatcher<Log>() {

            @Override
            public boolean matches(Object item) {
                if (item instanceof EventLog) {
                    EventLog eventLog = (EventLog) item;
                    Map<String, String> expectedProperties = Collections.emptyMap();
                    boolean nameAndPropertiesMatch = eventLog.getName().equals("eventName") && expectedProperties.equals(eventLog.getProperties());
                    boolean tokenMatch = eventLog.getTransmissionTargetTokens().size() == 1 && eventLog.getTransmissionTargetTokens().contains("test");
                    return nameAndPropertiesMatch && tokenMatch;
                }
                return false;
            }
        }), anyString());
    }

    @Test
    public void eventPropertiesCascading() {

        /* Create transmission target hierarchy. */
        AnalyticsTransmissionTarget grandParent = Analytics.getTransmissionTarget("grandParent");
        AnalyticsTransmissionTarget parent = grandParent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Set common properties across hierarchy with some overrides. */
        grandParent.setEventProperty("a", "1");
        grandParent.setEventProperty("b", "2");
        grandParent.setEventProperty("c", "3");

        /* Override some. */
        parent.setEventProperty("a", "11");
        parent.setEventProperty("b", "22");

        /* And a new one. */
        parent.setEventProperty("d", "44");

        /* Just to show we still get value from grandParent if we remove an override. */
        parent.setEventProperty("c", "33");
        parent.removeEventProperty("c");

        /* Overrides in child. */
        child.setEventProperty("d", "444");

        /* New in child. */
        child.setEventProperty("e", "555");
        child.setEventProperty("f", "666");

        /* Track event in child. Override properties in trackEvent. */
        Map<String, String> properties = new HashMap<>();
        properties.put("f", "6666");
        properties.put("g", "7777");
        child.trackEvent("eventName", properties);

        /* Verify log that was sent. */
        ArgumentCaptor<EventLog> logArgumentCaptor = ArgumentCaptor.forClass(EventLog.class);
        verify(mChannel).enqueue(logArgumentCaptor.capture(), anyString());
        EventLog log = logArgumentCaptor.getValue();
        assertNotNull(log);
        assertEquals("eventName", log.getName());
        assertEquals(1, log.getTransmissionTargetTokens().size());
        assertTrue(log.getTransmissionTargetTokens().contains("child"));

        /* Verify properties. */
        Map<String, String> expectedProperties = new HashMap<>();
        expectedProperties.put("a", "11");
        expectedProperties.put("b", "22");
        expectedProperties.put("c", "3");
        expectedProperties.put("d", "444");
        expectedProperties.put("e", "555");
        expectedProperties.put("f", "6666");
        expectedProperties.put("g", "7777");
        assertEquals(expectedProperties, log.getProperties());
    }
}
