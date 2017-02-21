package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Push notifications interface
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
    static final String PREFERENCE_KEY_PUSH_TOKEN = PREFERENCE_PREFIX + "push_token";


    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Push sInstance = null;

    /**
     *
     */
    private String mSenderId = null;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Application context, if not null it means onStart was called.
     */
    private Context mContext;

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
     *
     * @param senderId
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void setSenderId(String senderId) {
        getInstance().setInstanceSenderId(senderId);
    }

    /**
     * Enqueue a start session log
     *
     * @param pushToken
     */
    private void enqueuePushInstallationLog(@NonNull String pushToken) {
        PushInstallationLog log = new PushInstallationLog();
        log.setPushToken(pushToken);
        mChannel.enqueue(log, PUSH_GROUP);
    }

    private synchronized void handlePushToken(@NonNull String pushToken) {
        MobileCenterLog.debug(LOG_TAG, "Push token: " + pushToken);
        PreferencesStorage.putString(PREFERENCE_KEY_PUSH_TOKEN, pushToken);
        enqueuePushInstallationLog(pushToken);
    }

    @Override
    protected String getGroupName() {
        return PUSH_GROUP;
    }

    @Override
    protected String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        mContext = context;

        if (mSenderId != null) {
            AsyncTaskUtils.execute(LOG_TAG, new PushTokenTask());
        } else {
            MobileCenterLog.error(LOG_TAG, "Sender ID is null! Please set it by Push.setSenderId(senderId);");
        }
    }

    /**
     * Implements {@link #setSenderId(String)}.
     */
    private synchronized void setInstanceSenderId(String senderId) {
        mSenderId = senderId;
    }

    @VisibleForTesting
    private class PushTokenTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void[] params) {
            InstanceID instanceID = InstanceID.getInstance(mContext);
            try {
                return instanceID.getToken(mSenderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            } catch (IOException e) {
                MobileCenterLog.error(LOG_TAG, "Cannot get push token", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(String pushToken) {
            if (pushToken != null) {
                handlePushToken(pushToken);
            }
        }
    }
}
