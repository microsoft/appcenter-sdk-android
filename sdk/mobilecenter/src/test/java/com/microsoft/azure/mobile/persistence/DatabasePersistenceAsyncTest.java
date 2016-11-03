package com.microsoft.azure.mobile.persistence;

import org.junit.Test;

@SuppressWarnings("unused")
public class DatabasePersistenceAsyncTest {

    @Test
    public void onFailure() {
        /* Dummy test for callback that is not possible to be covered by unit test. */
        DatabasePersistenceAsync.AbstractDatabasePersistenceAsyncCallback callback = new DatabasePersistenceAsync.AbstractDatabasePersistenceAsyncCallback() {
            @Override
            public void onSuccess(Object result) {
            }
        };
        callback.onFailure(null);
    }
}
