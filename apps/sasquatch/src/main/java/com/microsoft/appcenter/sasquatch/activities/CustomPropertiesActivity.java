/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.CustomProperties;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.CustomPropertyFragment;

import java.util.ArrayList;
import java.util.List;

public class CustomPropertiesActivity extends AppCompatActivity {

    private final List<CustomPropertyFragment> mProperties = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_properties);
        addProperty();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            addProperty();
        }
        return true;
    }

    private void addProperty() {
        CustomPropertyFragment fragment = new CustomPropertyFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }

    public void send(@SuppressWarnings("UnusedParameters") View view) {
        CustomProperties customProperties = new CustomProperties();
        for (CustomPropertyFragment property : mProperties) {
            property.set(customProperties);
        }
        AppCenter.setCustomProperties(customProperties);
    }
}
