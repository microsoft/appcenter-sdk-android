/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.install;

import android.net.Uri;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

/**
 * Interface for installing release.
 */
public interface ReleaseInstaller {

    /**
     * Start installation of downloaded release.
     *
     * @param localUri path to local file.
     */
    @AnyThread
    void install(@NonNull Uri localUri);

    /**
     * Clear resources that were opened for installation.
     */
    void clear();

    /**
     * Listener for installing progress.
     */
    interface Listener {

        /**
         * Called when an error occurs during the downloading.
         *
         * @param errorMessage The message of the exception.
         */
        void onError(String errorMessage);

        /**
         * Called when the installation is cancelled.
         */
        void onCancel();
    }
}
