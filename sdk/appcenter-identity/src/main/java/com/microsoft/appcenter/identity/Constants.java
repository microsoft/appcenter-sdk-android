package com.microsoft.appcenter.identity;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Identity module.
 */
final class Constants {

    /**
     * Name of the service.
     */
    static final String SERVICE_NAME = "Identity";

    /**
     * TAG used in logging for Identity.
     */
    static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the identity group.
     */
    static final String IDENTITY_GROUP = "group_identity";

    /**
     * URL pattern to get configuration file. Variable is appSecret.
     */
    static final String CONFIG_URL = "https://mobilecentersdkdev.blob.core.windows.net/identity/%s.json";

    /**
     * File path to store cached configuration in application files.
     */
    static final String FILE_PATH = "appcenter/identity/config.json";

    /**
     * ETag preference storage key.
     */
    static final String PREFERENCE_E_TAG_KEY = SERVICE_NAME + ".configFileETag";

    /**
     * ETag response header.
     */
    static final String HEADER_E_TAG = "ETag";

    /**
     * ETag request header.
     */
    static final String HEADER_IF_NONE_MATCH = "If-None-Match";

    /**
     * JSON configuration key for clientId.
     */
    static final String CLIENT_ID = "client_id";

    /**
     * JSON configuration key for authorities array.
     */
    static final String AUTHORITIES = "authorities";

    /**
     * JSON configuration key for authority default boolean.
     */
    static final String AUTHORITY_DEFAULT = "default";

    /**
     * JSON configuration key for authority within {@link #AUTHORITIES} array.
     */
    static final String AUTHORITY_TYPE = "type";

    /**
     * JSON configuration value for b2c authority within {@link #AUTHORITIES} array.
     */
    static final String AUTHORITY_TYPE_B2C = "B2C";

    /**
     * JSON configuration key for authority url within {@link #AUTHORITIES} array.
     */
    static final String AUTHORITY_URL = "authority_url";

    /**
     * JSON configuration key for identity scope.
     */
    static final String IDENTITY_SCOPE = "identity_scope";
}
