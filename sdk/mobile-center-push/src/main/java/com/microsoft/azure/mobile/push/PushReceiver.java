package com.microsoft.azure.mobile.push;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PushReceiver extends BroadcastReceiver {
    /**
     * Action when we receive token.
     */
    private static final String INTENT_ACTION_REGISTRATION = "com.google.android.c2dm.intent.REGISTRATION";

    /**
     * Token key in intent result.
     */
    private static final String INTENT_EXTRA_REGISTRATION = "registration_id";

    /**
     *  Action when we receive a push.
     */
    public static final String INTENT_ACTION_RECEIVE = "com.google.android.c2dm.intent.RECEIVE";

    @Override
    public void onReceive(Context context, Intent intent) {

        /* Registration result action. */
        String action = intent.getAction();
        if (INTENT_ACTION_REGISTRATION.equals(action)) {
            String registrationId = intent.getStringExtra(INTENT_EXTRA_REGISTRATION);
            Push.getInstance().onTokenRefresh(registrationId);
        }

        //TODO if context is null then cache and replay at onstart push
        /* Received message action. */
        else if (INTENT_ACTION_RECEIVE.equals(action)) {
            if (Push.getInstance().isInBackground() && !FirebaseUtils.isFirebaseAvailable()) {
                PushNotifier.handleNotification(context, intent);
            }
            else if (!Push.getInstance().isInBackground()) {
                Push.getInstance().onMessageReceived(intent);
            }
        }
    }
}
