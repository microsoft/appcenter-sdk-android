/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.annotation.SuppressLint;
import android.provider.Settings.Secure;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.DeviceExtension;
import com.microsoft.appcenter.ingestion.models.one.UserExtension;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.context.UserIdContext;

import java.util.Date;
import java.util.Map;

import static com.microsoft.appcenter.Constants.COMMON_SCHEMA_PREFIX_SEPARATOR;

/**
 * Allow overriding Part A properties.
 */
public class PropertyConfigurator extends AbstractChannelListener {

    /**
     * Common schema prefix for Android device IDs.
     */
    private static final String ANDROID_DEVICE_ID_PREFIX = "a" + COMMON_SCHEMA_PREFIX_SEPARATOR;

    /**
     * App name to override common schema part A 'app.name'.
     */
    private String mAppName;

    /**
     * App version to override common schema part A 'app.ver'.
     */
    private String mAppVersion;

    /**
     * App locale to override common schema part A 'app.locale'.
     */
    private String mAppLocale;

    /**
     * User identifier to override common schema part A 'user.localId'.
     */
    private String mUserId;

    /**
     * Flag to enable populating common schema 'device.localId'.
     */
    private boolean mDeviceIdEnabled;

    /**
     * The transmission target which this configurator belongs to.
     */
    private final AnalyticsTransmissionTarget mTransmissionTarget;

    /**
     * Common event properties for this target. Inherited by children.
     */
    private final EventProperties mEventProperties = new EventProperties();

    /**
     * Create a new property configurator.
     *
     * @param transmissionTarget The transmission target of the configurator.
     */
    PropertyConfigurator(AnalyticsTransmissionTarget transmissionTarget) {
        mTransmissionTarget = transmissionTarget;
    }

