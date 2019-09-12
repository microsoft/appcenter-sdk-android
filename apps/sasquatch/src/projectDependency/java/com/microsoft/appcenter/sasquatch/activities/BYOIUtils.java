/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.support.annotation.NonNull;
import android.util.Log;

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

    static void setAuthTokenListener() {
        if (MainActivity.sAuthType == MainActivity.AuthType.FIREBASE) {
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
        }
    }

    static void setAuthToken(String authToken) {
        AppCenter.setAuthToken(authToken);
    }
}
