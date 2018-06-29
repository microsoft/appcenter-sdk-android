package com.microsoft.appcenter.sasquatch.activities;

import com.microsoft.appcenter.analytics.AnalyticsPrivateHelper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    int getLayoutId() {
        return R.layout.activity_page;
    }

    @Override
    void trackLog(String name, Map<String, String> properties) {
        AnalyticsPrivateHelper.trackPage(name, properties);
    }
}
