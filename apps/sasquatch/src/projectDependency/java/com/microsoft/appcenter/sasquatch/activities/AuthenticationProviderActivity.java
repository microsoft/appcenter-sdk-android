/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.microsoft.appcenter.analytics.AuthenticationProvider;
import com.microsoft.appcenter.auth.Auth;
import com.microsoft.appcenter.auth.SignInResult;
import com.microsoft.appcenter.auth.UserInformation;
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

    private static UserInformation sUserInformation;

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
        mFeatureList.add(new TestFeatures.TestFeature(R.string.b2c_sign_in_title, R.string.b2c_sign_in_description, new View.OnClickListener() {

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
                            sUserInformation = signInResult.getUserInformation();
                            loadAuthStatus(false);
                            String accountId = sUserInformation.getAccountId();
                            SharedPreferences.Editor edit = MainActivity.sSharedPreferences.edit();
                            edit.putString("accountId", accountId);
                            edit.apply();
                            Log.i(LOG_TAG, "Auth.signIn succeeded, accountId=" + accountId);
                        } catch (Exception e) {
                            sUserInformation = null;
                            loadAuthStatus(false);
                            Log.e(LOG_TAG, "Auth.signIn failed", e);
                        }
                    }
                });
            }
        }));
        mFeatureList.add(new TestFeatures.TestFeature(R.string.b2c_sign_out_title, R.string.b2c_sign_out_description, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    Auth.signOut();
                    sUserInformation = null;
                    loadAuthStatus(false);
                    SharedPreferences.Editor edit = MainActivity.sSharedPreferences.edit();
                    edit.putString(ACCOUNT_ID, null);
                    edit.apply();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Auth.signOut failed", e);
                }
            }
        }));
        mListView = findViewById(R.id.list);
        loadAuthStatus(sUserInformation == null);
        mListView.setOnItemClickListener(TestFeatures.getOnItemClickListener());
    }

    private static boolean isAuthenticated() {
        if (sUserInformation == null) {
            return false;
        }
        return sUserInformation.getAccessToken() != null;
    }

    private void loadAuthStatus(boolean loadDefaultStatus) {
        if (mAuthInfoTestFeature != null) {
            mFeatureList.remove(mAuthInfoTestFeature);
        }
        mAuthInfoTestFeature = getAuthenticationDefaultTestFeature();
        if (!loadDefaultStatus) {
            mAuthInfoTestFeature = isAuthenticated() ? getAuthenticatedTestFeature() : getNotAuthenticatedTestFeature();
        }
        mFeatureList.add(mAuthInfoTestFeature);
        mListView.setAdapter(new TestFeaturesListAdapter(mFeatureList));
    }

    private TestFeatures.TestFeature getAuthenticationDefaultTestFeature() {
        return getAuthenticationTestFeature(R.string.b2c_authentication_status_description);
    }

    private TestFeatures.TestFeature getAuthenticatedTestFeature() {
        return getAuthenticationTestFeature(R.string.b2c_authentication_status_authenticated);
    }

    private TestFeatures.TestFeature getNotAuthenticatedTestFeature() {
        return getAuthenticationTestFeature(R.string.b2c_authentication_status_not_authenticated);
    }

    private TestFeatures.TestFeature getAuthenticationTestFeature(int valueStringId) {
        return new TestFeatures.TestFeature(R.string.b2c_authentication_status_title, valueStringId, new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isAuthenticated()) {
                    startUserInfoActivity(sUserInformation);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(AuthenticationProviderActivity.this);
                    builder.setTitle(R.string.b2c_authentication_status_dialog_unavailable_title)
                           .setMessage(R.string.b2c_authentication_status_dialog_unavailable_description)
                           .setPositiveButton(R.string.alert_ok, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                    builder.create().show();
                }
            }
        });
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
