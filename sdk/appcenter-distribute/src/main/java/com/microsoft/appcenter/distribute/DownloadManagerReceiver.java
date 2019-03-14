/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Process download manager callbacks.
 */
public class DownloadManagerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        /*
         * Just resume app if clicking on pending download notification as
         * it's awkward to click on a notification and nothing happening.
         * Another option would be to open download list.
         */
        String action = intent.getAction();
        if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
            Distribute.getInstance().resumeApp(context);
        }

        /*
         * Forward the download identifier to Distribute for inspection when a download completes.
         */
        else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);
            Distribute.getInstance().checkDownload(context, downloadId, false);
        }
    }
}
