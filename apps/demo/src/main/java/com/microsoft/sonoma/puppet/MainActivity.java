package com.microsoft.sonoma.puppet;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.microsoft.sonoma.analytics.Analytics;
import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.utils.UUIDUtils;
import com.microsoft.sonoma.errors.ErrorReporting;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Sonoma.setLogLevel(Log.VERBOSE);
        Sonoma.start(getApplication(), UUIDUtils.randomUUID().toString(), Analytics.class, ErrorReporting.class);
    }
}
