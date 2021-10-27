/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.content.Context;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;

/**
 * The factory that can be used to create an instance of a {@link DownloadManagerReleaseDownloader}.
 */
public class ReleaseDownloaderFactory {

    /**
     * Create release downloader instance.
     *
     * @param context        android context.
     * @param releaseDetails release to download.
     * @param listener       listener to be notified of status.
     * @return release downloader instance.
     */
    public static ReleaseDownloader create(Context context, ReleaseDetails releaseDetails, ReleaseDownloader.Listener listener) {
        return new DownloadManagerReleaseDownloader(context, releaseDetails, listener);
    }
}
