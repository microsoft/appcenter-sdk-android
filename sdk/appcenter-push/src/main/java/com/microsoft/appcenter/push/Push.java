package com.microsoft.appcenter.push;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.push.ingestion.models.PushInstallationLog;
import com.microsoft.appcenter.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.util.HashMap;
import java.util.Map;

/**
 * Push notifications interface.
 */
public class Push extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Push";

    /**
     * TAG used in logging for Push.
     */
    private static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the push group.
     */
    private static final String PUSH_GROUP = "group_push";

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
     * Current context.
     */
    private Context mContext;

    /**
     * Sender ID. Used only when Firebase SDK not available to register for push.
     */
    private String mSenderId;

    /**
     * Indicates whether the push token must be registered in foreground.
     */
    private boolean mTokenNeedsRegistrationInForeground;

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
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Push service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
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
     * If you do not use the Google Services plugin, you must set the
     * Sender ID of your project before starting the Push service.
     *
     * @param senderId sender ID of your project.
     */
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
    @SuppressWarnings("WeakerAccess")
    public static void enableFirebaseAnalytics(@NonNull Context context) {
        AppCenterLog.debug(LOG_TAG, "Enabling firebase analytics collection.");
        setFirebaseAnalyticsEnabled(context, true);
=======
    public static void setSenderId(@SuppressWarnings("SameParameterValue") String senderId) {
        getInstance().instanceSetSenderId(senderId);
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
    }

    /**
     * Sets the sender ID. Must be called prior to starting the Push service.
     *
     * @param senderId sender ID of your project.
     */
    private synchronized void instanceSetSenderId(String senderId) {
        mSenderId = senderId;
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
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
    synchronized void onTokenRefresh(@NonNull final String pushToken) {
        AppCenterLog.debug(LOG_TAG, "Push token refreshed: " + pushToken);
        post(new Runnable() {

            @Override
            public void run() {
                enqueuePushInstallationLog(pushToken);
            }
        });
=======
    synchronized void onTokenRefresh(final String pushToken) {
        if (pushToken != null) {
            MobileCenterLog.debug(LOG_TAG, "Push token refreshed: " + pushToken);
            post(new Runnable() {

                @Override
                public void run() {
                    enqueuePushInstallationLog(pushToken);
                }
            });
        }
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
            try {
                String token = FirebaseInstanceId.getInstance().getToken();
                if (token != null) {
                    enqueuePushInstallationLog(token);
                }
            } catch (IllegalStateException e) {
                AppCenterLog.error(LOG_TAG, "Failed to get firebase push token.", e);
            }
=======
            registerPushToken();
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
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
        mContext = context;
        super.onStarted(context, appSecret, channel);
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
        if (!sFirebaseAnalyticsEnabled) {
            AppCenterLog.debug(LOG_TAG, "Disabling firebase analytics collection by default.");
            setFirebaseAnalyticsEnabled(context, false);
        }
=======
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
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
    public synchronized void onActivityResumed(Activity activity) {
        checkPushInActivityIntent(activity);
        if (mTokenNeedsRegistrationInForeground) {
            mTokenNeedsRegistrationInForeground = false;
            registerPushToken();
        }
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
            AppCenterLog.error(LOG_TAG, "Push.checkLaunchedFromNotification: activity may not be null");
            return;
        }
        if (intent == null) {
            AppCenterLog.error(LOG_TAG, "Push.checkLaunchedFromNotification: intent may not be null");
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
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
            Bundle extras = intent.getExtras();
            if (extras != null) {
                String googleMessageId = extras.getString(EXTRA_GOOGLE_MESSAGE_ID);
                if (googleMessageId != null && !googleMessageId.equals(mLastGoogleMessageId)) {
                    AppCenterLog.info(LOG_TAG, "Clicked push message from background id=" + googleMessageId);
                    mLastGoogleMessageId = googleMessageId;
                    Map<String, String> customData = new HashMap<>();
                    Map<String, Object> allData = new HashMap<>();
                    for (String extra : extras.keySet()) {
                        allData.put(extra, extras.get(extra));
                        if (!EXTRA_STANDARD_KEYS.contains(extra)) {
                            customData.put(extra, extras.getString(extra));
                        }
                    }
                    AppCenterLog.debug(LOG_TAG, "Push intent extra=" + allData);
                    mInstanceListener.onPushNotificationReceived(mActivity, new PushNotification(null, null, customData));
                }
=======
            String googleMessageId = PushIntentUtils.getGoogleMessageId(intent);
            if (googleMessageId != null && !googleMessageId.equals(mLastGoogleMessageId)) {
                PushNotification notification = new PushNotification(intent);
                MobileCenterLog.info(LOG_TAG, "Clicked push message from background id=" + googleMessageId);
                mLastGoogleMessageId = googleMessageId;
                MobileCenterLog.debug(LOG_TAG, "Push intent extras=" + intent.getExtras());
                mInstanceListener.onPushNotificationReceived(mActivity, notification);
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
            }
        }
    }

    /**
     * Called when push message received in foreground.
     *
     * @param pushIntent intent from push.
     */
<<<<<<< HEAD:sdk/appcenter-push/src/main/java/com/microsoft/appcenter/push/Push.java
    void onMessageReceived(final RemoteMessage remoteMessage) {
        AppCenterLog.info(LOG_TAG, "Received push message in foreground id=" + remoteMessage.getMessageId());
=======
    synchronized void onMessageReceived(Context context, final Intent pushIntent) {
        if (mActivity == null) {
            if (!FirebaseUtils.isFirebaseAvailable()) {
                PushNotifier.handleNotification(context, pushIntent);
            }
            return;
        }
        String messageId = PushIntentUtils.getGoogleMessageId(pushIntent);
        final PushNotification notification = new PushNotification(pushIntent);
        MobileCenterLog.info(LOG_TAG, "Received push message in foreground id=" + messageId);
>>>>>>> feature/remove-firebase-dependency:sdk/mobile-center-push/src/main/java/com/microsoft/azure/mobile/push/Push.java
        postOnUiThread(new Runnable() {
            @Override
            public void run() {
                handleOnMessageReceived(notification);
            }
        });
    }

    /**
     * Register application for push.
     */
    private synchronized void registerPushToken() {
        try {
            onTokenRefresh(FirebaseUtils.getToken());
            MobileCenterLog.info(LOG_TAG, "Firebase SDK is available, using Firebase SDK registration.");
        } catch (FirebaseUtils.FirebaseUnavailableException e) {
            MobileCenterLog.info(LOG_TAG, "Firebase SDK is not available, using built in registration. cause: " + e.getMessage());
            registerPushTokenWithoutFirebase();
        }
    }

    /**
     * Register application for push without Firebase.
     */
    private synchronized void registerPushTokenWithoutFirebase() {
        if (mSenderId == null) {
            int resId = mContext.getResources().getIdentifier("gcm_defaultSenderId", "string", mContext.getPackageName());
            try {
                mSenderId = mContext.getString(resId);
            } catch (Resources.NotFoundException e) {
                MobileCenterLog.error(LOG_TAG, "Push.setSenderId was not called, aborting registration.");
                return;
            }
        }
        Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
        registrationIntent.setPackage("com.google.android.gsf");
        registrationIntent.putExtra("sender", mSenderId);
        registrationIntent.putExtra("app", PendingIntent.getBroadcast(mContext, 0, new Intent(), 0));
        try {
            mContext.startService(registrationIntent);
        } catch (IllegalStateException e) {
            MobileCenterLog.info(LOG_TAG, "Cannot register in background, will wait to be in foreground");
            mTokenNeedsRegistrationInForeground = true;
        } catch (RuntimeException e) {
            MobileCenterLog.error(LOG_TAG, "Failed to register push token", e);
        }
    }

    /**
     * Top level method needed for synchronized code coverage.
     */
    @UiThread
    private synchronized void handleOnMessageReceived(PushNotification pushNotification) {
        if (mInstanceListener != null) {
            mInstanceListener.onPushNotificationReceived(mActivity, pushNotification);
        }
    }
}
