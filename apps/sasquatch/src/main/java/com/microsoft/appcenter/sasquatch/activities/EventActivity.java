package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;

import java.util.Map;

public class EventActivity extends LogActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCenter.startFromLibrary(getApplication(), Analytics.class);
    }

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        String target = getTransmissionTarget();
        if (target == null) {
            Analytics.trackEvent(name, properties);
        } else {
            Analytics.getTransmissionTarget(target).trackEvent(name, properties);
        }
    }
}