    /**
     * Override or inherit common schema properties while preparing log.
     *
     * @param log       A log.
     * @param groupName The group name.
     */
    @Override
    public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
        if (shouldOverridePartAProperties(log)) {
            AppExtension app = ((CommonSchemaLog) log).getExt().getApp();
            UserExtension user = ((CommonSchemaLog) log).getExt().getUser();
            DeviceExtension device = ((CommonSchemaLog) log).getExt().getDevice();

            /* Override app name if not null, else use the name of the nearest parent. */
            if (mAppName != null) {
                app.setName(mAppName);
            } else {
                for (AnalyticsTransmissionTarget target = mTransmissionTarget.mParentTarget; target != null; target = target.mParentTarget) {
                    String parentAppName = target.getPropertyConfigurator().getAppName();
                    if (parentAppName != null) {
                        app.setName(parentAppName);
                        break;
                    }
                }
            }

            /* Override app version if not null, else use the version of the nearest parent. */
            if (mAppVersion != null) {
                app.setVer(mAppVersion);
            } else {
                for (AnalyticsTransmissionTarget target = mTransmissionTarget.mParentTarget; target != null; target = target.mParentTarget) {
                    String parentAppVersion = target.getPropertyConfigurator().getAppVersion();
                    if (parentAppVersion != null) {
                        app.setVer(parentAppVersion);
                        break;
                    }
                }
            }

            /* Override app locale if not null, else use the locale of the nearest parent. */
            if (mAppLocale != null) {
                app.setLocale(mAppLocale);
            } else {
                for (AnalyticsTransmissionTarget target = mTransmissionTarget.mParentTarget; target != null; target = target.mParentTarget) {
                    String parentAppLocale = target.getPropertyConfigurator().getAppLocale();
                    if (parentAppLocale != null) {
                        app.setLocale(parentAppLocale);
                        break;
                    }
                }
            }

            /* Override userId if not null, else use the userId of the nearest parent. */
            if (mUserId != null) {
                user.setLocalId(mUserId);
            } else {
                for (AnalyticsTransmissionTarget target = mTransmissionTarget.mParentTarget; target != null; target = target.mParentTarget) {
                    String parentUserId = target.getPropertyConfigurator().getUserId();
                    if (parentUserId != null) {
                        user.setLocalId(parentUserId);
                        break;
                    }
                }
            }

            /* Fill out the device id if it has been collected. */
            if (mDeviceIdEnabled) {

                /* Get device identifier, Secure class already has an in memory cache. */
                @SuppressLint("HardwareIds")
                String androidId = Secure.getString(mTransmissionTarget.mContext.getContentResolver(), Secure.ANDROID_ID);
                device.setLocalId(ANDROID_DEVICE_ID_PREFIX + androidId);
            }
        }
    }

    /**
     * Checks if the log should be overridden by this instance.
     *
     * @param log log.
     * @return true if log should be overridden, false otherwise.
     */
    private boolean shouldOverridePartAProperties(@NonNull Log log) {
        return log instanceof CommonSchemaLog && log.getTag() == mTransmissionTarget &&
                mTransmissionTarget.isEnabled();
    }

    /**
     * Get app name.
     *
     * @return App name.
     */
    private String getAppName() {
        return mAppName;
    }

    /**
     * Override common schema Part A property App.Name.
     *
     * @param appName App name.
     */
    public void setAppName(final String appName) {
        Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

            @Override
            public void run() {
                mAppName = appName;
            }
        });
    }

    /**
     * Get app version.
     *
     * @return App version.
     */
    private String getAppVersion() {
        return mAppVersion;
    }

    /**
     * Override common schema Part A property App.Version.
     *
     * @param appVersion App version.
     */
    public void setAppVersion(final String appVersion) {
        Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

            @Override
            public void run() {
                mAppVersion = appVersion;
            }
        });
    }

    /**
     * Get app locale.
     *
     * @return App locale.
     */
    private String getAppLocale() {
        return mAppLocale;
    }

    /**
     * Override common schema Part A property App.Locale.
     *
     * @param appLocale App Locale.
     */
    public void setAppLocale(final String appLocale) {
        Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

            @Override
            public void run() {
                mAppLocale = appLocale;
            }
        });
    }

    /**
     * Get user id.
     *
     * @return user id.
     */
    private String getUserId() {
        return mUserId;
    }

    /**
     * Set the user identifier.
     * The user identifier needs to start with the c: prefix or must not have a prefix.
     * If the prefix is missing, the c: prefix will be automatically added.
     * The userId cannot be empty or just "c:".
     *
     * @param userId user identifier.
     */
    public void setUserId(final String userId) {
        if (UserIdContext.checkUserIdValidForOneCollector(userId)) {
            Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

                @Override
                public void run() {
                    mUserId = UserIdContext.getPrefixedUserId(userId);
                }
            });
        }
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key. The key must not be null.
     * @param value The boolean value.
     */
    public synchronized void setEventProperty(String key, boolean value) {
        mEventProperties.set(key, value);
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key. The key must not be null.
     * @param value The date value. The value cannot be null.
     */
    public synchronized void setEventProperty(String key, Date value) {
        mEventProperties.set(key, value);
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key. The key must not be null.
     * @param value The double value. The value must not be NaN or infinite.
     */
    public synchronized void setEventProperty(String key, double value) {
        mEventProperties.set(key, value);
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key. The key must not be null.
     * @param value The long value.
     */
    public synchronized void setEventProperty(String key, long value) {
        mEventProperties.set(key, value);
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key. The key must not be null.
     * @param value The string value. The value cannot be null.
     */
    public synchronized void setEventProperty(String key, String value) {
        mEventProperties.set(key, value);
    }

    /**
     * Removes the given key from the common event properties.
     *
     * @param key The property key to be removed.
     */
    public synchronized void removeEventProperty(String key) {
        mEventProperties.getProperties().remove(key);
    }

    /**
     * Enable collection of the Android device identifier for this target.
     * This does not have any effect on child transmission targets.
     */
    public void collectDeviceId() {
        Analytics.getInstance().postCommandEvenIfDisabled(new Runnable() {

            @Override
            public void run() {
                mDeviceIdEnabled = true;
            }
        });
    }

    /*
     * Extracted method to synchronize on each level at once while reading properties.
     * Nesting synchronize between parent/child could lead to deadlocks.
     */
    synchronized void mergeEventProperties(EventProperties mergedProperties) {
        for (Map.Entry<String, TypedProperty> property : mEventProperties.getProperties().entrySet()) {
            String key = property.getKey();
            if (!mergedProperties.getProperties().containsKey(key)) {
                mergedProperties.getProperties().put(key, property.getValue());
            }
        }
    }
}
