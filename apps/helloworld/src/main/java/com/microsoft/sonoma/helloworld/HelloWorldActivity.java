package com.microsoft.sonoma.helloworld;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.crashes.Crashes;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;


public class HelloWorldActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MobileCenter.setLogLevel(Log.VERBOSE);
        MobileCenter.start(getApplication(), "45d1d9f6-2492-4e68-bd44-7190351eb5f3", Analytics.class, Crashes.class);
    }

    @SuppressWarnings({"ConstantConditions", "ConstantIfStatement"})
    @Override
    protected void onDestroy() {

        /* Trigger a super not called exception for testing crash reporting. */
        if (false)
            super.onDestroy();
    }
}
