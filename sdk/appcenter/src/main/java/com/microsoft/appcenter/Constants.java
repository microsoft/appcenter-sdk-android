/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.File;

/**
 * Various constants and meta information loaded from the context.
 **/
public class Constants {

    /**
     * Application secret HTTP Header.
     */
    public static final String APP_SECRET = "App-Secret";

    /**
     * Number of metrics queue items which will trigger synchronization.
     */
    static final int DEFAULT_TRIGGER_COUNT = 50;

    /**
     * Maximum number of requests being sent for the group.
     */
    static final int DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    /**
     * Common schema prefix separator used in various field values.
     */
    public static final String COMMON_SCHEMA_PREFIX_SEPARATOR = ":";

    /**
     * Authorization HTTP Header.
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Auth token format for Authorization header.
     */
    public static final String AUTH_TOKEN_FORMAT = "Bearer %s";

    /**
     * Database name.
     */
    public static final String DATABASE = "com.microsoft.appcenter.documents";

    /**
     * Readonly table name.
     */
    public static final String READONLY_TABLE = "app_documents";

    /**
     * User-specific table name format.
     */
    public static final String USER_TABLE_FORMAT = "user_%s_documents";

    /**
     * Path where crash logs and temporary files are stored.
     */
    public static String FILES_PATH = null;

    /**
     * Constant used to add NDK identity to native crash logs.
     */
    public static final String WRAPPER_SDK_NAME_NDK = "appcenter.ndk";

    /**
     * Flag indicates whether the host application is debuggable or not.
     */
    public static boolean APPLICATION_DEBUGGABLE = false;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    public static final int DEFAULT_TRIGGER_INTERVAL = 3 * 1000;

    /**
     * Initializes constants from the given context. The context is used to set
     * the package name, version code, and the files path.
     *
     * @param context The context to use. Usually your Activity object.
     */
    public static void loadFromContext(Context context) {
        loadFilesPath(context);
        setDebuggableFlag(context);
    }

    /**
     * Helper method to set the files path. If an exception occurs, the files
     * path will be null!
     *
     * @param context The context to use. Usually your Activity object.
     */
    private static void loadFilesPath(Context context) {
        if (context != null) {
            try {

                /*
                 * The file shouldn't be null, but apparently it still can happen, see
                 * http://code.google.com/p/android/issues/detail?id=8886, Fixed in API 19.
                 */
                File file = context.getFilesDir();
                Constants.FILES_PATH = file.getAbsolutePath();
            } catch (Exception e) {
                AppCenterLog.error(AppCenter.LOG_TAG, "Exception thrown when accessing the application filesystem", e);
            }
        }
    }

    /**
     * Helper method to determine whether the host application is debuggable or not.
     *
     * @param context The context to use. Usually your Activity object.
     */
    private static void setDebuggableFlag(Context context) {
        if (context != null && context.getApplicationInfo() != null) {
            APPLICATION_DEBUGGABLE = (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) > 0;
        }
    }
}
