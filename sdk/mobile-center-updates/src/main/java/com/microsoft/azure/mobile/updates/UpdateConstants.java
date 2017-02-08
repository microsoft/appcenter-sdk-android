package com.microsoft.azure.mobile.updates;

import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.MobileCenter;

final class UpdateConstants {

    /**
     * Update service name.
     */
    static final String SERVICE_NAME = "Updates";

    /**
     * Log tag for this service.
     */
    static final String LOG_TAG = MobileCenter.LOG_TAG + SERVICE_NAME;

    /**
     * Used for deep link intent from browser, string field for update token.
     */
    static final String EXTRA_UPDATE_TOKEN = "update_token";

    /**
     * Used for deep link intent from browser, string field for request identifier.
     */
    static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the last download file location on download manager if completed,
     * empty string while download is in progress, null if we launched install U.I.
     * If this is null and {@link #PREFERENCE_KEY_DOWNLOAD_ID} is not null, it's to remember we
     * downloaded a file for later removal (when we disable SDK or prepare a new download).
     * <p>
     * Rationale is that we keep the file in case the user chooses to install it from downloads U.I.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_URI = PREFERENCE_PREFIX + "download_uri";

    /**
     * Preference key to store the last download identifier.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Preference key for request identifier to validate deep link intent.
     */
    static final String PREFERENCE_KEY_REQUEST_ID = PREFERENCE_PREFIX + EXTRA_REQUEST_ID;

    /**
     * Preference key to store token.
     */
    static final String PREFERENCE_KEY_UPDATE_TOKEN = PREFERENCE_PREFIX + EXTRA_UPDATE_TOKEN;

    @VisibleForTesting
    UpdateConstants() {
    }
}
