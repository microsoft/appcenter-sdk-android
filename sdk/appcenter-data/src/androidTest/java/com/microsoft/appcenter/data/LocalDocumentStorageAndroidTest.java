/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.PendingOperation;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.WriteOptions;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;
import static com.microsoft.appcenter.data.LocalDocumentStorage.FAILED_TO_READ_FROM_CACHE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class LocalDocumentStorageAndroidTest {

    private static final String TEST_VALUE = "Test value";

    private static final String ID = "id";

    private static final String USER_TABLE_NAME = Utils.getUserTableName("123");

    private static final String READ_ONLY_TABLE_NAME = Utils.getTableName(DefaultPartitions.APP_DOCUMENTS, "123");

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
        SharedPreferencesManager.initialize(sContext);
        AuthTokenContext.initialize(sContext);
        AuthTokenContext.getInstance().setAuthToken(UUIDUtils.randomUUID().toString(), UUIDUtils.randomUUID().toString(), new Date());
        mLocalDocumentStorage = new LocalDocumentStorage(sContext, USER_TABLE_NAME);
    }

    @After
    public void tearDown() {
        SharedPreferencesManager.clear();
        AuthTokenContext.unsetInstance();
        sContext.deleteDatabase(com.microsoft.appcenter.Constants.DATABASE);
    }

    @Test
    public void writeReadFail() {
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.APP_DOCUMENTS, ID);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions());
        assertFalse(document.hasFailed());

        /* Pass incorrect class type to create a deserialization error.  */
        DocumentWrapper<Integer> failedDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, Integer.class, new ReadOptions());
        assertNotNull(failedDocument);
        assertTrue(failedDocument.hasFailed());

        /* Confirm document can still be correctly retrieved from the cache. */
        DocumentWrapper<String> cachedDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(cachedDocument);
        assertFalse(cachedDocument.hasFailed());
        assertEquals(document.getDeserializedValue(), cachedDocument.getDeserializedValue());
    }

    @Test
    public void writeReadDelete() {
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.APP_DOCUMENTS, ID);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions());
        DocumentWrapper<String> cachedDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(cachedDocument);
        assertEquals(document.getDeserializedValue(), cachedDocument.getDeserializedValue());
        assertFalse(document.hasFailed());
        assertFalse(document.isFromDeviceCache());
        assertTrue(cachedDocument.isFromDeviceCache());
        mLocalDocumentStorage.deleteOnline(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID);
        DocumentWrapper<String> deletedDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDeserializedValue());
        assertNotNull(deletedDocument.getError());
    }

    @Test
    public void offlineUpdatePreservesEtag() {

        /* Write document to the cache with an etag set. */
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.USER_DOCUMENTS, ID, "etag", 0);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions());
        DocumentWrapper<String> createdDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument.getETag());
        assertEquals("etag", createdDocument.getETag());

        /* Replace document offline, etag should be replaced. */
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, "value2", String.class, new WriteOptions());
        DocumentWrapper<String> updatedDoc = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(updatedDoc.getETag());
        assertEquals("etag", updatedDoc.getETag());
    }

    @Test
    public void offlineDeletePreservesEtag() {

        /* Delete a non-existant document offline, etag should be null. */
        mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, new WriteOptions());
        DocumentWrapper<String> deletedDoc = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, null);
        assertNull(deletedDoc.getETag());

        /* Write document to the cache with an etag set. */
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.USER_DOCUMENTS, ID, "etag", 0);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions());
        DocumentWrapper<String> createdDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument.getETag());
        assertEquals("etag", createdDocument.getETag());

        /* Delete document offline, etag should be replaced. */
        mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, new WriteOptions());
        deletedDoc = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, null);
        assertNotNull(deletedDoc.getETag());
        assertEquals("etag", deletedDoc.getETag());
    }

    @Test
    public void offlineDeletePreservesEtagNullDocumentValue() {

        /* Write document to the cache with an etag set. */
        DocumentWrapper<Void> document = new DocumentWrapper<>(null, DefaultPartitions.USER_DOCUMENTS, ID, "etag", 0);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions());
        DocumentWrapper<String> createdDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument.getETag());
        assertEquals("etag", createdDocument.getETag());

        /* Delete document offline, etag should be replaced. */
        boolean success = mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, new WriteOptions());
        assertTrue(success);
        DocumentWrapper<Void> deletedDoc = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.USER_DOCUMENTS, ID, Void.class, null);
        assertNotNull(deletedDoc.getETag());
        assertEquals("etag", deletedDoc.getETag());
    }

    @Test
    public void readExpiredDocument() {

        /* Write a document and mock device ttl to be already expired a few seconds ago. */
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.APP_DOCUMENTS, ID);
        mLocalDocumentStorage.writeOnline(USER_TABLE_NAME, document, new WriteOptions() {

            @Override
            public int getDeviceTimeToLive() {
                return -2;
            }
        });

        /* Read with a TTL of 1 second: already expired. */
        DocumentWrapper<String> deletedDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions(1));
        assertNotNull(deletedDocument);
        assertNull(deletedDocument.getDeserializedValue());
        assertNotNull(deletedDocument.getError());
    }

    @Test
    public void resetPendingOperationColumnToNull() {
        DocumentWrapper<String> document = new DocumentWrapper<>(TEST_VALUE, USER_DOCUMENTS, ID);
        mLocalDocumentStorage.writeOffline(USER_TABLE_NAME, document, new WriteOptions(10));
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(PENDING_OPERATION_CREATE_VALUE, operation.getOperation());

        /* Reset pending operation column to null. */
        operation.setOperation(null);
        mLocalDocumentStorage.updatePendingOperation(operation);

        /* Retrieve the operations where pending_operation is not null. */
        operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(0, operations.size());
    }

    @Test
    public void createDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(READ_ONLY_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, "Test", String.class, new WriteOptions());
        DocumentWrapper<String> createdDocument = mLocalDocumentStorage.read(READ_ONLY_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test", createdDocument.getDeserializedValue());
    }

    @Test
    public void resetDatabase() {

        /* Create a user table and app table (readonly), and add a document to each. */
        mLocalDocumentStorage.createTableIfDoesNotExist(USER_TABLE_NAME);
        DocumentWrapper<String> userDocument = new DocumentWrapper<>(TEST_VALUE, USER_DOCUMENTS, ID);
        DocumentWrapper<String> appDocument = new DocumentWrapper<>(TEST_VALUE, DefaultPartitions.APP_DOCUMENTS, ID);
        mLocalDocumentStorage.writeOffline(USER_TABLE_NAME, userDocument, new WriteOptions());
        mLocalDocumentStorage.writeOffline(READ_ONLY_TABLE_NAME, appDocument, new WriteOptions());
        DocumentWrapper<String> cachedUserDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, String.class, new ReadOptions());
        DocumentWrapper<String> cachedAppDocument = mLocalDocumentStorage.read(READ_ONLY_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());

        /* Verify to documents were added to the tables and there were no errors. */
        assertNotNull(cachedUserDocument);
        assertNotNull(cachedAppDocument);
        assertNull(cachedUserDocument.getError());
        assertNull(cachedAppDocument.getError());
        assertNotNull(cachedUserDocument.getDeserializedValue());
        assertNotNull(cachedAppDocument.getDeserializedValue());

        /* Reset the database. */
        mLocalDocumentStorage.resetDatabase();
        cachedUserDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, String.class, new ReadOptions());
        cachedAppDocument = mLocalDocumentStorage.read(READ_ONLY_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());

        /* Verify that reading the documents gives an error and their contents are null. */
        assertNotNull(cachedUserDocument);
        assertNotNull(cachedAppDocument);
        assertNotNull(cachedUserDocument.getError());
        assertNotNull(cachedAppDocument.getError());
        assertNull(cachedUserDocument.getDeserializedValue());
        assertNull(cachedAppDocument.getDeserializedValue());
        assertEquals(FAILED_TO_READ_FROM_CACHE, cachedUserDocument.getError().getMessage());
        assertEquals("Document was not found in the cache.", cachedAppDocument.getError().getMessage());
    }

    @Test
    public void updateDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, "Test", String.class, new WriteOptions());
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, "Test1", String.class, new WriteOptions());
        DocumentWrapper<String> createdDocument = mLocalDocumentStorage.read(USER_TABLE_NAME, DefaultPartitions.APP_DOCUMENTS, ID, String.class, new ReadOptions());
        assertNotNull(createdDocument);
        assertEquals("Test1", createdDocument.getDeserializedValue());
    }

    @Test
    public void deleteOfflineAddsOnePendingOperation() {
        mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, new WriteOptions());
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
    }

    @Test
    public void createAndDeleteOffline() {
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(0, operations.size());
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test", String.class, new WriteOptions());
        operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        boolean updated = mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, new WriteOptions());
        assertTrue(updated);
        operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        operation = operations.get(0);
        assertEquals(PENDING_OPERATION_DELETE_VALUE, operation.getOperation());
    }

    @Test
    public void createAndUpdateOffline() {
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test", String.class, new WriteOptions());
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        assertTrue(operation.getDocument().contains("Test"));
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test2", String.class, new WriteOptions());
        operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        operation = operations.get(0);
        assertEquals(Constants.PENDING_OPERATION_REPLACE_VALUE, operation.getOperation());
        assertTrue(operation.getDocument().contains("Test2"));
    }

    @Test
    public void createAndUpdateOfflineDifferentIDs() {
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test", String.class, new WriteOptions());
        List<PendingOperation> operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(1, operations.size());
        PendingOperation operation = operations.get(0);
        assertEquals(PENDING_OPERATION_CREATE_VALUE, operation.getOperation());
        assertTrue(operation.getDocument().contains("Test"));
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID + "1", "Test2", String.class, new WriteOptions());
        operations = mLocalDocumentStorage.getPendingOperations(USER_TABLE_NAME);
        assertEquals(2, operations.size());
        PendingOperation operation1 = operations.get(0);
        PendingOperation operation2 = operations.get(1);
        assertEquals(PENDING_OPERATION_CREATE_VALUE, operation1.getOperation());
        assertEquals(PENDING_OPERATION_CREATE_VALUE, operation2.getOperation());
        assertTrue(operation1.getDocument().contains("Test"));
        assertTrue(operation2.getDocument().contains("Test2"));
    }

    @Test
    public void createUnExpiredDocument() {
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test", String.class, new WriteOptions(TimeToLive.INFINITE));
        DocumentWrapper<String> document = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, String.class, null);
        assertNull(document.getError());
        assertEquals("Test", document.getDeserializedValue());
    }

    @Test
    public void createDocumentWithoutOverflowException() {
        mLocalDocumentStorage.createOrUpdateOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, "Test", String.class, new WriteOptions(999999999));
        DocumentWrapper<String> document = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, String.class, null);
        assertNull(document.getError());
        assertEquals("Test", document.getDeserializedValue());
    }

    @Test
    public void syncDeleteOperationMustRemoveFromCache() {

        /* If a document is marked for deletion. */
        mLocalDocumentStorage.deleteOffline(USER_TABLE_NAME, USER_DOCUMENTS, ID, new WriteOptions());
        DocumentWrapper<Void> document = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, Void.class, null);
        assertNull(document.getError());

        /* When we delete after coming back online. */
        mLocalDocumentStorage.deleteOnline(USER_TABLE_NAME, USER_DOCUMENTS, ID);

        /* Then the entry is removed from cache. */
        document = mLocalDocumentStorage.read(USER_TABLE_NAME, USER_DOCUMENTS, ID, Void.class, null);
        assertNotNull(document.getError());
    }
}
