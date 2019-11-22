/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.TreeSet;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * DeviceInfoHelper class to retrieve device information.
 */
public class DeviceInfoHelper {

    /**
     * Devices' history.
     */
    private static SortedSet<DeviceHistory> mSetDevices = new TreeSet<>();

    /**
     * OS name.
     */
    private static final String OS_NAME = "Android";

    /**
     * Wrapper SDK information to use when building device properties.
     */
    private static WrapperSdk sWrapperSdk;

    /**
     * Flag to remember whether we already refresh devices' history or not.
     */
    private static boolean mNeedRefresh = true;

    /**
     * Preference storage key for last device info.
     */
    private static final String PREF_KEY_LAST_DEVICE_INFO = "com.microsoft.appcenter.crashes.deviceinfo";

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
            device.setAppBuild(String.valueOf(getVersionCode(packageInfo)));
        } catch (Exception e) {
            AppCenterLog.error(LOG_TAG, "Cannot retrieve package info", e);
            throw new DeviceInfoException("Cannot retrieve package info", e);
        }

        /* Application namespace. */
        device.setAppNamespace(context.getPackageName());

        /* Carrier info. */
        try {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            @SuppressWarnings("ConstantConditions")
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

        //noinspection ConstantConditions
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
     * Load devices' history.
     *
     * @param context The context of the application.
     */
    public static synchronized void loadHistoryDevices(Context context) {
        LogSerializer logSerializer = new DefaultLogSerializer();
        try {
            mSetDevices = logSerializer.deserializeDevices(SharedPreferencesManager.getStringSet(PREF_KEY_LAST_DEVICE_INFO));
            refreshHistoryDevice(context);
        } catch (java.lang.Exception e) {
            AppCenterLog.error(LOG_TAG, "Failed to deserialize devices' information: " + e);
        }
    }

    /**
     * Add current device history only to preference.
     */
    private static synchronized void refreshHistoryDevice(Context context) throws DeviceInfoException {

        /* If current device is not already stored in storage. */
        if (mNeedRefresh) {
            DeviceHistory currentDeviceHistory = new DeviceHistory(System.currentTimeMillis(), getDeviceInfo(context));
            mSetDevices.add(currentDeviceHistory);
            saveDevices();
            mSetDevices.remove(currentDeviceHistory);
            mNeedRefresh = false;
        }
    }

    /**
     * Return device information by timestamp.
     *
     * @return Device information.
     */
    public static synchronized Device getDeviceInfoByTimestamp(Long timestamp) {
        List<DeviceHistory> devices = new ArrayList(mSetDevices);
        int index = Collections.binarySearch(devices, new DeviceHistory(timestamp, null));

        /* If search return negative number we should select value the smaller target number and convert to the positive value. */
        if (index < 0) {
            index *= -1;
            index -= 1;
        }
        if (index == 0) {
            return devices.get(0).getDevice();
        } else if (index == devices.size()) {
            return devices.get(devices.size() - 1).getDevice();
        } else {
            return devices.get(index - 1).getDevice();
        }
    }

    /**
     * Clear the last devices' history and save current device information.
     */
    public static synchronized void clearHistoryDevices() {
       DeviceHistory currentDevice = mSetDevices.last();
       mSetDevices.clear();
       mSetDevices.add(currentDevice);
       saveDevices();
    }

    /**
     * Save devices' history.
     */
    private static synchronized void saveDevices() {
        LogSerializer logSerializer = new DefaultLogSerializer();
        try {
            Set<String> serializeDevices = logSerializer.serializeDevices(mSetDevices);
            SharedPreferencesManager.putStringSet(PREF_KEY_LAST_DEVICE_INFO, serializeDevices);
        } catch (java.lang.Exception e) {
            AppCenterLog.error(LOG_TAG, "Failed to save current device information: " + e);
        }
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