/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;

/**
 * The download manager API triggers strict mode exception in U.I. thread.
 */
class DownloadManagerRequestTask extends AsyncTask<Void, Void, Void> {

    private final WeakReference<DownloadManagerReleaseDownloader> mDownloader;

    DownloadManagerRequestTask(DownloadManagerReleaseDownloader downloader) {
        mDownloader = new WeakReference<>(downloader);
    }

    @Override
    protected Void doInBackground(Void[] params) {
        DownloadManagerReleaseDownloader downloader = mDownloader.get();
        if (downloader != null) {
            downloader.onRequest(this);
        }
        return null;
    }
}
