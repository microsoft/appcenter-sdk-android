package com.microsoft.azure.mobile.push;

import android.annotation.SuppressLint;
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
import android.net.Uri;
import android.os.Build;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class PushNotifier {

    /**
     * Default channel.
     */
    static final String CHANNEL_ID = "app_center_push";
    static final String CHANNEL_NAME = "Push";

    private Context mContext;

    public PushNotifier(Context context) {
        //TODO is this line necessary? : mContext = context.getApplicationContext();
        mContext = context;
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    public void handleNotification(Intent pushIntent)
            throws RuntimeException {
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);

        /* Generate notification identifier using the hash of the Google message id. */
        int notificationId = PushIntentUtils.getGoogleMessageId(pushIntent).hashCode();

        /* Click action. */
        PackageManager packageManager = mContext.getPackageManager();
        Intent actionIntent = packageManager.getLaunchIntentForPackage(mContext.getPackageName());
        actionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Map<String, String> customData = PushIntentUtils.getCustomData(pushIntent);
        for (String key : customData.keySet()) {
            actionIntent.putExtra(key, customData.get(key));
        }

        /* Reuse notification id for simplicity. */
        PendingIntent contentIntent = PendingIntent.getActivity(mContext, notificationId,
                actionIntent, 0);

        /* Get text. */
        String notificationTitle = PushIntentUtils.getTitle(pushIntent);
        String notificationMessage = PushIntentUtils.getMessage(pushIntent);
        Notification.Builder builder = new Notification.Builder(mContext);

        /* Set color. */
        setColor(pushIntent, builder);

        /* Set icon. */
        setIcon(pushIntent, builder);

        /* Set sound. */
        setSound(pushIntent, builder);

        /* Texts */
        builder.setContentTitle(notificationTitle).
                setContentText(notificationMessage).
                setWhen(System.currentTimeMillis());

        /* Click action. */
        builder.setContentIntent(contentIntent);

        /* Manage notification channel on Android O. */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && mContext.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.O) {

            /* Get channel. */
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

            /* Create or update channel. */
            notificationManager.createNotificationChannel(channel);

            /* And associate to notification. */
            builder.setChannelId(channel.getId());
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

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void setColor(Intent pushIntent, Notification.Builder builder) {
        String colorString = PushIntentUtils.getColor(pushIntent);
        if (colorString == null) {
            return;
        }
        try {
            builder.setColor(Color.parseColor(colorString));
            return;
        }
        catch (IllegalArgumentException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(),
                    "Provided color resource not found.");
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void setSound(Intent pushIntent, Notification.Builder builder) {
        if (!PushIntentUtils.useAnySound(pushIntent)) {
            return;
        }
        String customSound = PushIntentUtils.getCustomSound(pushIntent);
        if (customSound == null) {
            builder.setDefaults(Notification.DEFAULT_SOUND);
            return;
        }
        try {
            Resources resources = mContext.getResources();
            int id = resources.getIdentifier(customSound, "raw", mContext.getPackageName());
            Uri soundUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(resources.getResourcePackageName(id))
                    .appendPath(resources.getResourceTypeName(id))
                    .appendPath(resources.getResourceEntryName(id))
                    .build();
            builder.setSound(soundUri);
        }
        catch (Resources.NotFoundException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(),
                    "Sound file '" + customSound + "' not found; falling back to default.");
            builder.setDefaults(Notification.DEFAULT_SOUND);
        }
    }

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    private void setIcon(Intent pushIntent, Notification.Builder builder) {
        String iconString = PushIntentUtils.getIcon(pushIntent);
        if (iconString != null) {
            Resources resources = mContext.getResources();
            String packageName = mContext.getPackageName();
            int iconResourceId = resources.getIdentifier(iconString, "drawable", packageName);
            if (iconResourceId != 0) {
                MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                        "Found icon resource in 'drawable'.");
                builder.setSmallIcon(iconResourceId);
                return;
            }
            iconResourceId = resources.getIdentifier(iconString, "mipmap", packageName);
            if (iconResourceId != 0) {
                MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                        "Found icon resource in 'mipmap'.");
                builder.setSmallIcon(iconResourceId);
                return;
            }
        }
        MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                "Using application icon as notification icon.");
        builder.setSmallIcon(mContext.getApplicationInfo().icon);
    }
}


