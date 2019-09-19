package com.microsoft.appcenter.distribute.download;

import com.microsoft.appcenter.distribute.ReleaseDetails;

/**
 *
 */
public interface ReleaseDownloader {

    /**
     * Download the file for current release.
     *
     * @param releaseDetails
     */
    void download(ReleaseDetails releaseDetails);

    /**
     * Remove previously file.
     */
    void delete();

    /**
     * Listener for download states.
     *
     * @param listener Download listener.
     */
    void setListener(Listener listener);

    /**
     *
     */
    interface Listener {

        /**
         * Shows current status of the downloading.
         *
         * @param downloadedBytes
         * @param totalBytes
         */
        void onProgress(long downloadedBytes, long totalBytes);

        /**
         * Implement this method to handle successful REST call results.
         *
         * @param localUri The local URI of the file.
         */
        void onComplete(String localUri);

        /**
         * Implement this method to handle REST call failures.
         *
         * @param errorMessage The message of the exception.
         */
        void onError(String errorMessage);
    }

}
