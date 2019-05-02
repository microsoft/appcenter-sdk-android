/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Data module.
 */
public final class Constants {

    public static final String TOKEN_RESULT_SUCCEED = "Succeed";

    public static final String DOCUMENT_FIELD_NAME = "document";

    public static final String DOCUMENTS_FIELD_NAME = "Documents";

    public static final String PARTITION_KEY_FIELD_NAME = "PartitionKey";

    public static final String ID_FIELD_NAME = "id";

    public static final String ETAG_FIELD_NAME = "_etag";

    public static final String TIMESTAMP_FIELD_NAME = "_ts";

    static final int PARTITION_KEY_SUFFIX_LENGTH = 37;

    /**
     * Pending operation CREATE value.
     */
    static final String PENDING_OPERATION_CREATE_VALUE = "CREATE";

    /**
     * Pending operation REPLACE value.
     */
    static final String PENDING_OPERATION_REPLACE_VALUE = "REPLACE";

    /**
     * Pending operation DELETE value.
     */
    static final String PENDING_OPERATION_DELETE_VALUE = "DELETE";

    /**
     * Base URL to call token exchange service.
     */
    static final String DEFAULT_API_URL = "https://tokens.appcenter.ms/v0.1";

    /**
     * Name of the service.
     */
    static final String SERVICE_NAME = "Data";

    /**
     * Base key for stored preferences.
     */
    private static final String PREFERENCE_PREFIX = SERVICE_NAME + ".";

    /**
     * Cached partition names list file name.
     */
    static final String PREFERENCE_PARTITION_NAMES = PREFERENCE_PREFIX + "partitions";

    /**
     * Cached partition names list file name.
     */
    static final String PREFERENCE_PARTITION_PREFIX = PREFERENCE_PREFIX + "partition.";

    /**
     * TAG used in logging for Data.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the data group.
     */
    static final String DATA_GROUP = "group_data";

    /**
     * The continuation token header used to set continuation token.
     */
    public static final String CONTINUATION_TOKEN_HEADER = "x-ms-continuation";
}
