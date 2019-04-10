/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.PendingOperation;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class LocalDocumentStorageAndroidTest {

    private static final String TEST_VALUE = "Test value";

    private static final String ID = "id";

    private static final long mNow = Calendar.getInstance().getTimeInMillis();

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    private LocalDocumentStorage mLocalDocumentStorage;

    private static String mUserTableName = Utils.getUserTableName("123");

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
    }

    @Before
    public void setUp() {
        SharedPreferencesManager.initialize(sContext);
        AuthTokenContext.initialize(sContext);
        AuthTokenContext.getInstance().setAuthToken(UUIDUtils.randomUUID().toString(), UUIDUtils.randomUUID().toString(), new Date());
        mLocalDocumentStorage = new LocalDocumentStorage(sContext, mUserTableName);
    }

    @After
    public void tearDown() {
        SharedPreferencesManager.clear();
        AuthTokenContext.unsetInstance();
        sContext.deleteDatabase(com.microsoft.appcenter.Constants.DATABASE);
    }

    @Test
    public void writeReadDelete() {
        Document<String> document = new Document<>(TEST_VALUE, Constants.READONLY, ID);
        mLocalDocumentStorage.writeOnline(mUserTableName, document, new WriteOptions());
        Document<String> cachedDocument = mLocalDocumentStorage.read(mUserTableName, Constants.READONLY, ID, String.class, new ReadOptions());
        assertNotNull(cachedDocument);
        assertEquals(document.getDocument(), cachedDocument.getDocument());
        assertFalse(document.failed());
        assertFalse(document.isFromCache());
        assertTrue(cachedDocument.isFromCache());
        mLocalDocumentStorage.deleteOnline(mUserTableName, Constants.READONLY, ID);
        Document<String> deletedDocument = mLocalDocumentStorage.read(mUserTableName, Constants.READONLY, ID, String.class, new ReadOptions());
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getDocumentError());
    }

    @Test
    public void readExpiredDocument() {

        /* Write a document and mock device ttl to be already expired a few seconds ago. */
        Document<String> document = new Document<>(TEST_VALUE, Constants.READONLY, ID);
        mLocalDocumentStorage.writeOnline(mUserTableName, document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -2;
            }
        });

        /* Read with a TTL of 1 second: already expired. */
        Document<String> deletedDocument = mLocalDocumentStorage.read(mUserTableName, Constants.READONLY, ID, String.class, new ReadOptions(1));
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getDocumentError());
    }

    @Test
    public void updateLocalCopyDeletesExpiredOperation() {
        Document<String> document = new Document<>(TEST_VALUE, Constants.USER, ID);
        mLocalDocumentStorage.writeOffline(mUserTableName, document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -10;
            }
        });

        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());

        mLocalDocumentStorage.updatePendingOperation(operations.get(0));

        operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(0, operations.size());
    }

    @Test
    public void updateLocalCopyReplacesNotExpiredOperation() {
        Document<String> document = new Document<>(TEST_VALUE, Constants.USER, ID);
        mLocalDocumentStorage.writeOffline(mUserTableName, document, new WriteOptions(10));

        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());

        mLocalDocumentStorage.updatePendingOperation(operations.get(0));

        operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
    }

    @Test
    public void createDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.READONLY, ID, "Test", String.class, new WriteOptions());
        Document<String> createdDocument = mLocalDocumentStorage.read(mUserTableName, Constants.READONLY, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test", createdDocument.getDocument());
    }

    @Test
    public void updateDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.READONLY, ID, "Test", String.class, new WriteOptions());
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.READONLY, ID, "Test1", String.class, new WriteOptions());
        Document<String> createdDocument = mLocalDocumentStorage.read(mUserTableName, Constants.READONLY, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test1", createdDocument.getDocument());
    }

    @Test
    public void deleteOfflineAddsOnePendingOperation() {
        mLocalDocumentStorage.markForDeletion(mUserTableName, Constants.USER, ID);
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
    }

    @Test
    public void createAndDeleteOffline() {
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(0, operations.size());
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.USER, ID, "Test", String.class, new WriteOptions());
        operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        boolean updated = mLocalDocumentStorage.markForDeletion(mUserTableName, Constants.USER, ID);
        assertTrue(updated);
        operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
        operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_DELETE_VALUE, operation.getOperation());
    }

    @Test
    public void createAndUpdateOffline() {
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.USER, ID, "Test", String.class, new WriteOptions());
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        assertTrue(operation.getDocument().contains("Test"));
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.USER, ID, "Test2", String.class, new WriteOptions());
        operations = mLocalDocumentStorage.getPendingOperations(mUserTableName);
        assertEquals(1, operations.size());
        operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_REPLACE_VALUE, operation.getOperation());
        assertTrue(operation.getDocument().contains("Test2"));
    }

    @Test
    public void createUnExpiredDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(mUserTableName, Constants.USER, ID, "Test", String.class, new WriteOptions(WriteOptions.INFINITE));
        Document<String> document = mLocalDocumentStorage.read(mUserTableName, Constants.USER, ID, String.class, null);
        assertNull(document.getDocumentError());
        assertEquals("Test", document.getDocument());
    }

    @Test
    public void createDocumentWithoutOverflowException() {
        mLocalDocumentStorage.createOrUpdateOffline(PARTITION, ID, "Test", String.class, new WriteOptions(999999999));
        Document<String> document = mLocalDocumentStorage.read(PARTITION, ID, String.class, null);
        assertNull(document.getDocumentError());
        assertEquals("Test", document.getDocument());
    }
}
