package com.microsoft.appcenter.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;

import java.util.Locale;
import java.util.TimeZone;

/**
 * DeviceInfoHelper class to retrieve device information.
 */
public class DeviceInfoHelper {

    /**
     * OS name.
     */
    private static final String OS_NAME = "Android";

    /**
     * Wrapper SDK information to use when building device properties.
     */
    private static WrapperSdk sWrapperSdk;

    /**
     * Gets device information.
     *
     * @param context The context of the application.
     * @return {@link Device}
     * @throws DeviceInfoException If device information cannot be retrieved
     */
    public static synchronized Device getDeviceInfo(Context context) throws DeviceInfoException {
        Device device = new Device();

        /* Application version. */
        PackageInfo packageInfo;
        try {
            PackageManager packageManager = context.getPackageManager();
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            device.setAppVersion(packageInfo.versionName);
            device.setAppBuild(String.valueOf(packageInfo.versionCode));
        } catch (Exception e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Cannot retrieve package info", e);
            throw new DeviceInfoException("Cannot retrieve package info", e);
        }

        /* Application namespace. */
        device.setAppNamespace(context.getPackageName());

        /* Carrier info. */
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String networkCountryIso = telephonyManager.getNetworkCountryIso();
            if (!TextUtils.isEmpty(networkCountryIso)) {
                device.setCarrierCountry(networkCountryIso);
            }
            String networkOperatorName = telephonyManager.getNetworkOperatorName();
            if (!TextUtils.isEmpty(networkOperatorName)) {
                device.setCarrierName(networkOperatorName);
            }
        } catch (Exception e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Cannot retrieve carrier info", e);
        }

        /* Locale. */
        device.setLocale(Locale.getDefault().toString());

        /* Hardware info. */
        device.setModel(Build.MODEL);
        device.setOemName(Build.MANUFACTURER);

        /* OS version. */
        device.setOsApiLevel(Build.VERSION.SDK_INT);
        device.setOsName(OS_NAME);
        device.setOsVersion(Build.VERSION.RELEASE);
        device.setOsBuild(Build.ID);

        /* Screen size. */
        try {
            device.setScreenSize(getScreenSize(context));
        } catch (Exception e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Cannot retrieve screen size", e);
        }

        /* Set SDK name and version. Don't add the BuildConfig import or it will trigger a Javadoc warning... */
        device.setSdkName(com.microsoft.appcenter.BuildConfig.SDK_NAME);
        device.setSdkVersion(com.microsoft.appcenter.BuildConfig.VERSION_NAME);

        /* Timezone offset in minutes (including DST). */
        device.setTimeZoneOffset(TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60 / 1000);

        /* Add wrapper SDK information if any. */
        if (sWrapperSdk != null) {
            device.setWrapperSdkVersion(sWrapperSdk.getWrapperSdkVersion());
            device.setWrapperSdkName(sWrapperSdk.getWrapperSdkName());
            device.setWrapperRuntimeVersion(sWrapperSdk.getWrapperRuntimeVersion());
            device.setLiveUpdateReleaseLabel(sWrapperSdk.getLiveUpdateReleaseLabel());
            device.setLiveUpdateDeploymentKey(sWrapperSdk.getLiveUpdateDeploymentKey());
            device.setLiveUpdatePackageHash(sWrapperSdk.getLiveUpdatePackageHash());
        }

        /* Return device properties. */
        return device;
    }

    /**
     * Gets a size of a device for base orientation.
     *
     * @param context The context of the application.
     * @return A string with {@code <width>x<height>} format.
     */
    @SuppressLint("SwitchIntDef")
    @SuppressWarnings("SuspiciousNameCombination")
    private static String getScreenSize(Context context) {

        /* Guess resolution based on the natural device orientation */
        int screenWidth;
        int screenHeight;
        Display defaultDisplay = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        switch (defaultDisplay.getRotation()) {
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                screenHeight = size.x;
                screenWidth = size.y;
                break;
            default:
                screenWidth = size.x;
                screenHeight = size.y;
        }

         /* Serialize screen resolution */
        return screenWidth + "x" + screenHeight;
    }

    /**
     * Set wrapper SDK information to use when building device properties.
     *
     * @param wrapperSdk wrapper SDK information.
     */
    public static synchronized void setWrapperSdk(WrapperSdk wrapperSdk) {
        sWrapperSdk = wrapperSdk;
    }

    /**
     * Thrown when {@link DeviceInfoHelper} cannot retrieve device information from devices
     */
    public static class DeviceInfoException extends Exception {

        @SuppressWarnings("SameParameterValue")
        public DeviceInfoException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}