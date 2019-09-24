/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download;

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
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;
}
