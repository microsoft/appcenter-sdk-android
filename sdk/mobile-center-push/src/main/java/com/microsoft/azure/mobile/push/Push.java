package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.RemoteMessage;
import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Push notifications interface.
 */
public class Push extends AbstractMobileCenterService {

    /**
     * Google message identifier extra intent key.
     */
    @VisibleForTesting
    static final String EXTRA_GOOGLE_MESSAGE_ID = "google.message_id";

    /**
     * Intent extras not part of custom data.
     */
    @VisibleForTesting
    static final Set<String> EXTRA_STANDARD_KEYS = new HashSet<String>() {
        {
            add(EXTRA_GOOGLE_MESSAGE_ID);
            add("google.sent_time");
            add("collapse_key");
            add("from");
        }
    };

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Push";

    /**
     * TAG used in logging for Analytics.
     */
    private static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the push group.
     */
    private static final String PUSH_GROUP = "group_push";

    /**
     * Firebase analytics flag.
     */
    private static boolean sFirebaseAnalyticsEnabled;

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Push sInstance;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Push listener.
     */
    private PushListener mInstanceListener;

    /**
     * Check if push already inspected from intent.
     * Not reset on disabled to avoid repeat push callback when enabled again...
     */
    private String mLastGoogleMessageId;

    /**
     * Current activity.
     */
    private Activity mActivity;

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
        sFirebaseAnalyticsEnabled = false;
    }

    /**
     * Check whether Push service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see MobileCenterFuture
     */
    public static MobileCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Push service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static MobileCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Set push listener.
     *
     * @param pushListener push listener.
     */
    public static void setListener(PushListener pushListener) {
        getInstance().setInstanceListener(pushListener);
    }

    /**
     * If you are using the listener for background push notifications
     * and your activity has a launch mode such as singleTop, singleInstance or singleTask,
     * you need to call this method in your launcher {@link Activity#onNewIntent(Intent)} method.
     *
     * @param activity activity calling {@link Activity#onNewIntent(Intent)} (pass this).
     * @param intent   intent from {@link Activity#onNewIntent(Intent)}.
     */
    public static void checkLaunchedFromNotification(Activity activity, Intent intent) {
        getInstance().checkPushInActivityIntent(activity, intent);
    }

    /**
     * Enable firebase analytics collection.
     *
     * @param context the context to retrieve FirebaseAnalytics instance.
     */
    @SuppressWarnings("WeakerAccess")
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
        FirebaseAnalyticsUtils.setEnabled(context, enabled);
        sFirebaseAnalyticsEnabled = enabled;
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
     * @param pushToken the push token value.
     */
    synchronized void onTokenRefresh(@NonNull final String pushToken) {
        MobileCenterLog.debug(LOG_TAG, "Push token refreshed: " + pushToken);
        post(new Runnable() {

            @Override
            public void run() {
                enqueuePushInstallationLog(pushToken);
            }
        });
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            String token = FirebaseInstanceId.getInstance().getToken();
            if (token != null) {
                enqueuePushInstallationLog(token);
            }
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
        if (!sFirebaseAnalyticsEnabled) {
            MobileCenterLog.debug(LOG_TAG, "Disabling firebase analytics collection by default.");
            setFirebaseAnalyticsEnabled(context, false);
        }
    }

    /**
     * Implements {@link #setListener} at instance level.
     */
    private synchronized void setInstanceListener(PushListener instanceListener) {
        mInstanceListener = instanceListener;
    }

    /*
     * We can miss onCreate onStarted depending on how developers init the SDK.
     * So look for multiple events.
     */

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        checkPushInActivityIntent(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        checkPushInActivityIntent(activity);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        checkPushInActivityIntent(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        postOnUiThread(new Runnable() {

            @Override
            public void run() {
                mActivity = null;
            }
        });
    }

    private void checkPushInActivityIntent(Activity activity) {
        checkPushInActivityIntent(activity, activity.getIntent());
    }

    private void checkPushInActivityIntent(final Activity activity, final Intent intent) {
        if (activity == null) {
            MobileCenterLog.error(LOG_TAG, "Push.checkLaunchedFromNotification: activity may not be null");
            return;
        }
        if (intent == null) {
            MobileCenterLog.error(LOG_TAG, "Push.checkLaunchedFromNotification: intent may not be null");
            return;
        }
        postOnUiThread(new Runnable() {

            @Override
            public void run() {
                mActivity = activity;
                checkPushInIntent(intent);
            }
        });
    }

    /**
     * Check for push message clicked from notification center in activity intent.
     *
     * @param intent intent to inspect.
     */
    private synchronized void checkPushInIntent(Intent intent) {
        if (mInstanceListener != null) {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String googleMessageId = extras.getString(EXTRA_GOOGLE_MESSAGE_ID);
                if (googleMessageId != null && !googleMessageId.equals(mLastGoogleMessageId)) {
                    MobileCenterLog.info(LOG_TAG, "Clicked push message from background id=" + googleMessageId);
                    mLastGoogleMessageId = googleMessageId;
                    Map<String, String> customData = new HashMap<>();
                    Map<String, Object> allData = new HashMap<>();
                    for (String extra : extras.keySet()) {
                        allData.put(extra, extras.get(extra));
                        if (!EXTRA_STANDARD_KEYS.contains(extra)) {
                            customData.put(extra, extras.getString(extra));
                        }
                    }
                    MobileCenterLog.debug(LOG_TAG, "Push intent extra=" + allData);
                    mInstanceListener.onPushNotificationReceived(mActivity, new PushNotification(null, null, customData));
                }
            }
        }
    }

    /**
     * Called when push message received in foreground.
     *
     * @param remoteMessage push message details.
     */
    void onMessageReceived(final RemoteMessage remoteMessage) {
        MobileCenterLog.info(LOG_TAG, "Received push message in foreground id=" + remoteMessage.getMessageId());
        postOnUiThread(new Runnable() {

            @Override
            public void run() {
                handleOnMessageReceived(remoteMessage);
            }
        });
    }

    /**
     * Top level method needed for synchronized code coverage.
     */
    @UiThread
    private synchronized void handleOnMessageReceived(RemoteMessage remoteMessage) {
        if (mInstanceListener != null) {
            String title = null;
            String message = null;
            RemoteMessage.Notification notification = remoteMessage.getNotification();
            if (notification != null) {
                title = notification.getTitle();
                message = notification.getBody();
            }
            PushNotification pushNotification = new PushNotification(title, message, remoteMessage.getData());
            mInstanceListener.onPushNotificationReceived(mActivity, pushNotification);
        }
    }
}
