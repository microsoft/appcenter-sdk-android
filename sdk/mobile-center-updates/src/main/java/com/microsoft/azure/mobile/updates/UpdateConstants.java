package com.microsoft.azure.mobile.updates;

import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.MobileCenter;

/**
 * Updates constants.
 */
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
     * Base URL used to open browser to login.
     */
    static final String DEFAULT_LOGIN_URL = "https://install.mobile.azure.com";

    /**
     * Base URL to call server to check latest release.
     */
    static final String DEFAULT_API_URL = "https://api.mobile.azure.com";

    /**
     * Login URL path. Contains the app secret variable to replace.
     * Trailing slash needed to avoid redirection that can lose the query string on some servers.
     */
    static final String LOGIN_PAGE_URL_PATH_FORMAT = "/apps/%s/update-setup/";

    /**
     * Check latest release API URL path. Contains the app secret variable to replace.
     */
    static final String CHECK_UPDATE_URL_PATH_FORMAT = "/sdk/apps/%s/releases/latest";

    /**
     * API parameter for release hash.
     */
    static final String PARAMETER_RELEASE_HASH = "release_hash";

    /**
     * API parameter for redirect URL.
     */
    static final String PARAMETER_REDIRECT_ID = "redirect_id";

    /**
     * API parameter for request identifier.
     */
    static final String PARAMETER_REQUEST_ID = "request_id";

    /**
     * API parameter for platform.
     */
    static final String PARAMETER_PLATFORM = "platform";

    /**
     * API parameter value for this platform.
     */
    static final String PARAMETER_PLATFORM_VALUE = "Android";

    /**
     * Header used to pass token when checking latest release.
     */
    static final String HEADER_API_TOKEN = "x-api-token";

    /**
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

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

    /**
     * Preference key to store ignored release id.
     */
    static final String PREFERENCE_KEY_IGNORED_RELEASE_ID = PREFERENCE_PREFIX + "ignored_release_id";

    @VisibleForTesting
    UpdateConstants() {

        /* Hide constructor as it's just a constant class. */
    }
}
