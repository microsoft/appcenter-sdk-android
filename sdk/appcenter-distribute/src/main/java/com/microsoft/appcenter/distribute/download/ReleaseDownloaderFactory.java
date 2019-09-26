/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.http.HttpConnectionReleaseDownloader;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;

/**
 * The factory that can be used to create an instance of a {@link HttpConnectionReleaseDownloader} or a {@link DownloadManagerReleaseDownloader}.
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

        /*
         * DownloadManager on Android 4.x doesn't enable TLS 1.2 so the download keeps retry in SSL handshake failure.
         * Switch to direct downloading via HttpsURLConnection for Android versions prior to 5.0 because TLS 1.2 is enforced now.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new HttpConnectionReleaseDownloader(context, releaseDetails, listener);
        }
        return new DownloadManagerReleaseDownloader(context, releaseDetails, listener);
    }
}
