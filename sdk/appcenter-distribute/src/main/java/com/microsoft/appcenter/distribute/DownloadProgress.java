package com.microsoft.appcenter.distribute;

/**
 * Class to hold current download progress status.
 */
class DownloadProgress {

    /**
     * Number of bytes downloaded so far.
     */
    private long mCurrentSize;

    /**
     * Expected file size.
     */
    private long mTotalSize;

    /**
     * Init.
     */
    DownloadProgress(long currentSize, long totalSize) {
        mCurrentSize = currentSize;
        mTotalSize = totalSize;
    }

    /**
     * @return Number of bytes downloaded so far.
     */
    long getCurrentSize() {
        return mCurrentSize;
    }

    /**
     * @return Expected file size.
     */
    long getTotalSize() {
        return mTotalSize;
    }
}
