/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.Context;
import android.os.AsyncTask;

/**
 * Removing a download triggers strict mode exception in UI thread.
 */
class DownloadManagerRemoveTask extends AsyncTask<Void, Void, Void> {

    /**
     * Context.
     */
    @SuppressLint("StaticFieldLeak")
    private final Context mContext;

    /**
     * Download identifier to delete.
     */
    private final long mDownloadId;

    /**
     * Init.
     *
     * @param context    context.
     * @param downloadId download identifier to remove.
     */
    DownloadManagerRemoveTask(Context context, long downloadId) {
        mContext = context;
        mDownloadId = downloadId;
    }

    @Override
    protected Void doInBackground(Void... params) {

        /* This special cleanup task does not require any cancellation on state change as a previous download will never be reused. */
        DownloadManager downloadManager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.remove(mDownloadId);
        return null;
    }
}
