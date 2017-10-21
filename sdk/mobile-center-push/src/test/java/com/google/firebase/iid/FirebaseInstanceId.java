package com.google.firebase.iid;

/**
 * Stub interface to test FirebaseUtils.
 */
public abstract class FirebaseInstanceId {

    public static FirebaseInstanceId getInstance() {
        return null;
    }

    public abstract String getToken();
}
