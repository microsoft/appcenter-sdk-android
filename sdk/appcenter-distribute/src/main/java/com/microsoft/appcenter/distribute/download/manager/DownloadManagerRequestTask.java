/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * The download manager API triggers strict mode exception in UI thread.
 */
class DownloadManagerRequestTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;
    private String mTitle;

    DownloadManagerRequestTask(DownloadManagerReleaseDownloader downloader, String title) {
        mDownloader = downloader;
        mTitle = title;
    }

    @Override
    protected Void doInBackground(Void... params) {

        /* Download file. */
        ReleaseDetails releaseDetails = mDownloader.getReleaseDetails();
        Uri downloadUrl = releaseDetails.getDownloadUrl();
        AppCenterLog.debug(LOG_TAG, "Start downloading new release from " + downloadUrl);
        DownloadManager downloadManager = mDownloader.getDownloadManager();
        DownloadManager.Request request = createRequest(downloadUrl);
        request.setTitle(String.format(mTitle, releaseDetails.getShortVersion(), releaseDetails.getVersion()));

        /* Hide mandatory download to prevent canceling via notification cancel or download UI delete. */
        if (releaseDetails.isMandatoryUpdate()) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
            request.setVisibleInDownloadsUi(false);
        }
        long enqueueTime = System.currentTimeMillis();
        try {
            long downloadId = downloadManager.enqueue(request);
            if (!isCancelled()) {
                mDownloader.onDownloadStarted(downloadId, enqueueTime);
            }
        } catch (IllegalArgumentException e) {
            AppCenterLog.error(LOG_TAG, "Failed to download: download manager app is disabled.");
        }
        return null;
    }

    @VisibleForTesting
    DownloadManager.Request createRequest(Uri Uri) {
        return new DownloadManager.Request(Uri);
    }
}
