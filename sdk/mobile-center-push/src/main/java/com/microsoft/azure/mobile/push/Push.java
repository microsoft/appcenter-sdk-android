package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;

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
     * TAG used in logging for Push.
     */
    private static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

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
     * Sender ID.
     */
    private String mSenderId;

    /**
     * Indicates whether the push token must be registered in OnStart.
     */
    private boolean mTokenNeedsRegistration;

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
     * If you do not use the Google Services plugin, you must set the
     * Sender ID of your project before starting the Push service.
     *
     * @param senderId sender ID of your project.
     */
    public static void setSenderId(String senderId) {
        getInstance().instanceSetSenderId(senderId);
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
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        if (!enabled) {

            /* Nothing to do when diabling. */
            return;
        }
        registerPushToken();
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
        mContext = context;
        if (mTokenNeedsRegistration) {
            registerPushToken();
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
                String googleMessageId = PushIntentUtils.getGoogleMessageId(intent);
                if (googleMessageId != null && !googleMessageId.equals(mLastGoogleMessageId)) {
                    PushNotification notification = new PushNotification(intent);
                    MobileCenterLog.info(LOG_TAG, "Clicked push message from background id=" + googleMessageId);
                    mLastGoogleMessageId = googleMessageId;
                    MobileCenterLog.debug(LOG_TAG, "Push intent extras=" + extras);
                    mInstanceListener.onPushNotificationReceived(mActivity, notification);
                }
            }
        }
    }

    /**
     * Called when push message received in foreground.
     *
     * @param pushIntent intent from push.
     */
    void onMessageReceived(Context context, final Intent pushIntent) {
        if (!FirebaseUtils.isFirebaseAvailable()) {
            if (mActivity != null) {
                String messageId = PushIntentUtils.getGoogleMessageId(pushIntent);
                final PushNotification notification = new PushNotification(pushIntent);
                MobileCenterLog.info(LOG_TAG, "Received push message in foreground id=" + messageId);
                postOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        handleOnMessageReceived(notification);
                    }
                });
                return;
            }
            PushNotifier.handleNotification(context, pushIntent);
        }
    }

    private synchronized void registerPushToken() {

        /*
         * Check if Firebase is available first to avoid logging the "falling back" warning if
         * the app was never supposed to use Firebase in the first place.
         */
        if (FirebaseUtils.isFirebaseAvailable()) {
            try {
                String token = FirebaseUtils.getToken();
                onTokenRefresh(token);
                return;
            }
            catch (Exception e) {
                MobileCenterLog.warn(LOG_TAG,
                        "Failed to get push token with Firebase; falling back to custom logic.");
            }
        }

        if (mContext != null) {
            Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
            registrationIntent.putExtra("sender", mSenderId);
            registrationIntent.setPackage("com.google.android.gsf");
            registrationIntent.putExtra("app", PendingIntent.getBroadcast(mContext, 0, new Intent(), 0));
            try {
                mContext.startService(registrationIntent);
            } catch (RuntimeException e) {

                /* Abort if the GCM service can't be accessed. */
                MobileCenterLog.warn(LOG_TAG, "Failed to register push token", e);
            }
        }
        else {
            mTokenNeedsRegistration = true;
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
