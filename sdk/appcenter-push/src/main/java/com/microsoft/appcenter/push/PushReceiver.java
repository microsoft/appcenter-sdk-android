package com.microsoft.appcenter.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.push.Push.LOG_TAG;

public class PushReceiver extends BroadcastReceiver {

    /**
     * Action when we receive token.
     */
    @VisibleForTesting
    static final String INTENT_ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";

    /**
     * Token key in intent result.
     */
    @VisibleForTesting
    static final String INTENT_EXTRA_REGISTRATION = "registration_id";

    /**
     * Action when we receive a push.
     */
    @VisibleForTesting
    static final String INTENT_ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    @Override
    public void onReceive(Context context, Intent intent) {

        /* Registration result action. */
        String action = intent.getAction();
        if (INTENT_ACTION_REGISTRATION.equals(action)) {
            String registrationId = intent.getStringExtra(INTENT_EXTRA_REGISTRATION);
            Push.getInstance().onTokenRefresh(registrationId);
        }

        /* Received message action. */
        else if (INTENT_ACTION_RECEIVE.equals(action)) {
            Push.getInstance().onMessageReceived(context, intent);

            /* Prevent handling message by not initialized firebase. */
            if (!FirebaseUtils.isFirebaseAvailable()) {
                AppCenterLog.warn(LOG_TAG, "Abort \"" + INTENT_ACTION_RECEIVE + "\" broadcast " +
                        "because firebase is not available.");
                abortBroadcast();
            }
        }
    }
}
