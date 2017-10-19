package com.microsoft.azure.mobile.push;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirebaseUtils {

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

    public static String getToken() throws IllegalStateException {
        if (!isFirebaseAvailable()) {
            return null;
        }
        String warningMessage = "Something went wrong when getting Firebase token";
        try {
            Object firebaseInstanceId = mFirebaseClass.getMethod("getInstance").invoke(null);
            if (firebaseInstanceId == null) {
                MobileCenterLog.warn(Push.getInstance().getLoggerTag(), warningMessage);
                return null;
            }
            Method getTokenMethod = firebaseInstanceId.getClass().getMethod("getToken");
            if (getTokenMethod == null) {
                MobileCenterLog.warn(Push.getInstance().getLoggerTag(), warningMessage);
                return null;
            }
            return (String)getTokenMethod.invoke(firebaseInstanceId);
        } catch (IllegalAccessException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(), warningMessage);
        } catch (InvocationTargetException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(), warningMessage);
        } catch (NoSuchMethodException e) {
            MobileCenterLog.warn(Push.getInstance().getLoggerTag(), warningMessage);
        }
        return null;
    }
}
