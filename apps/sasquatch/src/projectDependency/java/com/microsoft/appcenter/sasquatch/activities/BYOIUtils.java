/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.auth0.android.Auth0;
import com.auth0.android.authentication.AuthenticationAPIClient;
import com.auth0.android.authentication.storage.CredentialsManager;
import com.auth0.android.authentication.storage.CredentialsManagerException;
import com.auth0.android.authentication.storage.SharedPreferencesStorage;
import com.auth0.android.callback.BaseCallback;
import com.auth0.android.result.Credentials;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.AuthTokenCallback;
import com.microsoft.appcenter.AuthTokenListener;

/**
 * TODO During release, move this class to main folder and delete jCenter empty version.
 */
class BYOIUtils {

    private static Auth0 sAuth0;

    private static CredentialsManager sAuth0CredentialsManager;

    static Auth0 getAuth0Client(Context context) {
        if (sAuth0 == null) {
            sAuth0 = new Auth0(context);
            sAuth0.setOIDCConformant(true);
        }
        return sAuth0;
    }

    static CredentialsManager getAuth0CredentialsManager(Context context) {
        if (sAuth0CredentialsManager == null) {
            AuthenticationAPIClient apiClient = new AuthenticationAPIClient(getAuth0Client(context));
            sAuth0CredentialsManager = new CredentialsManager(apiClient, new SharedPreferencesStorage(context));
        }
        return sAuth0CredentialsManager;
    }

    static void setAuthTokenListener(final Context context) {
        switch (MainActivity.sAuthType) {

            case FIREBASE:
                AppCenter.setAuthTokenListener(new AuthTokenListener() {

                    @Override
                    public void acquireAuthToken(final AuthTokenCallback callback) {
                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user == null) {
                            Log.e(MainActivity.LOG_TAG, "Failed to refresh Firebase token as user is signed out");
                            callback.onAuthTokenResult(null);
                        } else {
                            user.getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {

                                @Override
                                public void onSuccess(GetTokenResult getTokenResult) {
                                    Log.i(MainActivity.LOG_TAG, "Refreshed Firebase token");
                                    callback.onAuthTokenResult(getTokenResult.getToken());
                                }
                            }).addOnFailureListener(new OnFailureListener() {

                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(MainActivity.LOG_TAG, "Failed to refresh Firebase token", e);
                                    callback.onAuthTokenResult(null);
                                }
                            });
                        }
                    }
                });
                break;

            case AUTH0:
                AppCenter.setAuthTokenListener(new AuthTokenListener() {

                    @Override
                    public void acquireAuthToken(final AuthTokenCallback callback) {
                        getAuth0CredentialsManager(context).getCredentials(new BaseCallback<Credentials, CredentialsManagerException>() {

                            @Override
                            public void onSuccess(Credentials payload) {
                                Log.i(MainActivity.LOG_TAG, "Refreshed Auth0 token");
                                callback.onAuthTokenResult(payload.getIdToken());
                            }

                            @Override
                            public void onFailure(CredentialsManagerException error) {
                                Log.e(MainActivity.LOG_TAG, "Failed to refresh Auth0 token", error);
                                callback.onAuthTokenResult(null);
                            }
                        });
                    }
                });
                break;
        }
    }

    static void setAuthToken(String authToken) {
        AppCenter.setAuthToken(authToken);
    }
}
