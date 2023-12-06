/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install.session;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Locale;

/**
 * Process install manager callbacks.
 */
class InstallStatusReceiver extends BroadcastReceiver {

    @VisibleForTesting
    static final String INSTALL_STATUS_ACTION = "com.microsoft.appcenter.action.INSTALL_STATUS";

    /**
     * Raw value of PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT.
     * https://developer.android.com/reference/android/app/PendingIntent#FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
     * This flag will appear only in Android target SDK 34.
     */
    @VisibleForTesting
    private static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT_VALUE = 16777216;


    static IntentFilter getInstallerReceiverFilter() {
        IntentFilter installerReceiverFilter = new IntentFilter();
        installerReceiverFilter.addAction(INSTALL_STATUS_ACTION);
        return installerReceiverFilter;
    }

    /**
     * Return IntentSender with the receiver that listens to the package installer session status.
     *
     * @param context any context.
     * @param requestCode request code for the sender.
     * @return IntentSender with receiver.
     */
    static IntentSender getInstallStatusIntentSender(Context context, int requestCode) {
        int broadcastFlags = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            broadcastFlags = PendingIntent.FLAG_MUTABLE;
            if (Build.VERSION.SDK_INT >= 34) {
                broadcastFlags |= FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT_VALUE;
            }
        }
        // Suppress the warning as the flag PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT is unavailable on Android SDK < 34.
        @SuppressLint("WrongConstant") PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                new Intent(INSTALL_STATUS_ACTION),
                broadcastFlags);
        return pendingIntent.getIntentSender();
    }

    private final SessionReleaseInstaller mInstaller;

    InstallStatusReceiver(@NonNull SessionReleaseInstaller installer) {
        mInstaller = installer;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        AppCenterLog.verbose(LOG_TAG, "Receive broadcast action: " + action);
        if (INSTALL_STATUS_ACTION.equals(action)) {
            onInstallStatus(intent);
        }
    }

    private void onInstallStatus(Intent intent) {
        Bundle extras = intent.getExtras();
        for (String key : extras.keySet()) {
            AppCenterLog.verbose(LOG_TAG, "\t" + key + ": " + extras.get(key));
        }
        int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
        int sessionId = extras.getInt(PackageInstaller.EXTRA_SESSION_ID);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Intent confirmIntent = extras.getParcelable(Intent.EXTRA_INTENT);
                mInstaller.onInstallConfirmation(sessionId, confirmIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                AppCenterLog.info(LOG_TAG, "Application was successfully updated.");
                break;
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                mInstaller.onInstallCancel(sessionId);
                break;
            case PackageInstaller.STATUS_FAILURE:
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
            case PackageInstaller.STATUS_FAILURE_INVALID:
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_STORAGE:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
                mInstaller.onInstallError(sessionId, message);
                break;
            default:
                AppCenterLog.warn(LOG_TAG, String.format(Locale.ENGLISH, "Unrecognized status received from installer: %s", status));
        }
    }
}
