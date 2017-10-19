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
import android.os.Bundle;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;

public class PushNotifier {

    /**
     * Default channel.
     */
    static final String DEFAULT_CHANNEL_ID = "fcm_fallback_notification_channel";
    static final String DEFAULT_CHANNEL_NAME = "Miscellaneous";

    /**
     * Meta-data keys for notification overrides.
     */
    static final String META_CHANNEL_ID_KEY = "com.google.firebase.messaging.default_notification_channel_id";
    static final String META_DEFAULT_COLOR_ID_KEY = "com.google.firebase.messaging.default_notification_color";
    static final String META_DEFAULT_ICON_ID_KEY = "com.google.firebase.messaging.default_notification_icon";

    private Context mContext;
    private String mChannelId;
    private int mDefaultColorId;
    private int mDefaultIconId;

    public PushNotifier(Context context) {
        //TODO is this line necessary? : mContext = context.getApplicationContext();
        mContext = context;

        /* Get meta data. */
        Bundle metaData = null;
        try {
            metaData = context.getPackageManager().getApplicationInfo(context.getPackageName(),
                    PackageManager.GET_META_DATA).metaData;
        }
        catch (Exception e) {
            /*
             * NameNotFoundException or in some rare scenario an undocumented "RuntimeException: Package
             * manager has died.", probably caused by a system app process crash.
             */
        }
        if (metaData != null) {
            mChannelId = metaData.getString(META_CHANNEL_ID_KEY, DEFAULT_CHANNEL_ID);
            mDefaultIconId = metaData.getInt(META_DEFAULT_ICON_ID_KEY);
            mDefaultColorId = metaData.getInt(META_DEFAULT_COLOR_ID_KEY);
        }
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
            NotificationChannel channel = new NotificationChannel(mChannelId,
                    DEFAULT_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);

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
        if (colorString != null) {
            try {
                builder.setColor(Color.parseColor(colorString));
                return;
            }
            catch (IllegalArgumentException e) {
                MobileCenterLog.warn(Push.getInstance().getLoggerTag(),
                        "Provided color resource not found; falling back to default.");
            }
        }
        if (mDefaultColorId != 0) {
            try {
                builder.setColor(mContext.getColor(mDefaultColorId));
            }
            catch (Resources.NotFoundException e) {
                MobileCenterLog.warn(Push.getInstance().getLoggerTag(),
                        "Default color resource not found.");
            }
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
        int iconResourceId = 0;
        String iconString = PushIntentUtils.getIcon(pushIntent);
        if (iconString != null) {
            iconResourceId = mContext.getResources().getIdentifier(iconString, "drawable", mContext.getPackageName());
            if (iconResourceId == 0) {
                iconResourceId = mContext.getResources().getIdentifier(iconString, "mipmap", mContext.getPackageName());
            }
        }
        if (iconResourceId == 0 && mDefaultIconId != 0) {
            iconResourceId = mDefaultIconId;
        }
        if (iconResourceId == 0) {
            iconResourceId = mContext.getApplicationInfo().icon;
        }
        builder.setSmallIcon(iconResourceId);
    }
}


