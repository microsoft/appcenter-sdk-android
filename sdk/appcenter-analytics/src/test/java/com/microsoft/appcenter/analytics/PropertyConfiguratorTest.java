package com.microsoft.appcenter.analytics;

import android.content.ContentResolver;
import android.content.Context;
import android.provider.Settings.Secure;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.DeviceExtension;
import com.microsoft.appcenter.ingestion.models.one.Extensions;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest(Secure.class)
public class PropertyConfiguratorTest extends AbstractAnalyticsTest {

    private static StringTypedProperty typedProperty(String key, String value) {
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName(key);
        stringTypedProperty.setValue(value);
        return stringTypedProperty;
    }

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
    public void setCommonSchemaProperties() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setApp(new AppExtension());

        /* Get property configurator and set properties. */
        PropertyConfigurator pc = Analytics.getTransmissionTarget("test").getPropertyConfigurator();
        pc.setAppVersion("appVersion");
        pc.setAppName("appName");
        pc.setAppLocale("appLocale");
        log.addTransmissionTarget("test");
        pc.onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertEquals("appVersion", log.getExt().getApp().getVer());
        assertEquals("appName", log.getExt().getApp().getName());
        assertEquals("appLocale", log.getExt().getApp().getLocale());
    }

    @Test
    public void collectDeviceId() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setDevice(new DeviceExtension());

        /* Mock context. */
        mockStatic(Secure.class);
        when(Secure.getString(any(ContentResolver.class), anyString())).thenReturn("mockDeviceId");

        /* Get property configurator and collect device ID. */
        PropertyConfigurator pc = Analytics.getTransmissionTarget("test").getPropertyConfigurator();
        pc.collectDeviceId();
        log.addTransmissionTarget("test");
        pc.onPreparingLog(log, "groupName");

        /* Assert device ID is collected. */
        assertEquals("a:mockDeviceId", log.getExt().getDevice().getLocalId());
    }

    @Test
    public void collectDeviceIdSavedWhenDisabled() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setDevice(new DeviceExtension());

        /* Mock context. */
        mockStatic(Secure.class);
        when(Secure.getString(any(ContentResolver.class), anyString())).thenReturn("mockDeviceId");

        /* Disable Analytics. */
        Analytics.setEnabled(false).get();

        /* Get property configurator and collect device ID. */
        PropertyConfigurator pc = Analytics.getTransmissionTarget("test").getPropertyConfigurator();
        pc.collectDeviceId();
        log.addTransmissionTarget("test");

        /* Enable and simulate log preparing. */
        Analytics.setEnabled(true).get();
        pc.onPreparingLog(log, "groupName");

        /* Assert device ID is collected. */
        assertEquals("a:mockDeviceId", log.getExt().getDevice().getLocalId());
    }

    @Test
    public void commonSchemaPropertiesNotSetWhenDisabled() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setApp(new AppExtension());

        /* Get target, disable it, and set properties. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.setEnabledAsync(false).get();
        target.getPropertyConfigurator().setAppVersion("appVersion");
        target.getPropertyConfigurator().setAppName("appName");
        target.getPropertyConfigurator().setAppLocale("appLocale");
        log.addTransmissionTarget("test");
        target.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties are null. */
        assertNull(log.getExt().getApp().getVer());
        assertNull(log.getExt().getApp().getName());
        assertNull(log.getExt().getApp().getLocale());

        /* The properties are not applied but are saved, if we enable now we can see the values. */
        target.setEnabledAsync(true).get();
        target.getPropertyConfigurator().onPreparingLog(log, "groupName");
        assertEquals("appVersion", log.getExt().getApp().getVer());
        assertEquals("appName", log.getExt().getApp().getName());
        assertEquals("appLocale", log.getExt().getApp().getLocale());
    }

    @Test
    public void inheritCommonSchemaPropertiesFromGrandparent() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setApp(new AppExtension());

        /* Set properties on parent to override unset properties on child */
        AnalyticsTransmissionTarget grandparent = Analytics.getTransmissionTarget("grandparent");
        grandparent.getPropertyConfigurator().setAppVersion("appVersion");
        grandparent.getPropertyConfigurator().setAppName("appName");
        grandparent.getPropertyConfigurator().setAppLocale("appLocale");

        /* Mock Secure and set device ID. */
        mockStatic(Secure.class);
        when(Secure.getString(any(ContentResolver.class), anyString())).thenReturn("mockDeviceId");

        /* Set up hierarchy. */
        AnalyticsTransmissionTarget parent = grandparent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Simulate channel callbacks. */
        log.addTransmissionTarget("child");
        grandparent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        parent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        child.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertEquals("appVersion", log.getExt().getApp().getVer());
        assertEquals("appName", log.getExt().getApp().getName());
        assertEquals("appLocale", log.getExt().getApp().getLocale());
    }

    @Test
    public void checkGrandParentNotOverriddenByDescendants() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setApp(new AppExtension());

        /* Set up hierarchy. */
        AnalyticsTransmissionTarget grandparent = Analytics.getTransmissionTarget("grandparent");
        AnalyticsTransmissionTarget parent = grandparent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Set properties on parent to override unset properties on child (but not grandparent). */
        parent.getPropertyConfigurator().setAppVersion("appVersion");
        parent.getPropertyConfigurator().setAppName("appName");
        parent.getPropertyConfigurator().setAppLocale("appLocale");

        /* Also set 1 on child. */
        child.getPropertyConfigurator().setAppName("childName");

        /* Simulate channel callbacks. */
        log.addTransmissionTarget("grandParent");
        grandparent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        parent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        child.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertNull(log.getExt().getApp().getVer());
        assertNull(log.getExt().getApp().getName());
        assertNull(log.getExt().getApp().getLocale());
    }

    @Test
    public void grandparentsHaveNoPropertiesSet() {
        CommonSchemaLog log = new CommonSchemaEventLog();
        log.setExt(new Extensions());
        log.getExt().setApp(new AppExtension());

        /* Set up empty chain of parents. */
        AnalyticsTransmissionTarget grandparent = Analytics.getTransmissionTarget("grandparent");
        AnalyticsTransmissionTarget parent = grandparent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Simulate channel callbacks. */
        log.addTransmissionTarget("child");
        grandparent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        parent.getPropertyConfigurator().onPreparingLog(log, "groupName");
        child.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertNull(log.getExt().getApp().getVer());
        assertNull(log.getExt().getApp().getName());
        assertNull(log.getExt().getApp().getLocale());
    }

    @Test
    public void appCenterLogDoesNotOverride() {
        Log log = mock(EventLog.class);

        /* Get property configurator and set properties. */
        PropertyConfigurator pc = Analytics.getTransmissionTarget("test").getPropertyConfigurator();
        pc.setAppVersion("appVersion");
        pc.setAppName("appName");
        pc.setAppLocale("appLocale");
        pc.onPreparingLog(log, "groupName");

        /* Verify no interactions with the log. */
        verifyNoMoreInteractions(log);
    }

    @Test
    public void setCommonEventProperties() {

        /* Create transmission target and add property. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.getPropertyConfigurator().setEventProperty("key", "value");

        /* Track event without property. */
        target.trackEvent("eventName");

        /* Check event. */
        ArgumentCaptor<EventLog> eventLogArg = ArgumentCaptor.forClass(EventLog.class);
        verify(mChannel).enqueue(eventLogArg.capture(), anyString());
        EventLog log = eventLogArg.getValue();
        assertNotNull(log);
        assertEquals(Collections.singleton("test"), log.getTransmissionTargetTokens());
        assertEquals("eventName", log.getName());
        assertNull(log.getProperties());
        List<TypedProperty> typedProperties = new ArrayList<>();
        typedProperties.add(typedProperty("key", "value"));
        assertEquals(typedProperties, log.getTypedProperties());
    }

    @Test
    public void setCommonEventPropertiesWithNullMapProperties() {

        /* Create transmission target and add property. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.getPropertyConfigurator().setEventProperty("key", "value");

        /* Track event without property. */
        target.trackEvent("eventName", (Map<String, String>) null);

        /* Check event. */
        ArgumentCaptor<EventLog> eventLogArg = ArgumentCaptor.forClass(EventLog.class);
        verify(mChannel).enqueue(eventLogArg.capture(), anyString());
        EventLog log = eventLogArg.getValue();
        assertNotNull(log);
        assertEquals(Collections.singleton("test"), log.getTransmissionTargetTokens());
        assertEquals("eventName", log.getName());
        assertNull(log.getProperties());
        List<TypedProperty> typedProperties = new ArrayList<>();
        typedProperties.add(typedProperty("key", "value"));
        assertEquals(typedProperties, log.getTypedProperties());
    }

    @Test
    public void setAndRemoveCommonEventPropertiesWithMerge() {

        /* Create transmission target and add 2 properties (1 overwritten). */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");
        target.getPropertyConfigurator().setEventProperty("key1", "value1");
        target.getPropertyConfigurator().setEventProperty("key2", "ignore");
        target.getPropertyConfigurator().setEventProperty("remove", "ignore");

        /* Remove some properties. */
        target.getPropertyConfigurator().removeEventProperty("remove");
        target.getPropertyConfigurator().removeEventProperty("notFound");

        /* Prepare properties. */
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("key2", "value2");
        properties.put("key3", "value3");

        /* Track event with extra properties. */
        target.trackEvent("eventName", properties);

        /* Check event. */
        ArgumentCaptor<EventLog> eventLogArg = ArgumentCaptor.forClass(EventLog.class);
        verify(mChannel).enqueue(eventLogArg.capture(), anyString());
        EventLog log = eventLogArg.getValue();
        assertNotNull(log);
        assertEquals(Collections.singleton("test"), log.getTransmissionTargetTokens());
        assertEquals("eventName", log.getName());
        assertNull(log.getProperties());
        List<TypedProperty> typedProperties = new ArrayList<>();

        /* The order should be property configurator -> properties which is in the order of override properties. */
        typedProperties.add(typedProperty("key1", "value1"));
        typedProperties.add(typedProperty("key2", "value2"));
        typedProperties.add(typedProperty("key3", "value3"));
        assertEquals(typedProperties, log.getTypedProperties());
    }

    @Test
    public void trackEventWithEmptyProperties() {

        /* Create transmission target. */
        AnalyticsTransmissionTarget target = Analytics.getTransmissionTarget("test");

        /* Track event with empty properties. */
        target.trackEvent("eventName", Collections.<String, String>emptyMap());

        /* Check what event was sent. */
        ArgumentCaptor<EventLog> eventLogArg = ArgumentCaptor.forClass(EventLog.class);
        verify(mChannel).enqueue(eventLogArg.capture(), anyString());
        EventLog log = eventLogArg.getValue();
        assertNotNull(log);
        assertEquals(Collections.singleton("test"), log.getTransmissionTargetTokens());
        assertEquals("eventName", log.getName());
        assertNull(log.getProperties());
        assertEquals(Collections.emptyList(), log.getTypedProperties());
    }

    @Test
    public void eventPropertiesCascading() {

        /* Create transmission target hierarchy. */
        AnalyticsTransmissionTarget grandParent = Analytics.getTransmissionTarget("grandParent");
        AnalyticsTransmissionTarget parent = grandParent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");

        /* Set common properties across hierarchy with some overrides. */
        grandParent.getPropertyConfigurator().setEventProperty("a", "1");
        grandParent.getPropertyConfigurator().setEventProperty("b", "2");
        grandParent.getPropertyConfigurator().setEventProperty("c", "3");

        /* Override some. */
        parent.getPropertyConfigurator().setEventProperty("a", "11");
        parent.getPropertyConfigurator().setEventProperty("b", "22");

        /* And a new one. */
        parent.getPropertyConfigurator().setEventProperty("d", "44");

        /* Just to show we still get value from grandParent if we remove an override. */
        parent.getPropertyConfigurator().setEventProperty("c", "33");
        parent.getPropertyConfigurator().removeEventProperty("c");

        /* Overrides in child. */
        child.getPropertyConfigurator().setEventProperty("d", "444");

        /* New in child. */
        child.getPropertyConfigurator().setEventProperty("e", "555");
        child.getPropertyConfigurator().setEventProperty("f", "666");

        /* Track event in child. Override properties in trackEvent. */
        Map<String, String> properties = new LinkedHashMap<>();
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
        assertNull(log.getProperties());
        List<TypedProperty> typedProperties = new ArrayList<>();

        /* The order should be child -> parent -> grandparent which is in the order of override properties. */
        typedProperties.add(typedProperty("d", "444"));
        typedProperties.add(typedProperty("e", "555"));
        typedProperties.add(typedProperty("f", "6666"));
        typedProperties.add(typedProperty("a", "11"));
        typedProperties.add(typedProperty("b", "22"));
        typedProperties.add(typedProperty("c", "3"));
        typedProperties.add(typedProperty("g", "7777"));
        assertEquals(typedProperties, log.getTypedProperties());
    }
}
