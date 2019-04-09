/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.PendingOperation;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.DATABASE;
import static com.microsoft.appcenter.Constants.READONLY_TABLE;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.storage.Constants.READONLY;
import static com.microsoft.appcenter.storage.Constants.USER;

@WorkerThread
class LocalDocumentStorage {

    /**
     * Error message when failed to read from cache.
     */
    @VisibleForTesting
    static final String FAILED_TO_READ_FROM_CACHE = "Failed to read from cache.";

    /**
     * Partition column.
     */
    @VisibleForTesting
    static final String PARTITION_COLUMN_NAME = "partition";

    /**
     * Document Id column.
     */
    @VisibleForTesting
    static final String DOCUMENT_ID_COLUMN_NAME = "document_id";

    /**
     * Document column.
     */
    private static final String DOCUMENT_COLUMN_NAME = "document";

    /**
     * Etag column.
     */
    private static final String ETAG_COLUMN_NAME = "etag";

    /**
     * Expiration time column.
     */
    private static final String EXPIRATION_TIME_COLUMN_NAME = "expiration_time";

    /**
     * Download time column.
     */
    private static final String DOWNLOAD_TIME_COLUMN_NAME = "download_time";

    /**
     * Operation time column.
     */
    private static final String OPERATION_TIME_COLUMN_NAME = "operation_time";

    /**
     * Pending operation column.
     */
    private static final String PENDING_OPERATION_COLUMN_NAME = "pending_operation";

    /**
     * Current version of the schema.
     */
    private static final int VERSION = 1;

    /**
     * `Where` clause to select by partition and document ID.
     */
    private static final String BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE =
            String.format("%s = ? AND %s = ?", PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME);

    /**
     * Current schema.
     */
    private static final ContentValues SCHEMA =
            getContentValues("", "", "", "", 0, 0, 0, "");

    private final DatabaseManager mDatabaseManager;

    LocalDocumentStorage(Context context, String userTable) {
        mDatabaseManager = new DatabaseManager(context, DATABASE, READONLY_TABLE, VERSION, SCHEMA, new DatabaseManager.DefaultListener());
        if (userTable != null) {
            createTableIfDoesNotExist(userTable);
        }
    }

    /**
     * Creates a table for storing user partition documents.
     */
    void createTableIfDoesNotExist(String userTable) {
        mDatabaseManager.createTable(userTable, SCHEMA);
    }

    <T> void writeOffline(String table, Document<T> document, WriteOptions writeOptions) {
        write(table, document, writeOptions, PENDING_OPERATION_CREATE_VALUE);
    }

    <T> void writeOnline(String table, Document<T> document, WriteOptions writeOptions) {
        write(table, document, writeOptions, null);
    }

    private <T> long write(String table, Document<T> document, WriteOptions writeOptions, String pendingOperationValue) {
        if (writeOptions.getDeviceTimeToLive() == WriteOptions.NO_CACHE) {
            return 0;
        }
        AppCenterLog.debug(LOG_TAG, String.format("Trying to replace %s:%s document to cache", document.getPartition(), document.getId()));
        long now = System.currentTimeMillis();
        ContentValues values = getContentValues(
                document.getPartition(),
                document.getId(),
                Utils.getGson().toJson(document),
                document.getEtag(),
                now + writeOptions.getDeviceTimeToLive() * 1000,
                now,
                now,
                pendingOperationValue);
        return mDatabaseManager.replace(table, values, PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME);
    }

    <T> Document<T> read(String table, String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to read %s:%s document from cache", partition, documentId));
        Cursor cursor;
        ContentValues values;
        try {
            cursor = mDatabaseManager.getCursor(
                    table,
                    getPartitionAndDocumentIdQueryBuilder(),
                    null,
                    new String[]{partition, documentId},
                    EXPIRATION_TIME_COLUMN_NAME + " DESC");
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to read from cache: ", e);
            return new Document<>(FAILED_TO_READ_FROM_CACHE, e);
        }

        /* We only expect one value as we do upserts in the `write` method */
        values = mDatabaseManager.nextValues(cursor);
        if (values != null) {
            if (ReadOptions.isExpired(values.getAsLong(EXPIRATION_TIME_COLUMN_NAME))) {
                mDatabaseManager.delete(table, cursor.getLong(0));
                AppCenterLog.info(LOG_TAG, "Document was found in the cache, but it was expired. The cached document has been invalidated.");
                return new Document<>(new StorageException("Document was found in the cache, but it was expired. The cached document has been invalidated."));
            }
            Document<T> document = Utils.parseDocument(values.getAsString(DOCUMENT_COLUMN_NAME), documentType);
            write(table, document, new WriteOptions(readOptions.getDeviceTimeToLive()), values.getAsString(PENDING_OPERATION_COLUMN_NAME));
            document.setIsFromCache(true);
            return document;
        }
        AppCenterLog.info(LOG_TAG, "Document was found in the cache, but it was expired. The cached document has been invalidated.");
        return new Document<>(new StorageException("Document was not found in the cache."));
    }

