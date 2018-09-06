package com.microsoft.appcenter.analytics;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.provider.Settings.Secure;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.DeviceExtension;

import java.util.HashMap;
import java.util.Map;

/**
 * Allow overriding Part A properties.
 */
public class PropertyConfigurator extends AbstractChannelListener {

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
     * The device id to populate common schema 'device.localId'.
     */
    private String mDeviceId;

    /**
     * The transmission target which this configurator belongs to.
     */
    private final AnalyticsTransmissionTarget mTransmissionTarget;

    /**
     * Common event properties for this target. Inherited by children.
     */
    private final Map<String, String> mEventProperties = new HashMap<>();

    /**
     * Create a new property configurator.
     *
     * @param channel            The channel for this listener.
     * @param transmissionTarget The tranmission target of the configurator.
     */
    PropertyConfigurator(Channel channel, AnalyticsTransmissionTarget transmissionTarget) {
        mTransmissionTarget = transmissionTarget;
        if (channel != null) {
            channel.addListener(this);
        }
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

            /* Fill out the device id if it has been collected, or use the device id of the nearest parent. */
            if (mDeviceId != null) {
                device.setLocalId(mDeviceId);
            }
            else {
                for (AnalyticsTransmissionTarget target = mTransmissionTarget.mParentTarget; target != null; target = target.mParentTarget) {
                    String parentDeviceId = target.getPropertyConfigurator().getDeviceId();
                    if (parentDeviceId != null) {
                        device.setLocalId(parentDeviceId);
                        break;
                    }
                }
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
        String targetToken = mTransmissionTarget.getTransmissionTargetToken();
        return log instanceof CommonSchemaLog && mTransmissionTarget.isEnabled()
                && log.getTransmissionTargetTokens().contains(targetToken);
    }

    /**
     * Get app name. Used for checking parents for property inheritance.
     *
     * @return App name
     */
    private String getAppName() {
        return mAppName;
    }

    /**
     * Override common schema Part A property App.Name.
     *
     * @param appName App name
     */
    public void setAppName(String appName) {
        mAppName = appName;
    }

    /**
     * Get app version. Used for checking parents for property inheritance.
     *
     * @return App version
     */
    private String getAppVersion() {
        return mAppVersion;
    }

    /**
     * Override common schema Part A property App.Version.
     *
     * @param appVersion App version
     */
    public void setAppVersion(String appVersion) {
        mAppVersion = appVersion;
    }

    /**
     * Get app locale. Used for checking parents for property inheritance.
     *
     * @return App locale
     */
    private String getAppLocale() {
        return mAppLocale;
    }

    /**
     * Override common schema Part A property App.Locale.
     *
     * @param appLocale App Locale
     */
    public void setAppLocale(String appLocale) {
        mAppLocale = appLocale;
    }

    /**
     * Get device id. Used for checking parents for property inheritance.
     *
     * @return
     */
    private String getDeviceId() {
        return mDeviceId;
    }

    /**
     * Add or overwrite the given key for the common event properties. Properties will be inherited
     * by children of this transmission target.
     *
     * @param key   The property key.
     * @param value The property value.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void setEventProperty(String key, String value) {
        mEventProperties.put(key, value);
    }

    /**
     * Removes the given key from the common event properties.
     *
     * @param key The property key to be removed.
     */
    @SuppressWarnings("WeakerAccess")
    public synchronized void removeEventProperty(String key) {
        mEventProperties.remove(key);
    }

    @SuppressLint("HardwareIds")
    public void collectDeviceId() {
        mDeviceId = Secure.getString(mTransmissionTarget.getContext().getContentResolver(), Secure.ANDROID_ID);
    }

    /*
     * Extracted method to synchronize on each level at once while reading properties.
     * Nesting synchronize between parent/child could lead to deadlocks.
     */
    synchronized void mergeEventProperties(Map<String, String> mergedProperties) {
        for (Map.Entry<String, String> property : mEventProperties.entrySet()) {
            String key = property.getKey();
            if (!mergedProperties.containsKey(key)) {
                mergedProperties.put(key, property.getValue());
            }
        }
    }
}
