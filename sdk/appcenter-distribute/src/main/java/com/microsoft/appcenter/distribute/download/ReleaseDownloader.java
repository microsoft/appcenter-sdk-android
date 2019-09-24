package com.microsoft.appcenter.distribute.download;

import android.net.Uri;
import android.support.annotation.NonNull;

/**
 * Interface for downloading release.
 */
public interface ReleaseDownloader {

    /**
     * Start or resume downloading the installer for the release.
     */
    void resume();

    /**
     * Remove previously downloaded release.
     */
    void delete();

    /**
     * TODO
     */
    interface Listener {

        /**
         * Called when the downloading is starter.
         *
         * @param enqueueTime time just before enqueuing download.
         */
        void onStart(long enqueueTime);

        /**
         * Called periodically during download to display current progress.
         */
        boolean onProgress(long currentSize, long totalSize);

        /**
         * Called when the downloading is completed.
         *
         * @param localUri The local URI of the file.
         */
        boolean onComplete(@NonNull Uri localUri);

        /**
         * Called when an error occurs during the downloading.
         *
         * @param errorMessage The message of the exception.
         */
        void onError(String errorMessage);
    }

}
