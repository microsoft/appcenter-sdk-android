/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

public class DownloadUtils {

    /**
     * Distribute service name.
     */
    private static final String SERVICE_NAME = "Distribute";

    /**
     * Base key for stored preferences.
     */
    public static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store a path to a downloaded .apk file.
     */
    public static final String PREFERENCE_KEY_DOWNLOADED_FILE = "PREFERENCE_KEY_DOWNLOADED_FILE";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    private static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Token used for handler callbacks to check download progress.
     */
    public static final String HANDLER_TOKEN_CHECK_PROGRESS = SERVICE_NAME + ".handler_token_check_progress";

    /**
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

    /**
     * How often to check download progress in millis.
     */
    static final long CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS = 1000;

    /**
     * Preference key to store the downloading release file uri.
     */
    static final String PREFERENCE_KEY_STORE_DOWNLOADING_RELEASE_APK_FILE = PREFERENCE_PREFIX + "downloading_release_apk_file";

    /**
     * Get download identifier from storage.
     *
     * @return download identifier or negative value if not found.
     */
    static long getStoredDownloadId() {
        return SharedPreferencesManager.getLong(PREFERENCE_KEY_DOWNLOAD_ID, INVALID_DOWNLOAD_IDENTIFIER);
    }
}
