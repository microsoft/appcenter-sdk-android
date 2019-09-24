package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

import com.microsoft.appcenter.distribute.HttpReleaseDownloadListener;
import com.microsoft.appcenter.distribute.ManagerReleaseDownloadListener;
import com.microsoft.appcenter.distribute.download.http.HttpConnectionReleaseDownloader;
import com.microsoft.appcenter.distribute.download.manager.DownloadManagerReleaseDownloader;


public class ReleaseDownloaderFactory {

    private ReleaseDownloaderFactory() {
    }

    /**
     * Create release downloader.
     *
     * @param context
     * @return Release downloader.
     */
    public static ReleaseDownloader create(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new HttpConnectionReleaseDownloader(context);
        }
        return new DownloadManagerReleaseDownloader(context);
    }

    public static ReleaseDownloader.Listener createListener(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return new HttpReleaseDownloadListener(context);
        }
        return new ManagerReleaseDownloadListener(context);
    }
}
