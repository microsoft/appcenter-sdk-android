package com.microsoft.azure.mobile.push;

import android.support.annotation.VisibleForTesting;

import com.google.firebase.iid.FirebaseInstanceId;

/**
 * Utilities to manipulate Firebase Push SDK.
 */
class FirebaseUtils {

    @VisibleForTesting
    FirebaseUtils() {
    }

    /**
     * Check if Firebase Push SDK is available in this application.
     *
     * @return true if Firebase Push SDK initialized, false otherwise.
     */
    static boolean isFirebaseAvailable() {
        try {
            FirebaseInstanceId.getInstance();
            return true;
        } catch (NoClassDefFoundError | IllegalStateException e) {
            return false;
        }
    }
}
