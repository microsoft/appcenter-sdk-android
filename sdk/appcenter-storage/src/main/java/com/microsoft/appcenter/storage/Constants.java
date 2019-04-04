/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Storage module.
 */
public final class Constants {

    public static final String TOKEN_RESULT_SUCCEED = "Succeed";

    public static final String DOCUMENT_FIELD_NAME = "document";

    public static final String DOCUMENTS_FILED_NAME = "Documents";

    public static final String PARTITION_KEY_FIELD_NAME = "PartitionKey";

    public static final String ID_FIELD_NAME = "id";

    public static final String ETAG_FIELD_NAME = "_etag";

    public static final String TIMESTAMP_FIELD_NAME = "_ts";

    public static final int PARTITION_KEY_SUFFIX_LENGTH = 37;

    /**
     * Pending operation CREATE value.
     */
    public static final String PENDING_OPERATION_CREATE_VALUE = "CREATE";

    /**
     * Pending operation REPLACE value.
     */
    public static final String PENDING_OPERATION_REPLACE_VALUE = "REPLACE";

    /**
     * Pending operation DELETE value.
     */
    public static final String PENDING_OPERATION_DELETE_VALUE = "DELETE";

    /**
     * Base URL to call token exchange service.
     */
    static final String DEFAULT_API_URL = "https://api.appcenter.ms/v0.1"; //TODO This is not the right url.

    /**
     * Cached partition names list file name.
     */
    static final String PARTITION_NAMES = "partitions";

    /**
     * Name of the service.
     */
    static final String SERVICE_NAME = "Storage";

    /**
     * TAG used in logging for Storage.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the storage group.
     */
    static final String STORAGE_GROUP = "group_storage";

    /**
     * User partition.
     * An authenticated user can read/write documents in this partition.
     */
    public static String USER = "user-{%s}";

    /**
     * Readonly partition.
     * Everyone can read documents in this partition.
     * Writes are not allowed via the SDK.
     */
    @SuppressWarnings("WeakerAccess") //TODO Suppress warning once released to jcenter.
    public static String READONLY = "readonly";

    /**
     * The continuation token header used to set continuation token.
     */
    public static String CONTINUATION_TOKEN_HEADER = "x-ms-continuation";
}
