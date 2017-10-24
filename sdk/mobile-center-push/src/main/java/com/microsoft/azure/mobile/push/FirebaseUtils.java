package com.microsoft.azure.mobile.push;

import android.support.annotation.VisibleForTesting;

import java.lang.reflect.Method;

/**
 * Utilities to manipulate Firebase Push SDK via reflection.
 */
class FirebaseUtils {

    @VisibleForTesting
    FirebaseUtils() {
    }

    /**
     * Get firebase instance.
     */
    private static Object getFirebaseIdInstance() throws Exception {
        return Class.forName("com.google.firebase.iid.FirebaseInstanceId").getMethod("getInstance").invoke(null);
    }

    /**
     * Check if Firebase Push SDK is available in this application.
     *
     * @return true if Firebase Push SDK initialized, false otherwise.
     */
    static boolean isFirebaseAvailable() {
        try {
            getFirebaseIdInstance();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the registration token via Firebase SDK.
     *
     * @return the Firebase token or null if registration is initiated but pending.
     * @throws Exception if any error occurs like Firebase SDK not available or not initialized.
     */
    static String getToken() throws Exception {
        Object firebaseIdInstance = getFirebaseIdInstance();
        Method getTokenMethod = firebaseIdInstance.getClass().getMethod("getToken");
        return (String) getTokenMethod.invoke(firebaseIdInstance);
    }
}
