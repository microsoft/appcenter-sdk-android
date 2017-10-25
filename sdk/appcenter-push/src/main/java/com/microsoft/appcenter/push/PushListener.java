package com.microsoft.appcenter.push;

import android.app.Activity;
import android.support.annotation.UiThread;

/**
 * Listener for push messages.
 */
@SuppressWarnings("WeakerAccess")
public interface PushListener {

    /**
     * Called whenever a push notification is either clicked from system notification center or
     * when the push is received in foreground.
     *
     * @param activity         current activity when push is received or clicked.
     * @param pushNotification the push notification details. If clicked from background, title and
     *                         message will be empty.
     *                         <p>
     *                         If the push is received in foreground,
     *                         no notification has been generated in system notification center.
     */
    @UiThread
    void onPushNotificationReceived(Activity activity, PushNotification pushNotification);
}
