package com.microsoft.azure.mobile.updates;

import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.MobileCenter;

final class UpdateConstants {

    /**
     * Update service name.
     */
    static final String SERVICE_NAME = "Updates";

    /**
     * Used for deep link intent from browser, string field for update token.
     */
    static final String EXTRA_UPDATE_TOKEN = "update_token";

    /**
     * Used for deep link intent from browser, string field for request identifier.
     */
    static final String EXTRA_REQUEST_ID = "request_id";

    /**
     * Log tag for this service.
     */
    static final String LOG_TAG = MobileCenter.LOG_TAG + SERVICE_NAME;

    @VisibleForTesting
    UpdateConstants() {
    }
}
