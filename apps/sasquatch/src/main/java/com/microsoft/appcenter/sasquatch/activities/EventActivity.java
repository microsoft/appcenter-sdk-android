package com.microsoft.appcenter.sasquatch.activities;

import com.microsoft.appcenter.analytics.Analytics;

import java.util.Map;

public class EventActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        Analytics.trackEvent(name, properties);
    }
}
