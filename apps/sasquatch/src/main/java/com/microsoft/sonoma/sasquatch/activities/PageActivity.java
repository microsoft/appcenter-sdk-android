package com.microsoft.sonoma.sasquatch.activities;

import com.microsoft.sonoma.analytics.Analytics;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        Analytics.trackPage(name, properties);
    }
}
