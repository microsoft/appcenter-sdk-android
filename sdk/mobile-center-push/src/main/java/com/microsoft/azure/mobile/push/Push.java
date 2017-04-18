package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * Push notifications interface.
 */
public class Push extends AbstractMobileCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Push";

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the push group.
     */
    private static final String PUSH_GROUP = "group_push";

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store push token.
     */
    @VisibleForTesting
    static final String PREFERENCE_KEY_PUSH_TOKEN = PREFERENCE_PREFIX + "push_token";

    /**
     * Preference key to store if firebase analytics collections is enabled.
     */
    @VisibleForTesting
    static final String PREFERENCE_KEY_ANALYTICS_ENABLED = PREFERENCE_PREFIX + "analytics_enabled";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Push sInstance;

    /**
     * The PNS handle for this installation.
     */
    private String mPushToken;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Init.
     */
    private Push() {
        mFactories = new HashMap<>();
        mFactories.put(PushInstallationLog.TYPE, new PushInstallationLogFactory());
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Push getInstance() {
        if (sInstance == null) {
            sInstance = new Push();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Push service is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Push service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    @SuppressWarnings("WeakerAccess")
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Enable firebase analytics collection.
     *
     * @param context the context to retrieve FirebaseAnalytics instance.
     */
    public static void enableFirebaseAnalytics(@NonNull Context context) {
        MobileCenterLog.debug(LOG_TAG, "Enabling firebase analytics collection.");
        setFirebaseAnalyticsEnabled(context, true);
    }

    /**
     * Enable or disable firebase analytics collection.
     *
     * @param context the context to retrieve FirebaseAnalytics instance.
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    @SuppressWarnings("MissingPermission")
    private static void setFirebaseAnalyticsEnabled(@NonNull Context context, boolean enabled) {
        FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled);
        PreferencesStorage.putBoolean(PREFERENCE_KEY_ANALYTICS_ENABLED, enabled);
    }

    /**
     * Enqueue a push installation log.
     *
     * @param pushToken the push token value
     */
    private void enqueuePushInstallationLog(@NonNull String pushToken) {
        PushInstallationLog log = new PushInstallationLog();
        log.setPushToken(pushToken);
        mChannel.enqueue(log, PUSH_GROUP);
    }

    /**
     * Handle push token update success.
     *
     * @param pushToken the push token value
     */
    @VisibleForTesting
    synchronized void onTokenRefresh(@NonNull String pushToken) {
        if (isInactive())
            return;
        if (mPushToken != null && mPushToken.equals(pushToken))
            return;
        MobileCenterLog.debug(LOG_TAG, "Push token: " + pushToken);
        PreferencesStorage.putString(PREFERENCE_KEY_PUSH_TOKEN, pushToken);
        enqueuePushInstallationLog(pushToken);
        mPushToken = pushToken;
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    private synchronized void applyEnabledState(boolean enabled) {
        if (enabled && mChannel != null) {
            String token = FirebaseInstanceId.getInstance().getToken();
            if (token != null) {
                onTokenRefresh(token);
            }
        } else {

            /* Reset module state if disabled */
            mPushToken = null;
        }
    }

    @Override
    protected String getGroupName() {
        return PUSH_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    protected int getTriggerCount() {
        return 1;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        applyEnabledState(isInstanceEnabled());
        if (!PreferencesStorage.getBoolean(PREFERENCE_KEY_ANALYTICS_ENABLED)) {
            MobileCenterLog.debug(LOG_TAG, "Disabling firebase analytics collection by default.");
            setFirebaseAnalyticsEnabled(context, false);
        }
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        applyEnabledState(enabled);
    }
}
