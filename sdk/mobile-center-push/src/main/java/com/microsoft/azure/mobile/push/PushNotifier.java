package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class PushNotifier {

    /* Default channel. */
    static final String DEFAULT_CHANNEL_ID = "fcm_fallback_notification_channel";
    static final String DEFAULT_CHANNEL_NAME = "Miscellaneous";

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void handleNotification(Context context, Intent pushIntent)
            throws RuntimeException {
        //TODO is this needed? : context = context.getApplicationContext();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        /* Generate notification identifier using the hash of the Google message id. */
        int notificationId = PushIntentUtils.getGoogleMessageId(pushIntent).hashCode();

        /* Click action. */
        PackageManager packageManager = context.getPackageManager();
        Intent actionIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Map<String, String> customData = PushIntentUtils.getCustomData(pushIntent);
        for (String key : customData.keySet()) {
            actionIntent.putExtra(key, customData.get(key));
        }

        /* Reuse notification id for simplicity. */
        PendingIntent contentIntent = PendingIntent.getActivity(context, notificationId,
                actionIntent, 0);

        /* Get text. */
        String notificationTitle = PushIntentUtils.getTitle(pushIntent);
        String notificationMessage = PushIntentUtils.getMessage(pushIntent);
        Notification.Builder builder = new Notification.Builder(context);

        /* Set color. */
        String colorString = PushIntentUtils.getColor(pushIntent);
        if (colorString != null) {
            //TODO handle case when format is wrong
            /* Remove '#'. */
            colorString = colorString.substring(1);
            int colorVal = Integer.parseInt(colorString, 16);
            builder.setColor(colorVal);
        }

        /* Set icon. */
        String iconString = PushIntentUtils.getIcon(pushIntent);
        if (iconString != null) {
            //TODO handle case when format is wrong
            //TODO Invalid icon case: use launcher icon
            //TODO make sure to prioritize drawable over mipmap if it exists in both
            builder.setSmallIcon(Integer.parseInt(iconString));
        }

        /* Set sound. */
        String soundString = PushIntentUtils.getSound(pushIntent);
        if (soundString != null) {
            if (soundString.equals("default")) {
                builder.setDefaults(Notification.DEFAULT_SOUND);
            }
            else {
                Resources resources = context.getResources();
                int id = resources.getIdentifier(soundString, "raw", context.getPackageName());
                //TODO handle case when resource is not found
                Uri soundUri = new Uri.Builder()
                        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                        .authority(resources.getResourcePackageName(id))
                        .appendPath(resources.getResourceTypeName(id))
                        .appendPath(resources.getResourceEntryName(id))
                        .build();
                builder.setSound(soundUri);
            }
        }

        /* Texts */
        builder.setContentTitle(notificationTitle).
                setContentText(notificationMessage).
                setWhen(System.currentTimeMillis());

        /* Click action. */
        builder.setContentIntent(contentIntent);

        /* Manage notification channel on Android O. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O) {

            /* Get channel. */
            NotificationChannel channel = getNotificationChannel();
            if (channel != null) {

                /* Create or update channel. */
                notificationManager.createNotificationChannel(channel);

                /* And associate to notification. */
                builder.setChannelId(channel.getId());
            }
        }
        Notification notification;

        /* Build method depends on versions. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        }
        else {
            notification = builder.getNotification();
        }
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(notificationId, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @SuppressWarnings("WeakerAccess")
    private static NotificationChannel getNotificationChannel()
    {
        NotificationChannel channel = new NotificationChannel(DEFAULT_CHANNEL_ID,
                DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
        return channel;
    }
}
