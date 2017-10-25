package com.microsoft.appcenter.sasquatch;

import android.app.Application;
import android.util.Log;

import com.microsoft.appcenter.MobileCenter;

public class SasquatchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        MobileCenter.setLogLevel(Log.VERBOSE);
    }
}
