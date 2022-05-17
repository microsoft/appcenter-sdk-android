/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

import java.util.Locale;

/**
 * Process install manager callbacks.
 */
public class AppCenterPackageInstallerReceiver extends BroadcastReceiver {

    @VisibleForTesting
    static final String INSTALL_STATUS_ACTION = "com.microsoft.appcenter.action.INSTALL_STATUS";

    @VisibleForTesting
    static final String MY_PACKAGE_REPLACED_ACTION = "android.intent.action.MY_PACKAGE_REPLACED";

    static IntentFilter getInstallerReceiverFilter() {
        IntentFilter installerReceiverFilter = new IntentFilter();
        installerReceiverFilter.addAction(INSTALL_STATUS_ACTION);
        installerReceiverFilter.addAction(MY_PACKAGE_REPLACED_ACTION);
        return installerReceiverFilter;
    }

    /**
     * Return IntentSender with the receiver that listens to the package installer session status.
     *
     * @param context any context.
     * @param sessionId install sessionId.
     * @return IntentSender with receiver.
     */
    static IntentSender getInstallStatusIntentSender(Context context, int sessionId) {
        int broadcastFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            broadcastFlags = PendingIntent.FLAG_MUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(INSTALL_STATUS_ACTION),
                broadcastFlags);
        return pendingIntent.getIntentSender();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (MY_PACKAGE_REPLACED_ACTION.equals(action)) {
            onPackageReplaced(context);
        } else if (INSTALL_STATUS_ACTION.equals(action)) {
            onInstallStatus(context, intent);
        } else {
            AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH, "Unrecognized action %s - do nothing.", action));
        }
    }

    private void onPackageReplaced(Context context) {
        AppCenterLog.debug(LOG_TAG, "Post a notification as the installation finished in background.");
        String title = context.getString(R.string.appcenter_distribute_install_completed_title);
        Intent intent = DistributeUtils.getResumeAppIntent(context);
        DistributeUtils.postNotification(context, title, getInstallCompletedMessage(context), intent);
    }

    private static String getInstallCompletedMessage(Context context) {
        String versionName = "";
        int versionCode = 0;
        PackageInfo packageInfo = DeviceInfoHelper.getPackageInfo(context);
        if (packageInfo != null) {
            versionName =  packageInfo.versionName;
            versionCode = DeviceInfoHelper.getVersionCode(packageInfo);
        }
        String format = context.getString(R.string.appcenter_distribute_install_completed_message);
        String appName = AppNameHelper.getAppName(context);
        return String.format(format, appName, versionName, versionCode);
    }

    private void onInstallStatus(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                onInstallStatusPendingUserAction(context, confirmIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                onInstallStatusSuccess();
                break;
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_ABORTED:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
                onInstallStatusFailure(status, message);
                break;
            default:
                AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH, "Unrecognized status received from installer: %s", status));
        }
    }

    private void onInstallStatusPendingUserAction(Context context, Intent confirmIntent) {
        AppCenterLog.debug(LOG_TAG, "Ask confirmation to install a new release.");
        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(confirmIntent);
    }

    private void onInstallStatusSuccess() {
        AppCenterLog.debug(LOG_TAG, "Application was successfully updated.");
    }

    private void onInstallStatusFailure(int status, String message) {
        AppCenterLog.debug(LOG_TAG, String.format(Locale.ENGLISH, "Failed to install a new release with status: %s. Error message: %s.", status, message));
        Distribute.getInstance().showInstallingErrorToast();
    }
}
