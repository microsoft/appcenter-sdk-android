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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class LocalDocumentStorageAndroidTest {

    private static final String TEST_VALUE = "Test value";

    private static final String PARTITION = "partition";

    private static final String ID = "id";

    private static final long mNow = Calendar.getInstance().getTimeInMillis();

    /**
     * Context instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Context sContext;

    private LocalDocumentStorage mLocalDocumentStorage;

    @BeforeClass
    public static void setUpClass() {
        sContext = InstrumentationRegistry.getTargetContext();
    }

    @Before
    public void setUp() {
        mLocalDocumentStorage = new LocalDocumentStorage(sContext);
    }

    @After
    public void tearDown() {
        sContext.deleteDatabase(LocalDocumentStorage.DATABASE);
    }

    @Test
    public void writeReadDelete() {
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mLocalDocumentStorage.writeOnline(document, new WriteOptions());
        Document<String> cachedDocument = mLocalDocumentStorage.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(cachedDocument);
        assertEquals(document.getDocument(), cachedDocument.getDocument());
        assertFalse(document.failed());
        assertFalse(document.isFromCache());
        assertTrue(cachedDocument.isFromCache());
        mLocalDocumentStorage.deleteOnline(PARTITION, ID);
        Document<String> deletedDocument = mLocalDocumentStorage.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getDocumentError());
    }

    @Test
    public void readExpiredDocument() {

        /* Write a document and mock device ttl to be already expired a few seconds ago. */
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mLocalDocumentStorage.writeOnline(document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -2;
            }
        });

        /* Read with a TTL of 1 second: already expired. */
        Document<String> deletedDocument = mLocalDocumentStorage.read(PARTITION, ID, String.class, new ReadOptions(1));
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDocument());
        assertNotNull(deletedDocument.getDocumentError());
    }

    @Test
    public void updateLocalCopyDeletesExpiredOperation() {
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mLocalDocumentStorage.writeOffline(document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -10;
            }
        });

        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());

        mLocalDocumentStorage.updatePendingOperation(operations.get(0));

        operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(0, operations.size());
    }

    @Test
    public void updateLocalCopyReplacesNotExpiredOperation() {
        Document<String> document = new Document<>(TEST_VALUE, PARTITION, ID);
        mLocalDocumentStorage.writeOffline(document, new WriteOptions(10));

        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());

        mLocalDocumentStorage.updatePendingOperation(operations.get(0));

        operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());
    }

    @Test
    public void createDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(PARTITION, ID, "Test", String.class, new WriteOptions());
        Document<String> createdDocument = mLocalDocumentStorage.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test", createdDocument.getDocument());
    }

    @Test
    public void updateDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(PARTITION, ID, "Test", String.class, new WriteOptions());
        mLocalDocumentStorage.createOrUpdateOffline(PARTITION, ID, "Test1", String.class, new WriteOptions());
        Document<String> createdDocument = mLocalDocumentStorage.read(PARTITION, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test1", createdDocument.getDocument());
    }

    @Test
    public void deleteOfflineAddsOnePendingOperation() {
        mLocalDocumentStorage.deleteOffline(PARTITION, ID);
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());
    }

    @Test
    public void createAndDeleteOffline() {
        mLocalDocumentStorage.createOrUpdateOffline(PARTITION, ID, "Test", String.class, new WriteOptions());
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        mLocalDocumentStorage.deleteOffline(PARTITION, ID);
        operations = mLocalDocumentStorage.getPendingOperations();
        assertEquals(1, operations.size());
        operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_DELETE_VALUE, operation.getOperation());
    }
}
