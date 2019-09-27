/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.os.AsyncTask;

/**
 * The download manager API triggers strict mode exception in UI thread.
 */
class DownloadManagerRequestTask extends AsyncTask<Void, Void, Void> {

    private final DownloadManagerReleaseDownloader mDownloader;

    DownloadManagerRequestTask(DownloadManagerReleaseDownloader downloader) {
        mDownloader = downloader;
    }

    @Override
    protected Void doInBackground(Void[] params) {
        mDownloader.onRequest(this);
        return null;
    }
}
