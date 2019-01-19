package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.AuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import java.util.ArrayList;
import java.util.List;

public class AuthenticationProviderActivity extends AppCompatActivity {

    final static String SCOPES[] = {"https://appcenterIdentitySpike.onmicrosoft.com/identity/user_impersonation"};

    static PublicClientApplication sAuthApp;

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
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(featureList));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
        if (sAuthApp == null) {
            sAuthApp = new PublicClientApplication(
                    getApplicationContext(),
                    R.raw.auth);
        }
        sAuthApp.acquireToken(this, SCOPES, getAuthInteractiveCallback());
    }

    private AuthenticationCallback getAuthInteractiveCallback() {
        return new AuthenticationCallback() {

            @Override
            public void onSuccess(AuthenticationResult authenticationResult) {
                Log.i(MainActivity.LOG_TAG, "MSAL success, token=" + authenticationResult.getIdToken());
            }

            @Override
            public void onError(MsalException exception) {
                Log.e(MainActivity.LOG_TAG, "MSAL login failed", exception);
            }

            @Override
            public void onCancel() {
                Log.i(MainActivity.LOG_TAG, "MSAL cancelled");
            }
        };
    }

    private void startMSALoginActivity(AuthenticationProvider.Type type) {
        Intent intent = new Intent(getApplication(), MSALoginActivity.class);
        intent.putExtra(AuthenticationProvider.Type.class.getName(), type);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        sAuthApp.handleInteractiveRequestRedirect(requestCode, resultCode, data);
    }
}
