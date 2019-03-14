/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.app.Activity;

/**
 * Listener for push messages.
 */
@SuppressWarnings("WeakerAccess")
public interface PushListener {

    /**
     * Called from UI thread whenever a push notification is either clicked from system notification center or
     * when the push is received in foreground.
     *
     * @param activity         current activity when push is received or clicked.
     * @param pushNotification the push notification details. If clicked from background, title and
     *                         message will be empty.
     *                         <p>
     *                         If the push is received in foreground,
     *                         no notification has been generated in system notification center.
     */
    void onPushNotificationReceived(Activity activity, PushNotification pushNotification);
}
