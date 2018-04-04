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
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;

import java.util.LinkedHashMap;
import java.util.Map;

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
     * Cache of received pushes for managing duplicates.
     */
    private static final LinkedHashMap<String, String> sReceivedPushes = new LinkedHashMap<String, String>(0, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Entry<String, String> eldest) {
            return size() > 10;
        }
    };

    @VisibleForTesting
    static void clearCache() {
        sReceivedPushes.clear();
    }

    /**
     * Builds a push notification using the given context and intent.
     *
     * @param context    The current context.
     * @param pushIntent The intent that is associated with the push.
     */
    static void handleNotification(Context context, Intent pushIntent) throws RuntimeException {
        context = context.getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        /* Generate notification identifier using the hash of the Google message id. */
        String messageId = PushIntentUtils.getGoogleMessageId(pushIntent);
        if (messageId == null) {
            AppCenterLog.warn(LOG_TAG, "Push notification did not contain identifier.");
        } else if (sReceivedPushes.put(messageId, messageId) != null) {
            AppCenterLog.warn(LOG_TAG, "Ignore duplicate notification id=" + messageId);
            return;
        }
        int notificationId = messageId == null ? pushIntent.hashCode() : messageId.hashCode();

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
            PushIntentUtils.setGoogleMessageId(messageId, actionIntent);

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
        setColor(pushIntent, builder);

        /* Set icon. */
        setIcon(context, pushIntent, builder);

        /* Set sound. */
        setSound(context, pushIntent, builder);

        /* Texts */
        builder.setContentTitle(notificationTitle).
                setContentText(notificationMessage).
                setWhen(System.currentTimeMillis());

        /* Click action. Reuse notification id for simplicity. */
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId,
                actionIntent, 0);
        builder.setContentIntent(contentIntent);

        /* Build method depends on versions. */
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        } else {
            notification = getOldNotification(builder);
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        //noinspection ConstantConditions
        notificationManager.notify(notificationId, notification);
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private static Notification.Builder getOldNotificationBuilder(Context context) {
        return new Notification.Builder(context);
    }

    @NonNull
    @SuppressWarnings("deprecation")
    private static Notification getOldNotification(Notification.Builder builder) {
        return builder.getNotification();
    }

    /**
     * Sets the color in the notification builder if the property is set in the intent.
     *
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    private static void setColor(Intent pushIntent, Notification.Builder builder) {
        String colorString = PushIntentUtils.getColor(pushIntent);
        if (colorString != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.setColor(Color.parseColor(colorString));
            }
        }
    }


    /**
     * Sets the sound in the notification builder if the property is set in the intent.
     * This is effective only for devices running or targeting an Android version lower than 8.
     *
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    @SuppressWarnings("deprecation")
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
     * @param pushIntent The push intent.
     * @param builder    The builder to modify.
     */
    private static void setIcon(Context context, Intent pushIntent, Notification.Builder builder) {
        int iconResourceId = 0;
        String iconString = PushIntentUtils.getIcon(pushIntent);
        if (iconString != null) {
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
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O && context.getDrawable(iconResourceId) instanceof AdaptiveIconDrawable) {
            AppCenterLog.error(LOG_TAG, "Adaptive icons make Notification center crash (system process) on Android 8.0 (was fixed on Android 8.1), " +
                    "please update your icon to be non adaptive or please use another icon to push.");
            iconResourceId = 0;
        }
        return iconResourceId;
    }
}


