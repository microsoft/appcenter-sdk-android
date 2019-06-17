/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;

import java.util.Map;
import java.util.UUID;

import static android.content.Context.NOTIFICATION_SERVICE;
import static com.microsoft.appcenter.push.Push.LOG_TAG;

class PushNotifier {

    /**
     * Default channel identifier on Android 8.
     */
    private static final String CHANNEL_ID = "app_center_push";

    /**
     * Default channel name on Android 8.
     */
    private static final String CHANNEL_NAME = "Push";

    /**
     * Default notification icon meta-data name.
     */
    @VisibleForTesting
    static final String DEFAULT_ICON_METADATA_NAME = "com.google.firebase.messaging.default_notification_icon";

    /**
     * Default notification color meta-data name.
     */
    @VisibleForTesting
    static final String DEFAULT_COLOR_METADATA_NAME = "com.google.firebase.messaging.default_notification_color";

    /**
     * Builds a push notification using the given context and intent.
     *
     * @param context    The current context.
     * @param pushIntent The intent that is associated with the push.
     */
    static void handleNotification(Context context, Intent pushIntent) throws RuntimeException {
        context = context.getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        /* Generate notification identifier using the hash of the message id. */
        String messageId = PushIntentUtils.getMessageId(pushIntent);
        if (messageId == null) {
            AppCenterLog.warn(LOG_TAG, "Push notification did not contain identifier, generate one.");
            messageId = UUID.randomUUID().toString();
        }
        int notificationId = messageId.hashCode();

        /* Click action. */
        PackageManager packageManager = context.getPackageManager();
        Intent actionIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        if (actionIntent != null) {
            actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Map<String, String> customData = PushIntentUtils.getCustomData(pushIntent);
            for (String key : customData.keySet()) {
                actionIntent.putExtra(key, customData.get(key));
            }

            /* Set the message ID in the intent. */
            PushIntentUtils.setMessageId(messageId, actionIntent);
        } else {

            /* If no launcher, just create a placeholder action as the field is mandatory. */
            actionIntent = new Intent();
        }

        /* Get text. Use app name for title if title is missing. */
        String notificationTitle = PushIntentUtils.getTitle(pushIntent);
        if (notificationTitle == null || notificationTitle.isEmpty()) {
            notificationTitle = AppNameHelper.getAppName(context);
        }
        String notificationMessage = PushIntentUtils.getMessage(pushIntent);

        /* Start building notification. */
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O) {

            /* Get channel. */
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            /* Create or update channel. */
            //noinspection ConstantConditions
            notificationManager.createNotificationChannel(channel);

            /* And associate to notification. */
            builder = new Notification.Builder(context, channel.getId());
        } else {
            builder = getOldNotificationBuilder(context);
        }

        /* Set color. */
        setColor(context, pushIntent, builder);

        /* Set icon. */
        setIcon(context, pushIntent, builder);

        /* Set sound. */
        setSound(context, pushIntent, builder);

        /* Set texts. */
        builder.setContentTitle(notificationTitle)
                .setContentText(notificationMessage)
                .setWhen(System.currentTimeMillis())
                .setStyle(new Notification.BigTextStyle().bigText(notificationMessage));

