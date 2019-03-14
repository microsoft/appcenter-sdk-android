/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.DownloadManager;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.AppCenter;

/**
 * Distribute constants.
 */
final class DistributeConstants {

    /**
     * Distribute service name.
     */
    static final String SERVICE_NAME = "Distribute";

    /**
     * Log tag for this service.
     */
    static final String LOG_TAG = AppCenter.LOG_TAG + SERVICE_NAME;

    /**
     * Used for deep link intent from browser, string field for update token.
     */
    static final String EXTRA_UPDATE_TOKEN = "update_token";

    /**
     * Used for deep link intent from browser, string field for request identifier.
     */
    static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * Used for deep link intent from browser, string field for distribution group identifier.
     */
    static final String EXTRA_DISTRIBUTION_GROUP_ID = "distribution_group_id";

    /**
     * Used for download count reporting API, string field for distribution group identifier.
     */
    static final String PARAMETER_DISTRIBUTION_GROUP_ID = "distribution_group_id";

    /**
     * Used for deep link intent from browser, string field for update setup failed identifier.
     */
    static final String EXTRA_UPDATE_SETUP_FAILED = "update_setup_failed";

    /**
     * Used for deep link intent from browser, string field for tester app update setup failed identifier.
     */
    static final String EXTRA_TESTER_APP_UPDATE_SETUP_FAILED = "tester_app_update_setup_failed";

    /**
     * Base URL used to open browser to check install and get API token to check latest release.
     */
    static final String DEFAULT_INSTALL_URL = "https://install.appcenter.ms";

    /**
     * Base URL to call server to check latest release.
     */
    static final String DEFAULT_API_URL = "https://api.appcenter.ms/v0.1";

    /**
     * Update setup URL path. Contains the app secret variable to replace.
     * Trailing slash needed to avoid redirection that can lose the query string on some servers.
     */
    static final String UPDATE_SETUP_PATH_FORMAT = "/apps/%s/update-setup/";

    /**
     * Check latest private release API URL path. Contains the app secret variable to replace.
     */
    static final String GET_LATEST_PRIVATE_RELEASE_PATH_FORMAT = "/sdk/apps/%s/releases/latest?release_hash=%s%s";

    /**
     * Check latest public release API URL path. Contains the app secret variable to replace.
     */
    static final String GET_LATEST_PUBLIC_RELEASE_PATH_FORMAT = "/public/sdk/apps/%s/distribution_groups/%s/releases/latest?release_hash=%s%s";

    /**
     * API parameter for release hash.
     */
    static final String PARAMETER_RELEASE_HASH = "release_hash";

    /**
     * API parameter for release identifier.
     */
    static final String PARAMETER_RELEASE_ID = "downloaded_release_id";

    /**
     * API parameter for redirect URL.
     */
    static final String PARAMETER_REDIRECT_ID = "redirect_id";

    /**
     * API parameter for redirection scheme.
     */
    static final String PARAMETER_REDIRECT_SCHEME = "redirect_scheme";

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
     * API parameter for setup failure redirect key.
     */
    static final String PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY = "enable_failure_redirect";

    /**
     * API parameter for update setup failed key.
     */
    static final String PARAMETER_UPDATE_SETUP_FAILED = "update_setup_failed";

    /**
     * API parameter for install identifier.
     */
    static final String PARAMETER_INSTALL_ID = "install_id";

    /**
     * Header used to pass token when checking latest release.
     */
    static final String HEADER_API_TOKEN = "x-api-token";

    /**
     * Invalid download identifier.
     */
    static final long INVALID_DOWNLOAD_IDENTIFIER = -1;

    /**
     * After we show install U.I, the download is mark completed but we keep the file.
     * No download is also using this value.
     */
    static final int DOWNLOAD_STATE_COMPLETED = 0;

    /**
     * If the update is mandatory we remember the dialog was displayed to say a new download
     * was available. We use that to restore blocking dialog at restart
     * (while checking for more recent in background).
     */
    static final int DOWNLOAD_STATE_AVAILABLE = 1;

    /**
     * We are waiting to hear back from download manager, we may poll status on process restart.
     */
    static final int DOWNLOAD_STATE_ENQUEUED = 2;

