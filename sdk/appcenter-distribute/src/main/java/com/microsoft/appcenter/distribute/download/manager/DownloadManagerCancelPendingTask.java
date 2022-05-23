/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.database.Cursor;
import android.os.AsyncTask;

/**
 * Cancel download if it's still in pending state.
 */
class DownloadManagerCancelPendingTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;

    /**
     * Download identifier to check.
     */
    private final long mDownloadId;

    DownloadManagerCancelPendingTask(DownloadManagerReleaseDownloader downloader, long downloadId) {
        mDownloader = downloader;
        mDownloadId = downloadId;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (isPending()) {
            mDownloader.clearDownloadId(mDownloadId);
            mDownloader.onDownloadError(new IllegalStateException("Failed to start downloading file due to timeout exception."));
        }
        return null;
    }

    /**
     * Checks if download is in pending state.
     *
     * @return <code>true</code> download is in pending state, <code>false</code> otherwise.
     */
    private boolean isPending() {
        DownloadManager.Query query = new DownloadManager.Query()
                .setFilterById(mDownloadId)
                .setFilterByStatus(DownloadManager.STATUS_PENDING);
        try (Cursor cursor = mDownloader.getDownloadManager().query(query)) {
            return cursor != null && cursor.moveToFirst();
        }
    }
}
