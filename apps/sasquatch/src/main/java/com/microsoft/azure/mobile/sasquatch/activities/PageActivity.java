package com.microsoft.azure.mobile.sasquatch.activities;

import com.microsoft.azure.mobile.analytics.AnalyticsPrivateHelper;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        AnalyticsPrivateHelper.trackPage(name, properties);
    }
}
