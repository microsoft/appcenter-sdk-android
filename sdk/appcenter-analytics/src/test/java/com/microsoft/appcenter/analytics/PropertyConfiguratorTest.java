package com.microsoft.appcenter.analytics;

import android.content.Context;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.Extensions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class PropertyConfiguratorTest extends AbstractAnalyticsTest {

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
        pc.onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertEquals("appVersion", log.getExt().getApp().getVer());
        assertEquals("appName", log.getExt().getApp().getName());
        assertEquals("appLocale", log.getExt().getApp().getLocale());
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
        target.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties are null. */
        assertNull(log.getExt().getApp().getVer());
        assertNull(log.getExt().getApp().getName());
        assertNull(log.getExt().getApp().getLocale());
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

        AnalyticsTransmissionTarget parent = grandparent.getTransmissionTarget("parent");
        AnalyticsTransmissionTarget child = parent.getTransmissionTarget("child");
        child.getPropertyConfigurator().onPreparingLog(log, "groupName");

        /* Assert properties set on common schema. */
        assertEquals("appVersion", log.getExt().getApp().getVer());
        assertEquals("appName", log.getExt().getApp().getName());
        assertEquals("appLocale", log.getExt().getApp().getLocale());
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
}
