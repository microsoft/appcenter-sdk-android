package com.microsoft.appcenter.distribute.download;

import android.content.Context;
import android.os.Build;

public class ReleaseDownloaderFactory {

    private ReleaseDownloaderFactory() {
    }

    /**
     *  Create release downloader.
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
}
