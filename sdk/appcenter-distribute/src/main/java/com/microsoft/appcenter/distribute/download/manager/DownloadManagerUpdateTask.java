/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.database.Cursor;
import android.os.AsyncTask;

import java.util.NoSuchElementException;

import static com.microsoft.appcenter.distribute.DistributeConstants.INVALID_DOWNLOAD_IDENTIFIER;

/**
 * Inspect a pending or completed download.
 * This uses APIs that would trigger strict mode exception if used in UI thread.
 */
class DownloadManagerUpdateTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;

    DownloadManagerUpdateTask(DownloadManagerReleaseDownloader downloader) {
        mDownloader = downloader;
    }

    @Override
    protected Void doInBackground(Void... params) {

        /* Query download manager. */
        DownloadManager downloadManager = mDownloader.getDownloadManager();
        long downloadId = mDownloader.getDownloadId();
        if (downloadId == INVALID_DOWNLOAD_IDENTIFIER) {
            mDownloader.onStart();
            return null;
        }
        try {
            Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor == null) {
                throw new NoSuchElementException("Cannot find download with id=" + downloadId);
            }
            try {
                if (!cursor.moveToFirst()) {
                    throw new NoSuchElementException("Cannot find download with id=" + downloadId);
                }
                if (isCancelled()) {
                    return null;
                }
                int status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_FAILED) {
                    int reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                    throw new IllegalStateException("The download has failed with reason code: " + reason);
                }
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    mDownloader.onDownloadProgress(cursor);
                    return null;
                }

                /* Complete download. */
                mDownloader.onDownloadComplete(cursor);
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            mDownloader.onDownloadError(e);
        }
        return null;
    }
}
