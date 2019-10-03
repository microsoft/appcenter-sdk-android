/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import android.os.AsyncTask;

import java.io.File;

class HttpConnectionCheckTask extends AsyncTask<Void, Void, Void> {

    private final HttpConnectionReleaseDownloader mDownloader;

    HttpConnectionCheckTask(HttpConnectionReleaseDownloader downloader) {
        mDownloader = downloader;
    }

    @Override
    protected Void doInBackground(Void... params) {
        File targetFile = mDownloader.getTargetFile();
        if (targetFile == null) {
            mDownloader.onDownloadError("Cannot access to downloads folder. Shared storage is not currently available.");
            return null;
        }

        /* Check if we have already downloaded the release. */
        String downloadedReleaseFilePath = mDownloader.getDownloadedReleaseFilePath();
        if (downloadedReleaseFilePath != null) {

            /* Check if it's the same release. */
            File downloadedReleaseFile = new File(downloadedReleaseFilePath);
            if (downloadedReleaseFilePath.equals(targetFile.getAbsolutePath())) {
                if (downloadedReleaseFile.exists()) {
                    mDownloader.onDownloadComplete(targetFile);
                    return null;
                }
            } else {

                /* Remove previously downloaded release. */
                //noinspection ResultOfMethodCallIgnored
                downloadedReleaseFile.delete();
                mDownloader.setDownloadedReleaseFilePath(null);
            }
        }
        if (isCancelled()) {
            return null;
        }
        mDownloader.onStart(targetFile);
        return null;
    }
}
