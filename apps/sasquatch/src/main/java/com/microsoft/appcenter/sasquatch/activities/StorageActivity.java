/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.microsoft.appcenter.sasquatch.R;

import java.lang.reflect.InvocationTargetException;

import static com.microsoft.appcenter.sasquatch.activities.MainActivity.LOG_TAG;

class TestDocument {

    @SuppressWarnings("unused")
    String test = "ABC";
}

public class StorageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);

        /* TODO remove reflection once Storage published to jCenter. */
        try {
            Class<?> storage = Class.forName("com.microsoft.appcenter.storage.Storage");
            createDocument(storage);
            readDocument(storage);
            deleteDocument(storage);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Storage.Module call failed", e);
        }
    }

    private void deleteDocument(Class<?> storage) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        storage
                .getMethod("delete", String.class, String.class)
                .invoke(null, "test-partition", "document-id-123");
    }

    private void readDocument(Class<?> storage) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        storage
                .getMethod("read", String.class, String.class, Class.class)
                .invoke(null, "test-partition-other", "document-id-123", TestDocument.class);
    }

    private void createDocument(Class<?> storage) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        storage
                .getMethod("create", String.class, String.class, Object.class, Class.class)
                .invoke(null, "test-partition", "document-id-123", new TestDocument(), TestDocument.class);
    }
}
