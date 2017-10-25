package com.microsoft.appcenter.sasquatch.activities;

import com.microsoft.appcenter.analytics.AnalyticsPrivateHelper;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        AnalyticsPrivateHelper.trackPage(name, properties);
    }
}
