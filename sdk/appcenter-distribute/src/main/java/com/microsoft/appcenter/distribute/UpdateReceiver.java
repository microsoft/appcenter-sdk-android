/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

/**
 * Receiver of package replaced (update installed) callback.
 */
public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        AppCenterLog.verbose(LOG_TAG, "Receive broadcast action: " + action);
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            onPackageReplaced(context);
        }
    }

    /**
     * Package replaced callback. Posting notification that the installation finished.
     *
     * @param context the context in which the receiver is running.
     */
    private void onPackageReplaced(Context context) {
        AppCenterLog.debug(LOG_TAG, "Post a notification as the installation finished in background.");
        String title = context.getString(R.string.appcenter_distribute_install_completed_title);
        Intent intent = DistributeUtils.getResumeAppIntent(context);
        DistributeUtils.postNotification(context, title, getInstallCompletedMessage(context), intent);
    }

    private static String getInstallCompletedMessage(Context context) {
        String versionName = "?";
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
}
