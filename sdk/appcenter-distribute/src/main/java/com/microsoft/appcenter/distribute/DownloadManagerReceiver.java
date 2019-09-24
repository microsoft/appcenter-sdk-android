/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;
import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;

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

            /* Check intent data is what we expected. */
            long expectedDownloadId = SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
            if (expectedDownloadId == INVALID_DOWNLOAD_IDENTIFIER || expectedDownloadId != downloadId) {
                AppCenterLog.debug(LOG_TAG, "Ignoring download identifier we didn't expect, id=" + downloadId);
                return;
            }
            Distribute.getInstance().installDownloadedUpdate(context);
        }
    }
}
