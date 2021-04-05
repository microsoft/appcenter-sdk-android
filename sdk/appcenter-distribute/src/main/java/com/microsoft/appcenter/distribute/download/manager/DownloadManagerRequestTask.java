/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.net.Uri;
import android.os.AsyncTask;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;

/**
 * The download manager API triggers strict mode exception in UI thread.
 */
class DownloadManagerRequestTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;
    private final String mTitle;

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
            
            /*
             * In cases when Download Manager application is disabled,
             * IllegalArgumentException: Unknown URL content://downloads/my_download is thrown.
             */
            mDownloader.onDownloadError(new IllegalStateException("Failed to start download: Download Manager is disabled.", e));
        } catch (Throwable throwable) {

            /*
             * We have to catch all types of errors to avoid crashes in runtime during downloading
             * a new release with the specific Gradle configuration:
             * release {
             *     minifyEnabled true
             *     proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
             * }
             *
             * Possible error:
             * Caused by: java.lang.VerifyError: Verifier rejected class b.c.a.n.j.c.c: java.lang.Object
             * b.c.a.n.j.c.c.doInBackground(java.lang.Object[]) failed to verify:
             * java.lang.Object b.c.a.n.j.c.c.doInBackground(java.lang.Object[]): [0x6E]
             * expected to be within a catch-all for an instruction where a monitor is held
             * (declaration of 'b.c.a.n.j.c.c' appears in base.apk).
             */
            throw throwable;
        }
        return null;
    }

    @VisibleForTesting
    DownloadManager.Request createRequest(Uri Uri) {
        return new DownloadManager.Request(Uri);
    }
}
