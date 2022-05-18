package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.DeviceInfoHelper;

public class UpdateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            onPackageReplaced(context);
        }
    }

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
