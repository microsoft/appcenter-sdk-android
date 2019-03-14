/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch;

import android.app.Application;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;

public class SasquatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCenter.setLogLevel(Log.VERBOSE);
    }
}
