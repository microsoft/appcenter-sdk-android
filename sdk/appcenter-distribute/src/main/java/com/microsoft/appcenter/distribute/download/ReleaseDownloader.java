/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
     * Cancel download and remove previously downloaded release.
     */
    void cancel();

    /**
     * Listener for downloading progress.
     */
    interface Listener {

        /**
         * Called when the downloading is starter.
         *
         * @param enqueueTime timestamp in milliseconds just before enqueuing download.
         */
        void onStart(long enqueueTime);

        /**
         * Called periodically during download to display current progress.
         *
         * @param currentSize count of already downloaded bytes.
         * @param totalSize   total size in bytes of downloading file.
         * @return <code>true</code> if the listener are interested on more progress updates, <code>false</code> otherwise.
         */
        boolean onProgress(long currentSize, long totalSize);

        /**
         * Called when the downloading is completed.
         *
         * @param localUri The local URI of the file.
         * @return <code>true</code> if this file can be installed, <code>false</code> otherwise.
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
