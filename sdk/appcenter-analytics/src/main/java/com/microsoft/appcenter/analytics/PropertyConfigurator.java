package com.microsoft.appcenter.analytics;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

/**
 * Allow overriding Part A properties.
 */
public class PropertyConfigurator extends AbstractChannelListener {

    /**
     * App name to override common schema part A 'app.name'.
     */
    private String mAppName;

    /**
     * App name to override common schema part A 'app.ver'.
     */
    private String mAppVersion;

    /**
     * App name to override common schema part A 'app.locale'.
     */
    private String mAppLocale;

    /**
     * The transmission target which this configurator belongs to.
     */
    private AnalyticsTransmissionTarget mTransmissionTarget;

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
        if (log instanceof CommonSchemaLog && mTransmissionTarget.isEnabled()) {
            AppExtension app = ((CommonSchemaLog) log).getExt().getApp();

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
        }
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
}
