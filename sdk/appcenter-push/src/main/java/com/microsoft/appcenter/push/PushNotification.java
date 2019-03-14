/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.Intent;
import android.support.annotation.NonNull;

import java.util.Map;

/**
 * Object describing a received push notification.
 */
@SuppressWarnings("WeakerAccess")
public class PushNotification {

    /**
     * Notification title.
     */
    private final String mTitle;

    /**
     * Notification message.
     */
    private final String mMessage;

    /**
     * Custom data.
     */
    private final Map<String, String> mCustomData;

    /**
     * Init.
     *
     * @param title      notification title.
     * @param message    notification message.
     * @param customData custom data.
     */
    public PushNotification(@SuppressWarnings("SameParameterValue") String title, @SuppressWarnings("SameParameterValue") String message, @NonNull Map<String, String> customData) {
        mTitle = title;
        mMessage = message;
        mCustomData = customData;
    }

    /**
     * Init from an intent.
     *
     * @param pushIntent the intent that triggered the Push.
     */
    public PushNotification(Intent pushIntent) {
        mTitle = PushIntentUtils.getTitle(pushIntent);
        mMessage = PushIntentUtils.getMessage(pushIntent);
        mCustomData = PushIntentUtils.getCustomData(pushIntent);
    }

    /**
     * Get notification title.
     *
     * @return notification title or null if was not specified or if push received in background.
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Get notification message.
     *
     * @return notification message or null if push received in background.
     */
    public String getMessage() {
        return mMessage;
    }

    /**
     * Get custom data.
     *
     * @return custom data with the push. Can be empty but not null.
     */
    public Map<String, String> getCustomData() {
        return mCustomData;
    }
}
