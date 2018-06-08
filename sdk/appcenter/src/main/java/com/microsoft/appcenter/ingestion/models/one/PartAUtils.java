package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;

import java.util.Locale;

/**
 * Populate Part A properties.
 */
public class PartAUtils {

    /**
     * Adds part A extension to common schema log from device object in Log.
     *
     * @param src  source log.
     * @param dest destination common schema log.
     */
    public static void addPartAFromLog(Log src, CommonSchemaLog dest, String transmissionTarget) {

        /* TODO: We should cache the extension. */
        Device device = src.getDevice();

        /* Add top level part A fields. */
        dest.setVer("3.0");
        dest.setTimestamp(src.getTimestamp());
        /* TODO: We should cache the ikey for transmission target */
        dest.setIKey("o:" + transmissionTarget.split("-")[0]);

        /* Copy target token also in the set. */
        dest.addTransmissionTarget(transmissionTarget);

        /* Add extension. */
        dest.setExt(new Extensions());

        /* Add protocol extension. */
        dest.getExt().setProtocol(new ProtocolExtension());
        dest.getExt().getProtocol().setDevModel(device.getModel());
        dest.getExt().getProtocol().setDevMake(device.getOemName());

        /* Add user extension. */
        dest.getExt().setUser(new UserExtension());
        dest.getExt().getUser().setLocale(device.getLocale().replace("_", "-"));

        /* Add OS extension. */
        dest.getExt().setOs(new OsExtension());
        dest.getExt().getOs().setName(device.getOsName());
        dest.getExt().getOs().setVer(device.getOsVersion() + "-" + device.getOsBuild() + "-" + device.getOsApiLevel());

        /* TODO: Add app locale. */
        /* Add app extension. */
        dest.getExt().setApp(new AppExtension());
        dest.getExt().getApp().setVer(device.getAppVersion());
        dest.getExt().getApp().setId("a:" + device.getAppNamespace());

        /* TODO: Add network type. */
        /* Add net extension. */
        dest.getExt().setNet(new NetExtension());
        dest.getExt().getNet().setProvider(device.getCarrierName());

        /* Add SDK extension. */
        dest.getExt().setSdk(new SdkExtension());
        dest.getExt().getSdk().setLibVer(device.getSdkName() + "-" + device.getSdkVersion());

        /* Add loc extension. */
        dest.getExt().setLoc(new LocExtension());
        String timezoneOffset = String.format(Locale.US, "%s%02d:%02d",
                device.getTimeZoneOffset() >= 0 ? "+" : "-",
                Math.abs(device.getTimeZoneOffset() / 60),
                Math.abs(device.getTimeZoneOffset() % 60));
        dest.getExt().getLoc().setTz(timezoneOffset);
    }
}
