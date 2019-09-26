/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * Inspect a pending or completed download.
 * This uses APIs that would trigger strict mode exception if used in UI thread.
 */
class DownloadManagerUpdateTask extends AsyncTask<Void, Void, Void> {

    private final WeakReference<DownloadManagerReleaseDownloader> mDownloader;

    DownloadManagerUpdateTask(DownloadManagerReleaseDownloader downloader) {
        mDownloader = new WeakReference<>(downloader);
    }

    @Override
    protected Void doInBackground(Void[] params) {
        DownloadManagerReleaseDownloader downloader = mDownloader.get();
        if (downloader != null) {
            downloader.onUpdate();
        }
        return null;
    }
}
