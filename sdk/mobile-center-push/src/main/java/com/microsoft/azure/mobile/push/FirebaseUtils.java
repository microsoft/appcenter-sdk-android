package com.microsoft.azure.mobile.push;
import java.lang.reflect.InvocationTargetException;

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
            return (String) mFirebaseClass.getMethod("getToken").invoke(firebaseInstanceId);
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
