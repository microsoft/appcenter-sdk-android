package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.view.View;

import com.microsoft.appcenter.analytics.AnalyticsPrivateHelper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Map;

public class PageActivity extends LogActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* We don't support One Collector in page logs yet. */
        findViewById(R.id.transmission_target_label).setVisibility(View.GONE);
        findViewById(R.id.transmission_target).setVisibility(View.GONE);
    }

    @Override
    protected void trackLog(String name, Map<String, String> properties) {
        AnalyticsPrivateHelper.trackPage(name, properties);
    }
}
