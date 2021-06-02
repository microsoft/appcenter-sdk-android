/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;

/**
 * Distribute constants.
 */
public final class DistributeConstants {

    /**
     * Distribute service name.
     */
    static final String SERVICE_NAME = "DistributePlay";

    /**
     * Log tag for this service.
     */
    public static final String LOG_TAG = AppCenter.LOG_TAG + SERVICE_NAME;

    /**
     * Invalid download identifier.
     */
    public static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

    /**
     * Token used for handler callbacks to check download progress.
     */
    public static final String HANDLER_TOKEN_CHECK_PROGRESS = SERVICE_NAME + ".handler_token_check_progress";

    /**
     * The download progress will be reported after loading this number of bytes.
     */
    public static final long UPDATE_PROGRESS_BYTES_THRESHOLD = 512 * 1024;

    /**
     * The download progress will be reported not more often than this number of milliseconds.
     */
    public static final long UPDATE_PROGRESS_TIME_THRESHOLD = 500;

    /**
     * 1 KiB in bytes (this not a kilobyte).
     */
    public static final long KIBIBYTE_IN_BYTES = 1024;

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    public static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Preference key to store the downloading release file path.
     */
    public static final String PREFERENCE_KEY_DOWNLOADED_RELEASE_FILE = PREFERENCE_PREFIX + "downloaded_release_file";

    @VisibleForTesting
    DistributeConstants() {

        /* Hide constructor as it's just a constant class. */
    }
}
