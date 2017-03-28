package com.microsoft.azure.mobile.sasquatch;

import android.app.Application;
import android.util.Log;

import com.microsoft.azure.mobile.MobileCenter;

public class SasquatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCenter.setLogLevel(Log.VERBOSE);
    }
}
