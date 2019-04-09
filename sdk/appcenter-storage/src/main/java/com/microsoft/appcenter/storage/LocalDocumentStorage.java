/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.PendingOperation;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.storage.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.storage.Constants.READONLY;
import static com.microsoft.appcenter.storage.Constants.USER;

@WorkerThread
class LocalDocumentStorage {

    /**
     * Database name.
     */
    @VisibleForTesting
    static final String DATABASE = "com.microsoft.appcenter.documents";

    /**
     * Error message when failed to read from cache.
     */
    @VisibleForTesting
    static final String FAILED_TO_READ_FROM_CACHE = "Failed to read from cache.";

    /**
     * Readonly table name.
     */
    @VisibleForTesting
    static final String READONLY_TABLE = "app_documents";

    /**
     * User-specific table name format.
     */
    @VisibleForTesting
    static final String USER_TABLE_FORMAT = "user_%s_documents";

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
            getContentValues("", "", new Document<>(), "", 0, 0, 0, "");

    private final DatabaseManager mDatabaseManager;

    private LocalDocumentStorage(DatabaseManager databaseManager) {
        mDatabaseManager = databaseManager;
        createUserTable();
    }

    LocalDocumentStorage(Context context) {
        this(new DatabaseManager(context, DATABASE, READONLY_TABLE, VERSION, SCHEMA, new DatabaseManager.DefaultListener()));
    }

    <T> void writeOffline(Document<T> document, WriteOptions writeOptions) {
        write(document, writeOptions, PENDING_OPERATION_CREATE_VALUE);
    }

    <T> void writeOnline(Document<T> document, WriteOptions writeOptions) {
        write(document, writeOptions, null);
    }

    private <T> long write(Document<T> document, WriteOptions writeOptions, String pendingOperationValue) {
        if (writeOptions.getDeviceTimeToLive() == WriteOptions.NO_CACHE) {
            return 0;
        }
        AppCenterLog.debug(LOG_TAG, String.format("Trying to replace %s:%s document to cache", document.getPartition(), document.getId()));
        long now = System.currentTimeMillis();
        ContentValues values = getContentValues(
                document.getPartition(),
                document.getId(),
                document,
                document.getEtag(),
                now + writeOptions.getDeviceTimeToLive() * 1000,
                now,
                now,
                pendingOperationValue);
        return mDatabaseManager.replace(getTableName(document.getPartition()), values, PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME);
    }

