package com.microsoft.appcenter.push;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessagingService;

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
        FirebaseAnalytics instance = FirebaseAnalytics.getInstance(context);
        instance.setAnalyticsCollectionEnabled(enabled);
    }

    static void setFirebaseMessagingServiceEnabled(@NonNull Context context, boolean enabled) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName firebaseComponentName = new ComponentName(context, FirebaseMessagingService.class.getName());
        packageManager.setComponentEnabledSetting(firebaseComponentName,
                enabled ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Nullable
    static String getToken() throws FirebaseUnavailableException {
        return getFirebaseInstanceId().getToken();
    }

    @NonNull
    private static FirebaseInstanceId getFirebaseInstanceId() throws FirebaseUnavailableException {
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
