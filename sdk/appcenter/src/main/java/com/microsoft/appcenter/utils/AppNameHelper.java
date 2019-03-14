/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;

public class AppNameHelper {
    public static String getAppName(Context context) {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int labelRes = applicationInfo.labelRes;
        if (labelRes == 0) {
            return String.valueOf(applicationInfo.nonLocalizedLabel);
        } else {
            return context.getString(labelRes);
        }
    }
}
