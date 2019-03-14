/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

/**
 * Class to hold current download progress status.
 */
class DownloadProgress {

    /**
     * Number of bytes downloaded so far.
     */
    private final long mCurrentSize;

    /**
     * Expected file size.
     */
    private final long mTotalSize;

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
