package com.microsoft.azure.mobile.sasquatch.activities;

import android.widget.Toast;

import com.microsoft.azure.mobile.analytics.Analytics;
import com.microsoft.azure.mobile.sasquatch.R;

import java.util.Map;

public class EventActivity extends LogActivity {

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        Analytics.trackEvent(name, properties);
        Toast.makeText(getBaseContext(), R.string.description_event, Toast.LENGTH_SHORT).show();
    }
}
