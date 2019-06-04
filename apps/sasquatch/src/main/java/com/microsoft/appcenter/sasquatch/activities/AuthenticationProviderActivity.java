/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.UserInformation;
import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.auth.Auth;
import com.microsoft.appcenter.auth.SignInResult;
import com.microsoft.appcenter.sasquatch.R;
import com.microsoft.appcenter.sasquatch.features.TestFeatures;
import com.microsoft.appcenter.sasquatch.features.TestFeaturesListAdapter;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.sasquatch.SasquatchConstants.ACCOUNT_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ACCESS_TOKEN;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ID;
import static com.microsoft.appcenter.sasquatch.SasquatchConstants.USER_INFORMATION_ID_TOKEN;
import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

public class AuthenticationProviderActivity extends AppCompatActivity {

    private boolean mUserLeaving;

    private UserInformation mUserInformation;

    private TestFeatures.TestFeature mAuthInfoTestFeature;

    private List<TestFeatures.TestFeatureModel> mFeatureList;

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
        featureList.add(new TestFeatures.TestFeature(R.string.b2c_sign_in_title, R.string.b2c_sign_in_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Auth.signIn().thenAccept(new AppCenterConsumer<SignInResult>() {

                    @Override
                    public void accept(SignInResult signInResult) {
                        try {
                            Exception exception = signInResult.getException();
                            if (exception != null) {
                                throw exception;
                            }
                            mUserInformation = signInResult.getUserInformation();
                            loadAuthStatus(false);
                            String accountId = mUserInformation.getAccountId();
                            SharedPreferences.Editor edit = MainActivity.sSharedPreferences.edit();
                            edit.putString("accountId", accountId);
                            edit.apply();
                            Log.i(LOG_TAG, "Auth.signIn succeeded, accountId=" + accountId);
                        } catch (Exception e) {
                            mUserInformation = null;
                            loadAuthStatus(false);
                            Log.e(LOG_TAG, "Auth.signIn failed", e);
                        }
                    }
                });
            }
        }));
        featureList.add(new TestFeatures.TestFeature(R.string.b2c_sign_out_title, R.string.b2c_sign_out_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Auth.signOut();
                    mUserInformation = null;
                    loadAuthStatus(false);
                    SharedPreferences.Editor edit = MainActivity.sSharedPreferences.edit();
                    edit.putString(ACCOUNT_ID, null);
                    edit.apply();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Auth.signOut failed", e);
                }
            }
        }));
        mFeatureList = featureList;
        loadAuthStatus(true);
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(new TestFeaturesListAdapter(featureList));
        listView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    private void loadAuthStatus(boolean _default) {
        if (mAuthInfoTestFeature != null) {
            mFeatureList.remove(mAuthInfoTestFeature);
        }
        mAuthInfoTestFeature = getDefaultAuthenticationTestFeature();
        if (!_default) {
            if (isAuthenticated()) {
                mAuthInfoTestFeature = getAuthenticatedTestFeature();
            } else {
                mAuthInfoTestFeature = getNotAuthenticatedTestFeature();
            }
        }
        mFeatureList.add(mAuthInfoTestFeature);
    }

    private TestFeatures.TestFeature getDefaultAuthenticationTestFeature() {
        return new TestFeatures.TestFeature(R.string.b2c_authentication_status_title, R.string.b2c_authentication_status_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isAuthenticated()) {
                    startUserInfoActivity(mUserInformation);
                }
            }
        });
    }

    private TestFeatures.TestFeature getAuthenticatedTestFeature() {
        return new TestFeatures.TestFeature(R.string.b2c_authentication_status_title, R.string.b2c_authentication_status_authenticated, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isAuthenticated()) {
                    startUserInfoActivity(mUserInformation);
                }
            }
        });
    }

    private TestFeatures.TestFeature getNotAuthenticatedTestFeature() {
        return new TestFeatures.TestFeature(R.string.b2c_authentication_status_title, R.string.b2c_authentication_status_not_authenticated, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isAuthenticated()) {
                    startUserInfoActivity(mUserInformation);
                }
            }
        });
    }

    private boolean isAuthenticated() {
        return mUserInformation != null && mUserInformation.getAccessToken() != null;
    }

    private void startMSALoginActivity(AuthenticationProvider.Type type) {
        Intent intent = new Intent(getApplication(), MSALoginActivity.class);
        intent.putExtra(AuthenticationProvider.Type.class.getName(), type);
        startActivity(intent);
    }

    private void startUserInfoActivity(UserInformation userInformation) {
        Intent intent = new Intent(getApplication(), UserInformationActivity.class);
        intent.putExtra(USER_INFORMATION_ID, userInformation.getAccountId());
        intent.putExtra(USER_INFORMATION_ID_TOKEN, userInformation.getIdToken());
        intent.putExtra(USER_INFORMATION_ACCESS_TOKEN, userInformation.getAccessToken());
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
