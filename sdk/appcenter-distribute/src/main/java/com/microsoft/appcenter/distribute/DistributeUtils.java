/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.HashUtils;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;

import java.util.UUID;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_INSTALL_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_SCHEME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PRIVATE_UPDATE_SETUP_PATH_FORMAT;

/**
 * Some static util methods to avoid the main file getting too big.
 */
class DistributeUtils {

    /**
     * Scheme used to open the native Android tester app.
     */
    static final String TESTER_APP_PACKAGE_NAME = "com.microsoft.hockeyapp.testerapp";

    /**
     * Notification channel identifier.
     */
    static final String NOTIFICATION_CHANNEL_ID = "appcenter.distribute";

    /**
     * Get the notification identifier for downloads.
     *
     * @return notification identifier for downloads.
     */
    @VisibleForTesting
    static int getNotificationId() {
        return Distribute.class.getName().hashCode();
    }

    static void postNotification(@NonNull Context context, String title, String message, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            /* Create or update notification channel (mandatory on Android 8 target). */
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    context.getString(R.string.appcenter_distribute_notification_category),
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
            builder = new Notification.Builder(context, NOTIFICATION_CHANNEL_ID);
        } else {
            builder = getOldNotificationBuilder(context);
        }
        int pendingIntentFlag = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag = PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, pendingIntentFlag);
        builder.setTicker(title)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(context.getApplicationInfo().icon)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pendingIntent);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(DistributeUtils.getNotificationId(), notification);
    }

    static void cancelNotification(@NonNull Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(DistributeUtils.getNotificationId());
    }

    @NonNull
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private static Notification.Builder getOldNotificationBuilder(@NonNull Context context) {
        return new Notification.Builder(context);
    }

    /**
     * Get download state from storage.
     *
     * @return download state (completed by default).
     */
    static int getStoredDownloadState() {
        return SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_COMPLETED);
    }

    static Intent getResumeAppIntent(@NonNull Context context) {

        /*
         * Use our deep link activity with no parameter just to resume app correctly
         * without duplicating activities or clearing task.
         */
        Intent intent = new Intent(context, DeepLinkActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    @NonNull
    static String computeReleaseHash(@NonNull PackageInfo packageInfo) {
        return HashUtils.sha256(packageInfo.packageName + ":" + packageInfo.versionName + ":" + DeviceInfoHelper.getVersionCode(packageInfo));
    }

    /**
     * Update setup using native tester app.
     *
     * @param activity    activity from which to start tester app.
     * @param packageInfo package info.
     */
    static void updateSetupUsingTesterApp(Activity activity, PackageInfo packageInfo) {

        /* Compute hash. */
        String releaseHash = computeReleaseHash(packageInfo);

        /* Generate request identifier. */
        String requestId = UUID.randomUUID().toString();

        /* Build URL. */
        String url = "ms-actesterapp://update-setup";
        url += "?" + PARAMETER_RELEASE_HASH + "=" + releaseHash;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + activity.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        AppCenterLog.debug(LOG_TAG, "No token, need to open tester app to url=" + url);

        /* Store request id. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

        /* Open the native tester app */
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    /**
     * Update setup using browser.
     *
     * @param activity    activity from which to start browser.
     * @param installUrl  base install site URL.
     * @param appSecret   application secret.
     * @param packageInfo package info.
     */
    static void updateSetupUsingBrowser(Activity activity, String installUrl, String appSecret, PackageInfo packageInfo) {

        /*
         * If network is disconnected, browser will fail so wait.
         * Also we can't just wait for network to be up and launch browser at that time
         * as it's unpredictable and will interrupt the user, so just wait next relaunch.
         */
        if (!NetworkStateHelper.getSharedInstance(activity).isNetworkConnected()) {
            AppCenterLog.info(LOG_TAG, "Postpone enabling in app updates via browser as network is disconnected.");
            Distribute.getInstance().completeWorkflow();
            return;
        }

        /* Compute hash. */
        String releaseHash = computeReleaseHash(packageInfo);

        /* Generate request identifier. */
        String requestId = UUID.randomUUID().toString();

        /* Build URL. */
        String url = installUrl;
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, appSecret);
        url += "?" + PARAMETER_RELEASE_HASH + "=" + releaseHash;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + activity.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + IdHelper.getInstallId().toString();
        AppCenterLog.debug(LOG_TAG, "No token, need to open browser to url=" + url);

        /* Store request id. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId);

        /* Open browser, remember that whatever the outcome to avoid opening it twice. */
        BrowserUtils.openBrowser(url, activity);
    }

    /**
     * Get release details from cache if any.
     *
     * @return release details from cache or null.
     */
    static ReleaseDetails loadCachedReleaseDetails() {
        String cachedReleaseDetails = SharedPreferencesManager.getString(PREFERENCE_KEY_RELEASE_DETAILS);
        if (cachedReleaseDetails != null) {
            try {
                return ReleaseDetails.parse(cachedReleaseDetails);
            } catch (JSONException e) {
                AppCenterLog.error(LOG_TAG, "Invalid release details in cache.", e);
                SharedPreferencesManager.remove(PREFERENCE_KEY_RELEASE_DETAILS);
            }
        }
        return null;
    }

    static boolean isInvalidUpdateTrack(int updateTrack) {
        return updateTrack != UpdateTrack.PUBLIC && updateTrack != UpdateTrack.PRIVATE;
    }
}