    private static SQLiteQueryBuilder getPartitionAndDocumentIdQueryBuilder() {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE);
        return builder;
    }

    <T> Document<T> createOrUpdateOffline(String table, String partition, String documentId, T document, Class<T> documentType, WriteOptions writeOptions) {
        Document<T> cachedDocument = read(table, partition, documentId, documentType, new ReadOptions(ReadOptions.NO_CACHE));
        if (cachedDocument.getDocumentError() != null && cachedDocument.getDocumentError().getError().getMessage().equals(FAILED_TO_READ_FROM_CACHE)) {
            return cachedDocument;
        }

        /* The document cache has been expired, or the document did not exists, create it. */
        Document<T> writeDocument = new Document<>(document, partition, documentId);
        long rowId =
                cachedDocument.getDocumentError() != null ?
                        createOffline(table, writeDocument, writeOptions) :
                        updateOffline(table, writeDocument, writeOptions);
        return rowId >= 0 ? writeDocument : new Document<T>(new StorageException("Failed to write document into cache."));
    }

    private <T> long createOffline(String table, Document<T> document, WriteOptions writeOptions) {
        return write(table, document, writeOptions, Constants.PENDING_OPERATION_CREATE_VALUE);
    }

    private <T> long updateOffline(String table, Document<T> document, WriteOptions writeOptions) {
        return write(table, document, writeOptions, Constants.PENDING_OPERATION_REPLACE_VALUE);
    }

    /**
     * Creates or overwrites specified document entry in the cache. Sets the de-serialized value of
     * the document to null and the pending operation to DELETE.
     *
     * @param partition  Partition key.
     * @param documentId Document id.
     * @return True if cache was successfully written to, false otherwise.
     */
    boolean markForDeletion(String table, String partition, String documentId) {
        Document<Void> writeDocument = new Document<>(null, partition, documentId);
        long rowId = write(table, writeDocument, new WriteOptions(), Constants.PENDING_OPERATION_DELETE_VALUE);
        return rowId > 0;
    }

    /**
     * Deletes the specified document from the local cache.
     *
     * @param partition  Partition key.
     * @param documentId Document id.
     */
    void deleteOnline(String table, String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to delete %s:%s document from cache", partition, documentId));
        try {
            mDatabaseManager.delete(
                    table,
                    BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE,
                    new String[]{partition, documentId});
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to delete from cache: ", e);
        }
    }

    List<PendingOperation> getPendingOperations(String table) {
        List<PendingOperation> result = new ArrayList<>();
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(PENDING_OPERATION_COLUMN_NAME + "  IS NOT NULL");
        Cursor cursor = mDatabaseManager.getCursor(table, builder, null, null, null);

        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (cursor.moveToNext()) {
                ContentValues values = mDatabaseManager.buildValues(cursor);
                result.add(new PendingOperation(
                        table,
                        values.getAsString(PENDING_OPERATION_COLUMN_NAME),
                        values.getAsString(PARTITION_COLUMN_NAME),
                        values.getAsString(DOCUMENT_ID_COLUMN_NAME),
                        values.getAsString(DOCUMENT_COLUMN_NAME),
                        values.getAsLong(EXPIRATION_TIME_COLUMN_NAME)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * Deletes the specified document from the cache.
     *
     * @param operation Pending operation to delete.
     */
    void deletePendingOperation(PendingOperation operation) {
        deleteOnline(operation.getTable(), operation.getPartition(), operation.getDocumentId());
    }

    /**
     * Validate partition name.
     *
     * @param partition name.
     * @return true if the partition is supported, false otherwise.
     */
    static boolean isValidPartitionName(String partition) {
        return READONLY.equals(partition) || USER.equals(partition);
    }

    private static ContentValues getContentValues(
            String partition,
            String documentId,
            String document,
            String eTag,
            long expirationTime,
            long downloadTime,
            long operationTime,
            String pendingOperation) {
        ContentValues values = new ContentValues();
        values.put(PARTITION_COLUMN_NAME, partition);
        values.put(DOCUMENT_ID_COLUMN_NAME, documentId);
        values.put(DOCUMENT_COLUMN_NAME, document);
        values.put(ETAG_COLUMN_NAME, eTag);
        values.put(EXPIRATION_TIME_COLUMN_NAME, expirationTime);
        values.put(DOWNLOAD_TIME_COLUMN_NAME, downloadTime);
        values.put(OPERATION_TIME_COLUMN_NAME, operationTime);
        values.put(PENDING_OPERATION_COLUMN_NAME, pendingOperation);
        return values;
    }

    void updatePendingOperation(PendingOperation operation) {

        /*
         * Update the document in cache (if expiration_time still valid otherwise, remove the document),
         * clear the pending_operation column, update eTag, download_time and document columns.
         */
        long now = Calendar.getInstance().getTimeInMillis();
        if (operation.getExpirationTime() <= now) {
            deletePendingOperation(operation);
        } else {
            mDatabaseManager.replace(operation.getTable(), getContentValues(operation, now), PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME);
        }
    }

    private static ContentValues getContentValues(PendingOperation operation, long now) {
        return getContentValues(
                operation.getPartition(),
                operation.getDocumentId(),
                operation.getDocument(),
                operation.getEtag(),
                operation.getExpirationTime(),
                now,
                now,
                operation.getOperation());
    }
}
