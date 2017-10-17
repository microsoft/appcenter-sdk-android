package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class PushNotifier {

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public static void handleNotification(Context context, Intent pushIntent)
            throws RuntimeException {
        //TODO is this needed? : context = context.getApplicationContext();
        /* Get icon identifiers from AndroidManifest.xml */
        //Bundle appMetaData = EngagementUtils.getMetaData(context);
        //mNotificationIcon = getIcon(appMetaData, METADATA_NOTIFICATION_ICON);
        int notificationIcon = 0; //TODO get notification icon?
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
        Notification notification;

        /* Use builder starting Android 3.0. */
        Notification.Builder builder = new Notification.Builder(context);

        /* Icon for ticker and content icon */
        builder.setSmallIcon(notificationIcon);
//
//            /*
//             * Large icon, handled only since API Level 11 (needs down scaling if too large because it's
//             * cropped otherwise by the system).
//             */
//            Bitmap notificationImage = content.getNotificationImage();
//            if (notificationImage != null) {
//                builder.setLargeIcon(scaleBitmapForLargeIcon(mContext, notificationImage));
//           }

        /* Texts */
        builder.setContentTitle(notificationTitle);
        builder.setContentText(notificationMessage);
        builder.setTicker(notificationTitle);
        builder.setWhen(System.currentTimeMillis());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            builder.setShowWhen(true);
        }

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

        /* Build method again depends on versions. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            notification = builder.build();
        }
        else {
            notification = builder.getNotification();
        }
        //TODO color, icon, sound - see what firebase does.
        notificationManager.notify(notificationId, notification);
    }

    @TargetApi(Build.VERSION_CODES.O)
    @SuppressWarnings("WeakerAccess")
    private static NotificationChannel getNotificationChannel()
    {
        /*
         * We can't vibrate with no sound anymore and we need 1 channel for each combination. Only the
         * name can be updated after creating a channel, even if deleting channel. Other updates are
         * simply ignored by Android. Only user has control on options after the channel is created
         * once.
         */
        //TODO use getexistingchannel to see what firebase does
        String id = "azme";
        String name;
        name = "Miscellaneous";
        int priority = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(id, name, priority);
        channel.enableVibration(false);
        return channel;
    }
}
