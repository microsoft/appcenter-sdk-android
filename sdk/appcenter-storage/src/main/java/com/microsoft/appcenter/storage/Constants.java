package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.utils.AppCenterLog;

/**
 * Constants for Storage module.
 */
public final class Constants {

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
     * User partition
     * An authenticated user can read/write documents in this partition
     */
    public static String USER = "user-{%s}";

    /**
     * Readonly partition
     * Everyone can read documents in this partition
     * Writes is not allowed via the SDK
     */
    public static String READONLY = "readonly";


    /**
     * Handle API call failure.
     */
    public static synchronized void handleApiCallFailure(Exception e) {
        AppCenterLog.error(LOG_TAG, "Failed to call App Center APIs", e);
        if (!HttpUtils.isRecoverableError(e)) {
            if (e instanceof HttpException) {
                HttpException httpException = (HttpException) e;
                AppCenterLog.error(LOG_TAG, "Exception", httpException);
            }
        }
    }
    /**
     * Cosmosdb token cache file
     */
    static final String TOKEN_CACHE_FILE = "token_cache";

    static final String PARTITION_NAMES = "partition_names";

    static final String UN_AUTHENTICATED = "Unauthenticated";

}
