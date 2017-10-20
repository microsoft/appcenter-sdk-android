package com.microsoft.azure.mobile.push;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.lang.reflect.Method;


class FirebaseUtils {

    /* The Firebase Instance Id instance. Available iff remote messaging is available through
     * Firebase.
     */
    private static Object mFirebaseIdInstance = null;

    static {
        try {
            mFirebaseIdInstance = Class.forName("com.google.firebase.iid.FirebaseInstanceId")
                    .getMethod("getInstance").invoke(null);
        }
        catch (Exception e) {
            MobileCenterLog.debug(Push.getInstance().getLoggerTag(),
                    "Firebase not found; using custom logic for Push.");
        }
    }

    static boolean isFirebaseAvailable() {
        return mFirebaseIdInstance != null;
    }

    /* Caller of this method needs to try/catch Exception. */

    /**
     * Uses reflection to get the registration token via Firebase.
     *
     * @return The Firebase token
     * @throws Exception several exceptions can occur; be sure to try/catch this.
     */
     static String getToken() throws Exception {
        Method getTokenMethod = mFirebaseIdInstance.getClass().getMethod("getToken");
        return (String)getTokenMethod.invoke(mFirebaseIdInstance);
    }
}
