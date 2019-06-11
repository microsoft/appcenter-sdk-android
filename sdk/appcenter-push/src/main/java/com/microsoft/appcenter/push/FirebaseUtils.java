/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.push;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.microsoft.appcenter.utils.AppCenterLog;

import static com.microsoft.appcenter.push.Push.LOG_TAG;

/**
 * Utilities to manipulate Firebase Push SDK.
 */
class FirebaseUtils {

    @VisibleForTesting
    FirebaseUtils() {
    }

    /*
     * Wrap firebase unavailable exceptions for simple catching in different places of the code.
     */
    static class FirebaseUnavailableException extends Exception {

        FirebaseUnavailableException(Throwable cause) {
            super(cause);
        }

        FirebaseUnavailableException(@SuppressWarnings("SameParameterValue") String message) {
            super(message);
        }
    }

    /**
     * Check if Firebase Push SDK is available in this application.
     *
     * @return true if Firebase Push SDK initialized, false otherwise.
     */
    static boolean isFirebaseAvailable() {
        try {
            getFirebaseInstanceId();
            return true;
        } catch (FirebaseUnavailableException e) {
            return false;
        }
    }

    @SuppressWarnings("MissingPermission")
    static void setAnalyticsEnabled(@NonNull Context context, boolean enabled) {
        try {
            FirebaseAnalytics.getInstance(context).setAnalyticsCollectionEnabled(enabled);
        } catch (LinkageError e) {
            AppCenterLog.debug(LOG_TAG, "Firebase analytics not available so cannot change state.");
        }
    }

    static void setFirebaseMessagingServiceEnabled(@NonNull Context context, boolean enabled) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName firebaseComponentName = new ComponentName(context, FirebaseMessagingService.class.getName());
        packageManager.setComponentEnabledSetting(firebaseComponentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @NonNull
    static FirebaseInstanceId getFirebaseInstanceId() throws FirebaseUnavailableException {
        try {
            FirebaseInstanceId instance = FirebaseInstanceId.getInstance();
            if (instance == null) {
                throw new FirebaseUnavailableException("null instance");
            }
            return instance;
        } catch (IllegalStateException e) {
            throw new FirebaseUnavailableException(e);
        }
    }
}
