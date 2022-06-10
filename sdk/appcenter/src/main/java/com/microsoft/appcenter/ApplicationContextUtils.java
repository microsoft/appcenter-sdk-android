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
 * Context utility.
 */
class ApplicationContextUtils {

    /**
     * Conditions for taking a context.
     */
    static Context getApplicationContext(Application application) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            UserManager userManager = (UserManager) application.getSystemService(Context.USER_SERVICE);
            if (!userManager.isUserUnlocked()) {
                return application.createDeviceProtectedStorageContext();
            }
        }
        return application.getApplicationContext();
    }
}
