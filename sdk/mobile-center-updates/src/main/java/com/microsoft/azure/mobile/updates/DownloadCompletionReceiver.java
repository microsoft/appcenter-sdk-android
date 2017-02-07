package com.microsoft.azure.mobile.updates;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Process download manager callbacks.
 */
public class DownloadCompletionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /* Check intent action. */
        if (intent.getAction() != null) {
            switch (intent.getAction()) {

                /*
                 * Just resume app if clicking on pending download notification as
                 * it's always weird to click on a notification and nothing happening.
                 * Another option would be to open download list.
                 */
                case DownloadManager.ACTION_NOTIFICATION_CLICKED:
                    Updates.getInstance().resumeApp(context);
                    break;

                /*
                 * Forward the download identifier to Updates for inspection.
                 */
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
                    Updates.getInstance().processCompletedDownload(context, downloadId);
                    break;
            }
        }
    }
}