        /* Click action. Reuse notification id for simplicity. */
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId,
                actionIntent, 0);
        builder.setContentIntent(contentIntent);

        /* Set flags. */
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        //noinspection ConstantConditions
        notificationManager.notify(notificationId, notification);
    }

    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Notification.Builder getOldNotificationBuilder(Context context) {
        return new Notification.Builder(context);
    }

    /**
     * Sets the color in the notification builder if the property is set in the intent.
     *
     * @param context    The current context.
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    private static void setColor(Context context, Intent pushIntent, Notification.Builder builder) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        /* Check custom color from intent. */
        String colorString = PushIntentUtils.getColor(pushIntent);
        if (colorString != null) {
            try {
                builder.setColor(Color.parseColor(colorString));
                return;
            } catch (IllegalArgumentException e) {
                AppCenterLog.error(LOG_TAG, "Invalid color string received in push payload.", e);
            }
        }

        /* Check default color. */
        int colorResourceId = getResourceIdFromMetadata(context, DEFAULT_COLOR_METADATA_NAME);
        if (colorResourceId != 0) {
            AppCenterLog.debug(LOG_TAG, "Using color specified in meta-data for notification.");
            builder.setColor(getColor(context, colorResourceId));
        }
    }


    /**
     * Sets the sound in the notification builder if the property is set in the intent.
     * This is effective only for devices running or targeting an Android version lower than 8.
     *
     * @param context    The current context.
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static void setSound(Context context, Intent pushIntent, Notification.Builder builder) {
        String sound = PushIntentUtils.getSound(pushIntent);
        if (sound != null) {
            try {
                Resources resources = context.getResources();
                int id = resources.getIdentifier(sound, "raw", context.getPackageName());
                Uri soundUri = new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(resources.getResourcePackageName(id))
                        .appendPath(resources.getResourceTypeName(id))
                        .appendPath(resources.getResourceEntryName(id))
                        .build();
                builder.setSound(soundUri);
            } catch (Resources.NotFoundException e) {
                AppCenterLog.warn(LOG_TAG, "Sound file '" + sound + "' not found; falling back to default.");
                builder.setDefaults(Notification.DEFAULT_SOUND);
            }
        }
    }


    /**
     * Sets the icon for the notification builder if the property is set in the intent, if no custom
     * icon is provided as an extra, the app icon is used.
     *
     * @param context    The current context.
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    private static void setIcon(Context context, Intent pushIntent, Notification.Builder builder) {

        /* Check custom icon from intent. */
        String iconString = PushIntentUtils.getIcon(pushIntent);

        /* Try to get resource identifier. */
        int iconResourceId = 0;
        if (!TextUtils.isEmpty(iconString)) {
            Resources resources = context.getResources();
            String packageName = context.getPackageName();
            iconResourceId = resources.getIdentifier(iconString, "drawable", packageName);
            if (iconResourceId != 0) {
                AppCenterLog.debug(LOG_TAG, "Found icon resource in 'drawable'.");
            } else {
                iconResourceId = resources.getIdentifier(iconString, "mipmap", packageName);
                if (iconResourceId != 0) {
                    AppCenterLog.debug(LOG_TAG, "Found icon resource in 'mipmap'.");
                }
            }
        }
        if (iconResourceId != 0) {
            iconResourceId = validateIcon(context, iconResourceId);
        }

        /* Check default icon. */
        if (iconResourceId == 0) {
            iconResourceId = getResourceIdFromMetadata(context, DEFAULT_ICON_METADATA_NAME);
            if (iconResourceId != 0) {
                AppCenterLog.debug(LOG_TAG, "Using icon specified in meta-data for notification.");
                iconResourceId = validateIcon(context, iconResourceId);
            }
        }

        /* If no icon specified use application icon. */
        if (iconResourceId == 0) {
            AppCenterLog.debug(LOG_TAG, "Using application icon as notification icon.");
            iconResourceId = validateIcon(context, context.getApplicationInfo().icon);
        }

        /* Fall back to a 1 pixel icon if icon invalid. */
        if (iconResourceId == 0) {
            AppCenterLog.warn(LOG_TAG, "Using 1 pixel icon as fallback for notification.");
            iconResourceId = R.drawable.ic_stat_notify_dot;
        }
        builder.setSmallIcon(iconResourceId);
    }

    private static int validateIcon(Context context, int iconResourceId) {
        if (iconResourceId == 0) {
            return iconResourceId;
        }
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && context.getDrawable(iconResourceId) instanceof AdaptiveIconDrawable) {
            AppCenterLog.error(LOG_TAG, "Adaptive icons make Notification center crash (system process) on Android 8.0 (was fixed on Android 8.1), " +
                    "please update your icon to be non adaptive or please use another icon to push.");
            iconResourceId = 0;
        }
        return iconResourceId;
    }

    private static int getResourceIdFromMetadata(Context context, String metadataName) {
        Bundle metaData = null;
        try {
            metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData;
        } catch (PackageManager.NameNotFoundException e) {
            AppCenterLog.error(LOG_TAG, "Package name not found.", e);
        }
        if (metaData != null) {
            return metaData.getInt(metadataName);
        }
        return 0;
    }

    @SuppressWarnings("deprecation")
    private static int getColor(Context context, int colorResourceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorResourceId);
        } else {
            return context.getResources().getColor(colorResourceId);
        }
    }
}


