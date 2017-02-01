package com.microsoft.azure.mobile.updates;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Process download manager callbacks.
 */
public class DownloadCompletionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /* Check intent action. */
        switch (intent.getAction()) {

            /*
             * Just resume app if clicking on pending download notification as
             * it's always weird to click on a notification and nothing happening.
             * Another option would be to open download list.
             */
            case DownloadManager.ACTION_NOTIFICATION_CLICKED:
                PackageManager packageManager = context.getPackageManager();
                Intent resumeIntent = packageManager.getLaunchIntentForPackage(context.getPackageName());
                if (resumeIntent != null) {
                    resumeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(resumeIntent);
                }
                break;

            /*
             * Forward the download identifier to Updates for inspection.
             */
            case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                Updates.processCompletedDownload(context, downloadId);
                break;
        }
    }
}
