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
import android.util.Log;

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
    static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

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
     * Firebase analytics flag.
     */
    private boolean mFirebaseAnalyticsEnabled;

    /**
     * Check if push already inspected from intent.
     * Not reset on disabled to avoid repeat push callback when enabled again...
     */
    private String mLastGoogleMessageId;

    /**
     * First google message id obtained. Need to save because when app is launched from
     * push, the activity will always contain the original intent thereafter, so it needs to
     * be remembered to avoid being replayed.
     */
    private String mFirstGoogleMessageId;

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
    @SuppressWarnings("WeakerAccess")
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Push service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings("WeakerAccess")
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Set push listener.
     *
     * @param pushListener push listener.
     */
    @SuppressWarnings("WeakerAccess")
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
    @SuppressWarnings("WeakerAccess")
    public static void checkLaunchedFromNotification(Activity activity, Intent intent) {
        getInstance().checkPushInActivityIntent(activity, intent);
    }

    /**
     * If you do not use the Google Services plugin, you must set the
     * Sender ID of your project before starting the Push service.
     *
     * @param senderId sender ID of your project.
     */
    @SuppressWarnings("WeakerAccess")
    public static void setSenderId(@SuppressWarnings("SameParameterValue") String senderId) {
        getInstance().instanceSetSenderId(senderId);
    }

    /**
     * Enable firebase analytics collection.
     *
     * @param context the context to retrieve FirebaseAnalytics instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static void enableFirebaseAnalytics(@NonNull Context context) {
        AppCenterLog.debug(LOG_TAG, "Enabling Firebase analytics collection.");
        getInstance().setFirebaseAnalyticsEnabled(context, true);
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
     * Enable or disable firebase analytics collection.
     *
     * @param context the context to retrieve FirebaseAnalytics instance.
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    @SuppressWarnings("MissingPermission")
    private synchronized void setFirebaseAnalyticsEnabled(@NonNull Context context, boolean enabled) {
        try {
            FirebaseUtils.setAnalyticsEnabled(context, enabled);
        } catch (FirebaseUtils.FirebaseUnavailableException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to enable or disable Firebase analytics collection.");
        }
        mFirebaseAnalyticsEnabled = enabled;
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
    synchronized void onTokenRefresh(final String pushToken) {
        if (pushToken != null) {
            AppCenterLog.debug(LOG_TAG, "Push token refreshed: " + pushToken);
            post(new Runnable() {

                @Override
                public void run() {
                    enqueuePushInstallationLog(pushToken);
                }
            });
        }
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (enabled) {
            registerPushToken();
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
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
        if (FirebaseUtils.isFirebaseAvailable() && !mFirebaseAnalyticsEnabled) {
            AppCenterLog.debug(LOG_TAG, "Disabling Firebase analytics collection by default.");
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
            String googleMessageId = PushIntentUtils.getMessageId(intent);
            if (googleMessageId != null && !googleMessageId.equals(mLastGoogleMessageId)
                    && !googleMessageId.equals(mFirstGoogleMessageId)) {
                if (mFirstGoogleMessageId == null) {
                    mFirstGoogleMessageId = googleMessageId;
                }
                PushNotification notification = new PushNotification(intent);
                AppCenterLog.info(LOG_TAG, "Clicked push message from background id=" + googleMessageId);
                mLastGoogleMessageId = googleMessageId;
                AppCenterLog.debug(LOG_TAG, "Push intent extras=" + intent.getExtras());
                mInstanceListener.onPushNotificationReceived(mActivity, notification);
            }
        }
    }

    /**
     * Called when push message received in foreground.
     *
     * @param pushIntent intent from push.
     */
    synchronized void onMessageReceived(Context context, final Intent pushIntent) {

        /* Log message. */
        boolean isBackground = mActivity == null;
        if (AppCenterLog.getLogLevel() <= Log.DEBUG) {
            StringBuilder message = new StringBuilder("Received push intent=");
            message.append(pushIntent);
            message.append(" background=").append(isBackground);
            Bundle intentExtras = pushIntent.getExtras();
            if (intentExtras != null) {
                message.append('\n');
                for (String key : intentExtras.keySet()) {
                    message.append(key).append("=").append(intentExtras.get(key)).append('\n');
                }
            }
            AppCenterLog.debug(LOG_TAG, message.toString());
        }

        /* Check if background. */
        if (isBackground) {

            /* If background and Firebase SDK is active, it will already generate a notification. */
            if (FirebaseUtils.isFirebaseAvailable()) {
                AppCenterLog.debug(LOG_TAG, "Background notifications are handled by Firebase SDK when integrated.");
            } else {
                PushNotifier.handleNotification(context, pushIntent);
            }
        } else {
            final PushNotification notification = new PushNotification(pushIntent);
            postOnUiThread(new Runnable() {

                @Override
                public void run() {
                    handleOnMessageReceived(notification);
                }
            });
        }
    }

    /**
     * Register application for push.
     */
    private synchronized void registerPushToken() {
        try {
            onTokenRefresh(FirebaseUtils.getToken());
            AppCenterLog.info(LOG_TAG, "Firebase SDK is available, using Firebase SDK registration.");
        } catch (FirebaseUtils.FirebaseUnavailableException e) {
            AppCenterLog.info(LOG_TAG, "Firebase SDK is not available, using built in registration. cause: " + e.getMessage());
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
                AppCenterLog.error(LOG_TAG, "Push.setSenderId was not called, aborting registration.");
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
            AppCenterLog.info(LOG_TAG, "Cannot register in background, will wait to be in foreground");
            mTokenNeedsRegistrationInForeground = true;
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to register push token", e);
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
