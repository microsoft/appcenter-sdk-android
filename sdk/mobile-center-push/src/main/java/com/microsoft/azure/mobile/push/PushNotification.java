package com.microsoft.azure.mobile.push;

import android.app.Activity;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.Map;

/**
 * Object describing a received push notification.
 */
@SuppressWarnings("WeakerAccess")
public class PushNotification {

    /**
     * Notification title.
     */
    private String mTitle;

    /**
     * Notification message.
     */
    private String mMessage;

    /**
     * Custom data.
     */
    private Map<String, String> mCustomData;

    /**
     * Activity context of the push notification.
     */
    private WeakReference<Activity> mActivity;

    /**
     * Init.
     */
    public PushNotification(String title, String message, @NonNull Map<String, String> customData, @NonNull WeakReference<Activity> activity) {
        mTitle = title;
        mMessage = message;
        mCustomData = customData;
        mActivity = activity;
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

    /**
     * Get the activity that was current when the push was received in foreground or clicked from
     * notification.
     *
     * @return current activity reference. Reference wrapper is never null but activity can be null.
     */
    public WeakReference<Activity> getActivity() {
        return mActivity;
    }
}
