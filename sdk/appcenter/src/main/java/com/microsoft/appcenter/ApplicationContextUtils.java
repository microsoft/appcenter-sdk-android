/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.UserManager;

/**
 * Helper application context utility class to deal with direct boot where regular storage is not available.
 */
class ApplicationContextUtils {

    /**
     * Get application context with device-protected storage if needed.
     * Note that this method might return a new instance of device-protected storage context object each call.
     * See {@link Context#createDeviceProtectedStorageContext()}.
     *
     * @param application android application.
     * @return application context with device-protected storage if needed.
     */
    static Context getApplicationContext(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserManager userManager = (UserManager) application.getSystemService(Context.USER_SERVICE);

            /*
             * On devices with direct boot, a user is unlocked only after they've entered
             * their credentials (such as a lock pattern or PIN).
             */
            if (!userManager.isUserUnlocked()) {
                return application.createDeviceProtectedStorageContext();
            }
        }
        return application;
    }

    /**
     * Indicates if the storage APIs of this Context are backed by device-protected storage.
     *
     * @param context context to check.
     * @return true if the storage APIs of this Context are backed by device-protected storage.
     */
    static boolean isDeviceProtectedStorage(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.isDeviceProtectedStorage();
        }
        return false;
    }
}