    <T> Document<T> read(String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to read %s:%s document from cache", partition, documentId));
        Cursor cursor;
        ContentValues values;
        String table = getTableName(partition);
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
            write(document, new WriteOptions(readOptions.getDeviceTimeToLive()), values.getAsString(PENDING_OPERATION_COLUMN_NAME));
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

    <T> Document<T> createOrUpdateOffline(String partition, String documentId, T document, Class<T> documentType, WriteOptions writeOptions) {
        Document<T> cachedDocument = read(partition, documentId, documentType, new ReadOptions(ReadOptions.NO_CACHE));
        if (cachedDocument.getDocumentError() != null && cachedDocument.getDocumentError().getError().getMessage().equals(FAILED_TO_READ_FROM_CACHE)) {
            return cachedDocument;
        }

        /* The document cache has been expired, or the document did not exists, create it. */
        Document<T> writeDocument = new Document<>(document, partition, documentId);
        long rowId = cachedDocument.getDocumentError() != null ? createOffline(writeDocument, writeOptions) : updateOffline(writeDocument, writeOptions);
        return rowId >= 0 ? writeDocument : new Document<T>(new StorageException("Failed to write document into cache."));
    }

    private <T> long createOffline(Document<T> document, WriteOptions writeOptions) {
        return write(document, writeOptions, Constants.PENDING_OPERATION_CREATE_VALUE);
    }

    private <T> long updateOffline(Document<T> document, WriteOptions writeOptions) {
        return write(document, writeOptions, Constants.PENDING_OPERATION_REPLACE_VALUE);
    }

    /**
     * Creates or overwrites specified document entry in the cache. Sets the de-serialized value of
     * the document to null and the pending operation to DELETE.
     *
     * @param partition  Partition key.
     * @param documentId Document id.
     * @return True if cache was successfully written to, false otherwise.
     */
    boolean markForDeletion(String partition, String documentId) {
        Document<Void> writeDocument = new Document<>(null, partition, documentId);
        long rowId = write(writeDocument, new WriteOptions(), Constants.PENDING_OPERATION_DELETE_VALUE);
        return rowId > 0;
    }

    /**
     * Deletes the specified document from the local cache.
     *
     * @param partition  Partition key.
     * @param documentId Document id.
     */
    void deleteOnline(String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to delete %s:%s document from cache", partition, documentId));
        try {
            mDatabaseManager.delete(
                    getTableName(partition),
                    BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE,
                    new String[]{partition, documentId});
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to delete from cache: ", e);
        }
    }

    /**
     * Deletes the specified document from the cache.
     *
     * @param pendingOperation Pending operation to delete.
     */
    void deletePendingOperation(PendingOperation pendingOperation) {
        deleteOnline(pendingOperation.getPartition(), pendingOperation.getDocumentId());
    }

    List<PendingOperation> getPendingOperations() {
        return getPendingOperations(Constants.USER);
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

    void updatePendingOperation(PendingOperation operation) {

        /*
         * Update the document in cache (if expiration_time still valid otherwise, remove the document),
         * clear the pending_operation column, update etag, download_time and document columns.
         */
        long now = Calendar.getInstance().getTimeInMillis();
        if (operation.getExpirationTime() <= now) {
            deletePendingOperation(operation);
        } else {
            mDatabaseManager.replace(getTableName(operation.getPartition()), getContentValues(operation, now), PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME);
        }
    }

    @VisibleForTesting
    @NonNull
    static String getTableName(String partition) {
        if (USER.equals(partition)) {
            return getUserTableName();
        }
        return READONLY_TABLE;
    }

    private static String getUserTableName() {
        return String.format(USER_TABLE_FORMAT, AuthTokenContext.getInstance().getAccountId()).replace("-", "");
    }

    private List<PendingOperation> getPendingOperations(String partition) {
        List<PendingOperation> result = new ArrayList<>();
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(PENDING_OPERATION_COLUMN_NAME + "  IS NOT NULL");
        Cursor cursor = mDatabaseManager.getCursor(getTableName(partition), builder, null, null, null);

        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (cursor.moveToNext()) {
                ContentValues values = mDatabaseManager.buildValues(cursor);
                result.add(new PendingOperation(
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
     * Creates a table for storing user partition documents.
     */
    void createUserTable() {
        if (AuthTokenContext.getInstance().getAccountId() != null) {
            mDatabaseManager.createTable(getUserTableName(), SCHEMA);
        }
    }

    private static <T> ContentValues getContentValues(
            String partition,
            String documentId,
            Document<T> document,
            String etag,
            long expirationTime,
            long downloadTime,
            long operationTime,
            String pendingOperation) {
        ContentValues values = new ContentValues();
        values.put(PARTITION_COLUMN_NAME, partition);
        values.put(DOCUMENT_ID_COLUMN_NAME, documentId);
        values.put(DOCUMENT_COLUMN_NAME, Utils.getGson().toJson(document));
        values.put(ETAG_COLUMN_NAME, etag);
        values.put(EXPIRATION_TIME_COLUMN_NAME, expirationTime);
        values.put(DOWNLOAD_TIME_COLUMN_NAME, downloadTime);
        values.put(OPERATION_TIME_COLUMN_NAME, operationTime);
        values.put(PENDING_OPERATION_COLUMN_NAME, pendingOperation);
        return values;
    }

    private static ContentValues getContentValues(PendingOperation operation, long now) {
        ContentValues values = new ContentValues();
        values.put(PARTITION_COLUMN_NAME, operation.getPartition());
        values.put(DOCUMENT_ID_COLUMN_NAME, operation.getDocumentId());
        values.put(DOCUMENT_COLUMN_NAME, operation.getDocument());
        values.put(ETAG_COLUMN_NAME, operation.getEtag());
        values.put(EXPIRATION_TIME_COLUMN_NAME, operation.getExpirationTime());
        values.put(DOWNLOAD_TIME_COLUMN_NAME, now);
        values.put(OPERATION_TIME_COLUMN_NAME, now);
        values.put(PENDING_OPERATION_COLUMN_NAME, operation.getOperation());
        return values;
    }
}
