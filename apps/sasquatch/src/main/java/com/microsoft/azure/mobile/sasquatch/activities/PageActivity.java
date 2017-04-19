package com.microsoft.azure.mobile.sasquatch.activities;

import android.widget.Toast;

import com.microsoft.azure.mobile.analytics.AnalyticsPrivateHelper;
import com.microsoft.azure.mobile.sasquatch.R;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        AnalyticsPrivateHelper.trackPage(name, properties);
        Toast.makeText(getBaseContext(), R.string.description_page, Toast.LENGTH_SHORT).show();
    }
}
