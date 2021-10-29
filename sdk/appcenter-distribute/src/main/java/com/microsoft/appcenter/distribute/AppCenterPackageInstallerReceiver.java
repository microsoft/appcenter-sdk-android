/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.widget.Toast;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Locale;

/**
 * Process install manager callbacks.
 */
public class AppCenterPackageInstallerReceiver extends BroadcastReceiver {

    public static final String START_ACTION = "com.microsoft.appcenter.action.START";
    public static final String MY_PACKAGE_REPLACED_ACTION = "android.intent.action.MY_PACKAGE_REPLACED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (MY_PACKAGE_REPLACED_ACTION.equals(intent.getAction())) {
            AppCenterLog.debug(AppCenterLog.LOG_TAG, "Restart application after installing a new release.");
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(launchIntent);
        } else if (START_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    AppCenterLog.debug(AppCenterLog.LOG_TAG, "Ask confirmation to install a new release.");

                    /* This test app isn't privileged, so the user has to confirm the install. */
                    Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                    context.startActivity(confirmIntent);
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    AppCenterLog.debug(AppCenterLog.LOG_TAG, "Application was successfully updated.");
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    AppCenterLog.debug(AppCenterLog.LOG_TAG, String.format(Locale.ENGLISH, "Failed to install a new release with status: %s. Error message: %s.", status, message));
                    Toast.makeText(context, context.getString(R.string.something_goes_wrong_during_installing_new_release), Toast.LENGTH_SHORT).show();
                    break;
                default:
                    AppCenterLog.debug(AppCenterLog.LOG_TAG, String.format(Locale.ENGLISH, "Unrecognized status received from installer: %s", status));
                    Toast.makeText(context, context.getString(R.string.something_goes_wrong_during_installing_new_release), Toast.LENGTH_SHORT).show();
            }
        } else {
            AppCenterLog.debug(AppCenterLog.LOG_TAG, String.format(Locale.ENGLISH, "Unrecognized action %s - do nothing.", intent.getAction()));
        }
    }
}
