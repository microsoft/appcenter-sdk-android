package com.microsoft.azure.mobile.push;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirebaseUtils {

    private static Class mFirebaseClass = null;

    static {
        try {
            mFirebaseClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
        } catch (ClassNotFoundException e) {
            /* Do nothing if Firebase class is not found. */
        }
    }

    public static boolean isFirebaseAvailable() {
        return mFirebaseClass != null;
    }

    /* TODO log messages? */
    public static String getToken() throws IllegalStateException {
        if (!isFirebaseAvailable()) {
            return null;
        }
        try {
            Object firebaseInstanceId = mFirebaseClass.getMethod("getInstance").invoke(null);
            if (firebaseInstanceId == null) {
                return null;
            }
            Method getTokenMethod = firebaseInstanceId.getClass().getMethod("getToken");
            if (getTokenMethod == null) {
                return null;
            }
            return (String)getTokenMethod.invoke(firebaseInstanceId);
        } catch (IllegalAccessException e) {
            /* Do nothing if something went wrong; null will be returned. */
        } catch (InvocationTargetException e) {
            /* Do nothing if something went wrong; null will be returned. */
        } catch (NoSuchMethodException e) {
            /* Do nothing if something went wrong; null will be returned. */
        }
        return null;
    }
}
