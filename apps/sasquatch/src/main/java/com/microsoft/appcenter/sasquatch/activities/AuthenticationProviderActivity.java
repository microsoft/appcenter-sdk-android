/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationProviderActivity extends AppCompatActivity {

    private boolean mUserLeaving;

    private TestFeatures.TestFeature mAuthInfoTestFeature;

    private List<TestFeatures.TestFeatureModel> mFeatureList;

    private ListView mListView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_provider_list);

        /* Populate UI. */
        mFeatureList = new ArrayList<>();
        mFeatureList.add(new TestFeatures.TestFeatureTitle(R.string.msa_title));
        mFeatureList.add(new TestFeatures.TestFeature(R.string.msa_compact_title, R.string.msa_compact_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_COMPACT);
            }
        }));
        mFeatureList.add(new TestFeatures.TestFeature(R.string.msa_delegate_title, R.string.msa_delegate_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_DELEGATE);
            }
        }));
        mListView = findViewById(R.id.list);
        mListView.setAdapter(new TestFeaturesListAdapter(mFeatureList));
        mListView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    private void startMSALoginActivity(AuthenticationProvider.Type type) {
        Intent intent = new Intent(getApplication(), MSALoginActivity.class);
        intent.putExtra(AuthenticationProvider.Type.class.getName(), type);
        startActivity(intent);
    }

    @Override
    protected void onUserLeaveHint() {
        mUserLeaving = true;
    }

    @Override
    protected void onRestart() {

        /* When coming back from browser, finish this intermediate menu screen too. */
        super.onRestart();
        if (mUserLeaving) {
            finish();
        }
    }
}
