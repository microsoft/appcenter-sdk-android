package com.microsoft.appcenter.distribute.download;

import com.microsoft.appcenter.distribute.ReleaseDetails;

/**
 * Interface for downloading release.
 */
public interface ReleaseDownloader {

    /**
     * Start or resume downloading the installer for the release.
     *
     * @param releaseDetails
     */
    void download(ReleaseDetails releaseDetails);

    /**
     * Remove previously downloaded release.
     */
    void delete(long downloadId);

    /**
     * Set listener for download state.
     *
     * @param listener Download listener.
     */
    void setListener(Listener listener);

    /**
     * Remove listener for download state.
     */
    void removeListener();

    /**
     * Cancel the current task.
     *
     * @param state Interrupt the task.
     */
    void cancel(boolean state);

    /**
     *
     */
    interface Listener {

        /**
         * Called periodically during download to display current progress.
         *
         * @param downloadedBytes
         * @param totalBytes
         */
        void onProgress(long downloadedBytes, long totalBytes);

        /**
         * Called when the downloading is completed.
         *
         * @param localUri The local URI of the file.
         */
        void onComplete(String localUri);

        /**
         * Called when an error occurs during the downloading.
         *
         * @param errorMessage The message of the exception.
         */
        void onError(String errorMessage);
    }

}
