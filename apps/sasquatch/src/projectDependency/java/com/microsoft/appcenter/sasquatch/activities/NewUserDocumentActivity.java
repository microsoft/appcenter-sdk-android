/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.fragments.TypedPropertyFragment;

import java.util.ArrayList;
import java.util.List;

public class NewUserDocumentActivity extends AppCompatActivity {

    private final List<TypedPropertyFragment> mProperties = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_user_document);
        addProperty();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                addProperty();
                break;
        }
        return true;
    }

    private void addProperty() {
        TypedPropertyFragment fragment = new TypedPropertyFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.list, fragment).commit();
        mProperties.add(fragment);
    }
}
