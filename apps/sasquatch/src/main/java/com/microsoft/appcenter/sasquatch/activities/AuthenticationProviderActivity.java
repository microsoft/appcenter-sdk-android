package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.identity.Identity;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationProviderActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth_provider_list);

        /* Populate UI. */
        List<TestFeatures.TestFeatureModel> featureList = new ArrayList<>();
        featureList.add(new TestFeatures.TestFeatureTitle(R.string.msa_title));
        featureList.add(new TestFeatures.TestFeature(R.string.msa_compact_title, R.string.msa_compact_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_COMPACT);
            }
        }));
        featureList.add(new TestFeatures.TestFeature(R.string.msa_delegate_title, R.string.msa_delegate_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startMSALoginActivity(AuthenticationProvider.Type.MSA_DELEGATE);
            }
        }));
        featureList.add(new TestFeatures.TestFeature(R.string.b2c_login_title, R.string.b2c_login_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Identity.login();
            }
        }));
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(featureList));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    private void startMSALoginActivity(AuthenticationProvider.Type type) {
        Intent intent = new Intent(getApplication(), MSALoginActivity.class);
        intent.putExtra(AuthenticationProvider.Type.class.getName(), type);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}
