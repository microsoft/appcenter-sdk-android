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
    public static final String TOKEN_RESULT_SUCCEED = "succeed";

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


}
