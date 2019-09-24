package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

import com.microsoft.appcenter.distribute.ReleaseDetails;
import com.microsoft.appcenter.distribute.download.http.HttpConnectionReleaseDownloader;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;


// TODO JavaDoc
public class ReleaseDownloaderFactory {

    /**
     * Create release downloader.
     *
     * @param context TODO
     * @return Release downloader.
     */
    public static ReleaseDownloader create(Context context, ReleaseDetails releaseDetails, ReleaseDownloader.Listener listener) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new HttpConnectionReleaseDownloader(context, releaseDetails, listener);
        }
        return new DownloadManagerReleaseDownloader(context, releaseDetails, listener);
    }
}
