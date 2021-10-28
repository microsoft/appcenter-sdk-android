/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.distribute.ReleaseDetails;

/**
 * Interface for downloading release.
 */
public interface ReleaseDownloader {

    /**
     * Check if downloading was already started (during the current session).
     * <p>
     * Note: It's NOT supposed to check status after app process restart.
     * </p>
     *
     * @return <code>true</code> if download progress is already started, <code>false</code> otherwise.
     */
    boolean isDownloading();

    /**
     * Get release details.
     *
     * @return release details.
     */
    @NonNull
    ReleaseDetails getReleaseDetails();

    /**
     * Start or resume downloading the installer for the release.
     */
    @AnyThread
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
        @WorkerThread
        void onStart(long enqueueTime);

        /**
         * Called periodically during download to display current progress.
         *
         * @param currentSize count of already downloaded bytes.
         * @param totalSize   total size in bytes of downloading file.
         * @return <code>true</code> if the listener are interested on more progress updates, <code>false</code> otherwise.
         */
        @WorkerThread
        boolean onProgress(long currentSize, long totalSize);

        /**
         * Called when the downloading is completed.
         *
         * @param downloadId downloadId of downloaded file.
         * @return <code>true</code> if this file can be installed, <code>false</code> otherwise.
         */
        @WorkerThread
        void onComplete(@NonNull Long downloadId);

        /**
         * Called when an error occurs during the downloading.
         *
         * @param errorMessage The message of the exception.
         */
        void onError(@Nullable String errorMessage);
    }
}
