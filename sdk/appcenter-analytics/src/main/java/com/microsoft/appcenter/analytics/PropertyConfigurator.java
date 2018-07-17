package com.microsoft.appcenter.analytics;

import android.support.annotation.NonNull;
import android.util.Property;

import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.AppExtension;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

/**
 * Allow overriding Part A properties.
 */
public class PropertyConfigurator extends AbstractChannelListener {

    private String mAppName;

    private String mAppVersion;

    private String mAppLocale;

    private AnalyticsTransmissionTarget mTransmissionTarget;

    PropertyConfigurator(Channel channel, AnalyticsTransmissionTarget transmissionTarget) {
        mTransmissionTarget = transmissionTarget;
        channel.addListener(PropertyConfigurator.class);
    }

    @Override
    public void onPreparingLog(@NonNull Log log, @NonNull String groupName) {
        if (log instanceof CommonSchemaLog) {
            AppExtension app = ((CommonSchemaLog) log).getExt().getApp();

            /* Override app name if not null, else use the name of the nearest parent. */
            if (mAppName != null) {
                app.setId(mAppName);
            } else {
                AnalyticsTransmissionTarget parent = mTransmissionTarget.mParentTarget;
                while (parent != null) {
                    String parentAppName = parent.getPropertyConfigurator().getAppName();
                    if (parentAppName != null) {
                        app.setId(parentAppName);
                        break;
                    }
                    parent = parent.mParentTarget;
                }
            }

            /* Override app version if not null, else use the version of the nearest parent. */
            if (mAppVersion != null) {
                app.setVer(mAppVersion);
            } else {
                AnalyticsTransmissionTarget parent = mTransmissionTarget.mParentTarget;
                while (parent != null) {
                    String parentAppVersion = parent.getPropertyConfigurator().getAppVersion();
                    if (parentAppVersion != null) {
                        app.setVer(parentAppVersion);
                        break;
                    }
                    parent = parent.mParentTarget;
                }
            }

            /* Override app locale if not null, else use the locale of the nearest parent. */
            if (mAppLocale != null) {
                app.setLocale(mAppLocale);
            } else {
                AnalyticsTransmissionTarget parent = mTransmissionTarget.mParentTarget;
                while (parent != null) {
                    String parentAppLocale = parent.getPropertyConfigurator().getAppLocale();
                    if (parentAppLocale != null) {
                        app.setLocale(parentAppLocale);
                        break;
                    }
                    parent = parent.mParentTarget;
                }
            }
        }
    }

    /**
     * Override common schema Part A property App.Name.
     *
     * @param appName   App name
     */
    public void setAppName(String appName) {
        mAppName = appName;
    }

    protected String getAppName() {
        return mAppName;
    }

    /**
     * Override common schema Part A property App.Version.
     *
     * @param appVersion    App version
     */
    public void setAppVersion(String appVersion) {
        mAppVersion = appVersion;
    }

    protected String getAppVersion() {
        return mAppVersion;
    }

    /**
     * Override common schema Part A property App.Locale.
     *
     * @param appLocale     App Locale
     */
    public void setAppLocale(String appLocale) {
        mAppLocale = appLocale;
    }

    protected String getAppLocale() {
        return mAppLocale;
    }
}
