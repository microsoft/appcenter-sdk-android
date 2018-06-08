package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;

import org.junit.Test;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PartAUtilsTest {

    @Test
    public void coverInit() {
        new PartAUtils();
    }

    @Test
    public void checkPartAConversionPositiveTimeZoneOffSet() {
        checkPartAConversion(480, "+08:00");
    }

    @Test
    public void checkPartAConversionNegativeTimeZoneOffSet() {
        checkPartAConversion(-480, "-08:00");
    }

    /**
     * Convert to Part A and check.
     */
    private void checkPartAConversion(int appCenterTimeZoneOffset, String commonSchemaTimeZoneOffset) {

        /* Create App Center models, starting with the device object. */
        Device device = new Device();
        device.setModel("model");
        device.setOemName("oemName");
        device.setLocale("en_US");
        device.setOsName("osName");
        device.setOsVersion("8.1.0");
        device.setOsBuild("ABC.123");
        device.setOsApiLevel(23);
        device.setAppVersion("1.0.0");
        device.setAppNamespace("com.appcenter.test");
        device.setCarrierName("carrierName");
        device.setSdkName("appcenter.android");
        device.setSdkVersion("1.5.0");
        device.setTimeZoneOffset(appCenterTimeZoneOffset);

        /* App Center timestamp and transmission targets. */
        Date timestamp = new Date();
        String transmissionTarget = "T1UUID1-T2UUID2";
        Log log = mock(Log.class);
        when(log.getDevice()).thenReturn(device);
        when(log.getTimestamp()).thenReturn(timestamp);

        /* Convert. */
        MockCommonSchemaLog commonSchemaLog = new MockCommonSchemaLog();
        PartAUtils.addPartAFromLog(log, commonSchemaLog, transmissionTarget);

        /* Verify conversion. */
        assertEquals("3.0", commonSchemaLog.getVer());
        assertEquals(timestamp, commonSchemaLog.getTimestamp());
        assertEquals("o:T1UUID1", commonSchemaLog.getIKey());
        assertNotNull(commonSchemaLog.getExt());
        assertNotNull(commonSchemaLog.getExt().getProtocol());
        assertEquals("model", commonSchemaLog.getExt().getProtocol().getDevModel());
        assertEquals("oemName", commonSchemaLog.getExt().getProtocol().getDevMake());
        assertNotNull(commonSchemaLog.getExt().getUser());
        assertEquals("en-US", commonSchemaLog.getExt().getUser().getLocale());
        assertNotNull(commonSchemaLog.getExt().getOs());
        assertEquals("osName", commonSchemaLog.getExt().getOs().getName());
        assertEquals("8.1.0-ABC.123-23", commonSchemaLog.getExt().getOs().getVer());
        assertNotNull(commonSchemaLog.getExt().getApp());
        assertEquals("1.0.0", commonSchemaLog.getExt().getApp().getVer());
        assertEquals("a:com.appcenter.test", commonSchemaLog.getExt().getApp().getId());
        assertNotNull(commonSchemaLog.getExt().getNet());
        assertEquals("carrierName", commonSchemaLog.getExt().getNet().getProvider());
        assertNotNull(commonSchemaLog.getExt().getSdk());
        assertEquals("appcenter.android-1.5.0", commonSchemaLog.getExt().getSdk().getLibVer());
        assertNotNull(commonSchemaLog.getExt().getLoc());
        assertEquals(commonSchemaTimeZoneOffset, commonSchemaLog.getExt().getLoc().getTz());
        assertEquals(Collections.singleton(transmissionTarget), commonSchemaLog.getTransmissionTargetTokens());
    }
}
