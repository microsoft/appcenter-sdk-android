package com.microsoft.azure.mobile.push;

/**
 * Listener for push messages.
 */
@SuppressWarnings("WeakerAccess")
public interface PushListener {

    /**
     * Called whenever a push notification is either clicked from system notification center or
     * when the push is received in foreground.
     *
     * @param pushNotification the push notification details. If clicked from background, title and
     *                         message will be empty.
     *                         <p>
     *                         If the push is received in foreground,
     *                         no notification has been generated in system notification center
     *                         and you can access title and message from this parameter.
     */
    void onPushNotificationReceived(PushNotification pushNotification);
}
