/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.microsoft.appcenter.analytics.AnalyticsPrivateHelper;
import com.microsoft.appcenter.sasquatch.R;

import java.util.Map;

public class PageActivity extends PropertyActivity {

    private TextView mName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        @SuppressLint("InflateParams") View topView = getLayoutInflater().inflate(R.layout.activity_log_top, null);
        ((LinearLayout) findViewById(R.id.top_layout)).addView(topView);

        /* Init name field. */
        mName = findViewById(R.id.name);
    }

    @Override
    protected void send(View view) {
        String name = mName.getText().toString();
        Map<String, String> properties = readStringProperties();
        AnalyticsPrivateHelper.trackPage(name, properties);
    }

    @Override
    protected boolean isStringTypeOnly() {
        return true;
    }
}
