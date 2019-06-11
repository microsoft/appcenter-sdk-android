/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.data.exception.DataException;
import com.microsoft.appcenter.data.models.DocumentWrapper;
import com.microsoft.appcenter.data.models.LocalDocument;
import com.microsoft.appcenter.data.models.ReadOptions;
import com.microsoft.appcenter.data.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.DATABASE;
import static com.microsoft.appcenter.Constants.READONLY_TABLE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_CREATE_VALUE;
import static com.microsoft.appcenter.data.Constants.PENDING_OPERATION_DELETE_VALUE;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
import static com.microsoft.appcenter.data.DefaultPartitions.USER_DOCUMENTS;

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
    private static final String PARTITION_COLUMN_NAME = "partition";

    /**
     * Document Id column.
     */
    private static final String DOCUMENT_ID_COLUMN_NAME = "document_id";

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
        mDatabaseManager.createTable(userTable, SCHEMA, new String[]{PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME});
    }

    /**
     * Delete the database and create a new, empty one.
     */
    void resetDatabase() {
        mDatabaseManager.resetDatabase();
    }

    <T> void writeOffline(String table, DocumentWrapper<T> document, WriteOptions writeOptions) {
        write(table, document, writeOptions, PENDING_OPERATION_CREATE_VALUE);
    }

    <T> void writeOnline(String table, DocumentWrapper<T> document, WriteOptions writeOptions) {
        write(table, document, writeOptions, null);
    }

    private <T> long write(String table, DocumentWrapper<T> document, WriteOptions writeOptions, String pendingOperationValue) {
        if (writeOptions.getDeviceTimeToLive() == TimeToLive.NO_CACHE) {
            return 0;
        }
        AppCenterLog.debug(LOG_TAG, String.format("Trying to replace %s:%s document to cache", document.getPartition(), document.getId()));
        long now = System.currentTimeMillis();
        ContentValues values = getContentValues(
                document.getPartition(),
                document.getId(),
                document.getJsonValue(),
                document.getETag(),
                writeOptions.getDeviceTimeToLive() == TimeToLive.INFINITE ?
                        TimeToLive.INFINITE : now + writeOptions.getDeviceTimeToLive() * 1000L,
                now,
                now,
                pendingOperationValue);
        return mDatabaseManager.replace(table, values);
    }

    private static SQLiteQueryBuilder getPartitionAndDocumentIdQueryBuilder() {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE);
        return builder;
    }

    <T> DocumentWrapper<T> createOrUpdateOffline(String table, String partition, String documentId, T document, Class<T> documentType, WriteOptions writeOptions) {
        DocumentWrapper<T> cachedDocument = read(table, partition, documentId, documentType, null);
        cachedDocument.setFromCache(true);
        if (cachedDocument.getError() != null && cachedDocument.getError().getMessage().equals(FAILED_TO_READ_FROM_CACHE)) {
            return cachedDocument;
        }

        /* The document cache has been expired, or the document did not exists, create it. */
        DocumentWrapper<T> writeDocument = new DocumentWrapper<>(document, partition, documentId, cachedDocument.getETag(), System.currentTimeMillis());
        long rowId =
                cachedDocument.getError() != null ?
                        createOffline(table, writeDocument, writeOptions) :
                        updateOffline(table, writeDocument, writeOptions);
        if (rowId < 0) {
            writeDocument = new DocumentWrapper<>(new DataException("Failed to write document into cache."));
        }
        writeDocument.setFromCache(true);
        return writeDocument;
    }

    private <T> long createOffline(String table, DocumentWrapper<T> document, WriteOptions writeOptions) {
        return write(table, document, writeOptions, Constants.PENDING_OPERATION_CREATE_VALUE);
    }

    private <T> long updateOffline(String table, DocumentWrapper<T> document, WriteOptions writeOptions) {
        return write(table, document, writeOptions, Constants.PENDING_OPERATION_REPLACE_VALUE);
    }

    /**
     * Add delete pending operation to a document.
     *
     * @param table           table.
     * @param documentWrapper document wrapper.
     * @param writeOptions    captures the timeToLive on the cached delete operation
     * @return true if storage update succeeded, false otherwise.
     */
    boolean deleteOffline(String table, DocumentWrapper<Void> documentWrapper, WriteOptions writeOptions) {
        documentWrapper.setFromCache(true);
        return deleteOffline(table, documentWrapper.getPartition(), documentWrapper.getId(), writeOptions);
    }

    /**
     * Add delete pending operation to a document.
     *
     * @param table        table.
     * @param partition    partition.
     * @param documentId   document identifier.
     * @param writeOptions captures the timeToLive on the cached delete operation
     * @return true if storage update succeeded, false otherwise.
     */
    boolean deleteOffline(String table, String partition, String documentId, WriteOptions writeOptions) {
        DocumentWrapper<Void> cachedDocument = read(table, partition, documentId, Void.class, null);
        DocumentWrapper<Void> writeDocument = new DocumentWrapper<>(null, partition, documentId, cachedDocument.getETag(), System.currentTimeMillis());
        return write(table, writeDocument, writeOptions, PENDING_OPERATION_DELETE_VALUE) > 0;
    }

    /**
     * Remove a document from local storage.
     *
     * @param table      table.
     * @param partition  partition.
     * @param documentId document identifier.
     * @return true if storage delete succeeded, false otherwise.
     */
    boolean deleteOnline(String table, String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to delete %s:%s document from cache", partition, documentId));
        try {
            return mDatabaseManager.delete(
                    table,
                    BY_PARTITION_AND_DOCUMENT_ID_WHERE_CLAUSE,
                    new String[]{partition, documentId}) > 0;
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to delete from cache: ", e);
            return false;
        }
    }

    List<LocalDocument> getDocumentsByPartition(String table, String partition) {
        return queryLocalStorage(table, PARTITION_COLUMN_NAME + " = ?", new String[] { partition });
    }


    List<LocalDocument> getPendingOperations(String table) {
        return queryLocalStorage(table, PENDING_OPERATION_COLUMN_NAME + " IS NOT NULL", null);
    }
    private List<LocalDocument> queryLocalStorage(String table, String whereClause, String[] selectionArgs) {
        List<LocalDocument> result = new ArrayList<>();
        if (table == null) {
            return result;
        }
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(whereClause);
        Cursor cursor = mDatabaseManager.getCursor(table, builder, null, selectionArgs, null);

        //noinspection TryFinallyCanBeTryWithResources
        try {
            while (cursor.moveToNext()) {
                ContentValues values = mDatabaseManager.buildValues(cursor);
                result.add(new LocalDocument(
                                table,
                                values.getAsString(PENDING_OPERATION_COLUMN_NAME),
                                values.getAsString(PARTITION_COLUMN_NAME),
                                values.getAsString(DOCUMENT_ID_COLUMN_NAME),
                                values.getAsString(DOCUMENT_COLUMN_NAME),
                                values.getAsLong(EXPIRATION_TIME_COLUMN_NAME),
                                values.getAsLong(DOWNLOAD_TIME_COLUMN_NAME),
                                values.getAsLong(OPERATION_TIME_COLUMN_NAME)));
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * @param localDocuments list of documents to check the condition on
     * @return true if there is at least one document is in storage for the given partition
     * and has pending operation on it and is not expired
     */
    static boolean hasPendingOperationAndIsNotExpired(@NonNull List<LocalDocument> localDocuments) {
        for (LocalDocument doc : localDocuments) {
            if (doc.isPendingOperation() && !doc.isExpired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Validate partition name.
     *
     * @param partition name.
     * @return true if the partition is supported, false otherwise.
     */
    static boolean isValidPartitionName(String partition) {
        return APP_DOCUMENTS.equals(partition) || USER_DOCUMENTS.equals(partition);
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

    /**
     * Update the pending operation.
     *
     * @param operation Pending operation to update.
     */
    void updatePendingOperation(LocalDocument operation) {
        ContentValues values = getContentValues(
                operation.getPartition(),
                operation.getDocumentId(),
                operation.getDocument(),
                operation.getETag(),
                operation.getExpirationTime(),
                operation.getDownloadTime(),
                operation.getOperationTime(),
                operation.getOperation());
        mDatabaseManager.replace(operation.getTable(), values);
    }

    @NonNull
    <T> DocumentWrapper<T> read(String table, String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {
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
            return new DocumentWrapper<>(FAILED_TO_READ_FROM_CACHE, e);
        }

        /* We only expect one value as we do upserts in the `write` method. */
        values = mDatabaseManager.nextValues(cursor);
        cursor.close();
        if (values != null) {
            if (ReadOptions.isExpired(values.getAsLong(EXPIRATION_TIME_COLUMN_NAME))) {
                mDatabaseManager.delete(table, values.getAsLong(DatabaseManager.PRIMARY_KEY));
                String errorMessage = "Document was found in the cache, but it was expired. The cached document has been invalidated.";
                AppCenterLog.debug(LOG_TAG, errorMessage);
                return new DocumentWrapper<>(new DataException(errorMessage));
            }
            String document = values.getAsString(DOCUMENT_COLUMN_NAME);
            String eTag = values.getAsString(ETAG_COLUMN_NAME);
            long operationTime = values.getAsLong(OPERATION_TIME_COLUMN_NAME);
            DocumentWrapper<T> documentWrapper = Utils.parseDocument(document, partition, documentId, eTag, operationTime / 1000L, documentType);
            documentWrapper.setFromCache(true);
            documentWrapper.setPendingOperation(values.getAsString(PENDING_OPERATION_COLUMN_NAME));

            /*
             * Update the expiredAt time only when the readOptions is not null, otherwise keep updating it.
             */
            if (readOptions != null) {
                if (readOptions.getDeviceTimeToLive() == TimeToLive.NO_CACHE) {

                    /* Delete the document since no cache was requested. */
                    mDatabaseManager.delete(table, values.getAsLong(DatabaseManager.PRIMARY_KEY));
                } else if (!documentWrapper.hasFailed()) {

                    /* We update cache timestamp only if no serialization issue, otherwise that would corrupt cache in payload. */
                    write(table, documentWrapper, new WriteOptions(readOptions.getDeviceTimeToLive()), values.getAsString(PENDING_OPERATION_COLUMN_NAME));
                }
            }
            return documentWrapper;
        }
        AppCenterLog.debug(LOG_TAG, "Document was found in the cache, but it was expired. The cached document has been invalidated.");
        return new DocumentWrapper<>(new DataException("Document was not found in the cache."));
    }
}
