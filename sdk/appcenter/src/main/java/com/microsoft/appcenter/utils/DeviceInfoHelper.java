/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;

import androidx.annotation.Nullable;

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
     * Country code.
     */
    private static String mCountryCode;

    /**
     * Data residency region.
     */
    private static @Nullable String mDataResidencyRegion;

    public static PackageInfo getPackageInfo(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (Exception e) {
            AppCenterLog.error(LOG_TAG, "Cannot retrieve package info", e);
            return null;
        }
    }

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
        PackageInfo packageInfo = getPackageInfo(context);
        if (packageInfo == null) {
            throw new DeviceInfoException("Cannot retrieve package info");
        }
        device.setAppVersion(packageInfo.versionName);
        device.setAppBuild(String.valueOf(getVersionCode(packageInfo)));

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
            AppCenterLog.error(LOG_TAG, "Cannot retrieve carrier info", e);
        }

        /* Set country code. */
        if (mCountryCode != null) {
            device.setCarrierCountry(mCountryCode);
        }

        /* Set data residency region. */
        device.setDataResidencyRegion(mDataResidencyRegion);

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
            AppCenterLog.error(LOG_TAG, "Cannot retrieve screen size", e);
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

    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static int getVersionCode(PackageInfo packageInfo) {

        /*
         * Only devices running on Android 9+ have the new version code major which modifies the long version code.
         * But we want to report the legacy version code for distribute and for consistency in telemetry
         * with devices using the same app across all os versions.
         * In most apps, versionCodeMajor is 0 so getLongVersionCode would actually return the existing versionCode anyway.
         */
        return packageInfo.versionCode;
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
        Display defaultDisplay;
        Point size = new Point();

        /* Use DeviceManager to avoid android.os.strictmode.IncorrectContextUseViolation when StrictMode is enabled on API 30. */
        DisplayManager displayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        defaultDisplay = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        size.x = displayMetrics.widthPixels;
        size.y = displayMetrics.heightPixels;
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
     * Set the two-letter ISO country code.
     *
     * @param countryCode the two-letter ISO country code.
     */
    public static void setCountryCode(String countryCode) {
        if (countryCode != null && countryCode.length() != 2) {
            AppCenterLog.error(AppCenterLog.LOG_TAG, "App Center accepts only the two-letter ISO country code.");
            return;
        }
        mCountryCode = countryCode;
        AppCenterLog.debug(AppCenterLog.LOG_TAG, String.format("Set country code: %s", countryCode));
    }

    /**
     * Set the country code or any other string to identify the data residency region.
     *
     * @param dataResidencyRegion residency region code.
     */
    public static void setDataResidencyRegion(@Nullable String dataResidencyRegion) {
        mDataResidencyRegion = dataResidencyRegion;
    }

    /**
     * Thrown when {@link DeviceInfoHelper} cannot retrieve device information from devices
     */
    public static class DeviceInfoException extends Exception {

        @SuppressWarnings("SameParameterValue")
        public DeviceInfoException(String detailMessage) {
            super(detailMessage);
        }
    }
}