    /**
     * Download is finished, notification was posted but user could ignore it,
     * we use that state to show install U.I 1 time when application is resumed.
     */
    static final int DOWNLOAD_STATE_NOTIFIED = 3;

    /**
     * State used for mandatory update to block app until user installs the app.
     */
    static final int DOWNLOAD_STATE_INSTALLING = 4;

    /**
     * Token used for handler callbacks to check download progress.
     */
    static final String HANDLER_TOKEN_CHECK_PROGRESS = SERVICE_NAME + ".handler_token_check_progress";

    /**
     * How often to check download progress in millis.
     */
    static final long CHECK_PROGRESS_TIME_INTERVAL_IN_MILLIS = 1000;

    /**
     * 1 MiB in bytes (this not a megabyte).
     */
    static final float MEBIBYTE_IN_BYTES = 1024 * 1024;

    /**
     * Time to wait for installing optional updates if user postponed, in millis.
     */
    static final long POSTPONE_TIME_THRESHOLD = 24 * 60 * 60 * 1000;

    /**
     * Notification channel identifier.
     */
    static final String NOTIFICATION_CHANNEL_ID = "appcenter.distribute";

    /**
     * Previous name of preferences, used for fail-over logic for missing token/distribution group.
     */
    static final String PREFERENCES_NAME_MOBILE_CENTER = "MobileCenter";

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Preference key to store the current/last download identifier (we keep download until a next
     * one is scheduled as the file can be opened from device downloads U.I.).
     */
    static final String PREFERENCE_KEY_DOWNLOAD_ID = PREFERENCE_PREFIX + "download_id";

    /**
     * Preference key to store the SDK state related to {@link #PREFERENCE_KEY_DOWNLOAD_ID} when not null.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_STATE = PREFERENCE_PREFIX + "download_state";

    /**
     * Preference key for request identifier to validate deep link intent.
     */
    static final String PREFERENCE_KEY_REQUEST_ID = PREFERENCE_PREFIX + EXTRA_REQUEST_ID;

    /**
     * Preference key to store token.
     */
    static final String PREFERENCE_KEY_UPDATE_TOKEN = PREFERENCE_PREFIX + EXTRA_UPDATE_TOKEN;

    /**
     * Preference key to store latest downloaded release hash.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH = PREFERENCE_PREFIX + "downloaded_release_hash";

    /**
     * Preference key to store latest downloaded release id.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_RELEASE_ID = PREFERENCE_PREFIX + "downloaded_release_id";

    /**
     * Preference key to store distribution group identifier of latest downloaded release.
     */
    static final String PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID = PREFERENCE_PREFIX + "downloaded_distribution_group_id";

    /**
     * Preference key to store distribution group identifier.
     */
    static final String PREFERENCE_KEY_DISTRIBUTION_GROUP_ID = PREFERENCE_PREFIX + EXTRA_DISTRIBUTION_GROUP_ID;

    /**
     * Preference key to store release details.
     */
    static final String PREFERENCE_KEY_RELEASE_DETAILS = PREFERENCE_PREFIX + "release_details";

    /**
     * Preference key to store download start time. Used to avoid showing install U.I. of a completed
     * download if we already updated (the download workflow can work across process restarts).
     * <p>
     * We can't use {@link DownloadManager#COLUMN_LAST_MODIFIED_TIMESTAMP} as we could have a corner case
     * where we install upgrade from email or another mean while waiting download triggered by SDK.
     * So the time we store as a reference needs to be before download time.
     */
    static final String PREFERENCE_KEY_DOWNLOAD_TIME = PREFERENCE_PREFIX + "download_time";

    /**
     * Preference key to hold the time when user chose "ask me in a day".
     */
    static final String PREFERENCE_KEY_POSTPONE_TIME = PREFERENCE_PREFIX + "postpone_time";

    /**
     * Preference key for update setup failure package hash.
     */
    static final String PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY = PREFERENCE_PREFIX + "update_setup_failed_package_hash";

    /**
     * Preference key for update setup failure error message.
     */
    static final String PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY = PREFERENCE_PREFIX + "update_setup_failed_message";

    /**
     * Preference key for tester app update setup failure error message.
     */
    static final String PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY = PREFERENCE_PREFIX + "tester_app_update_setup_failed_message";

    @VisibleForTesting
    DistributeConstants() {

        /* Hide constructor as it's just a constant class. */
    }
}
