package com.microsoft.azure.mobile.push;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.lang.reflect.Method;


class FirebaseUtils {

    private static Class mFirebaseClass = null;

    static {
        try {
            mFirebaseClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
            MobileCenterLog.debug(Push.getInstance().getLoggerTag(), "Firebase found.");
        } catch (ClassNotFoundException e) {
            MobileCenterLog.debug(Push.getInstance().getLoggerTag(), "Firebase not found; using custom logic for Push.");
        }
    }

    public static boolean isFirebaseAvailable() {
        return mFirebaseClass != null;
    }


    /* Caller of this method needs to try/catch Exception. */

    /**
     * Uses reflection to get the registration token via Firebase.
     *
     * @return The Firebase token
     * @throws Exception several exceptions can occur; be sure to try/catch this.
     */
     static String getToken() throws Exception {
        Object firebaseInstanceId = mFirebaseClass.getMethod("getInstance").invoke(null);
        Method getTokenMethod = firebaseInstanceId.getClass().getMethod("getToken");
        return (String)getTokenMethod.invoke(firebaseInstanceId);
    }
}
