package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.storage.exception.StorageException;
import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.Calendar;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

@VisibleForTesting
class DocumentCache {

    /**
     * Database name.
     */
    @VisibleForTesting
    static final String DATABASE = "com.microsoft.appcenter.documents";

    /**
     * Table name.
     */
    @VisibleForTesting
    static final String TABLE = "cache";

    /**
     * Document Id column.
     */
    @VisibleForTesting
    static final String DOCUMENT_ID_COLUMN_NAME = "documentId";

    /**
     * Partition column.
     */
    @VisibleForTesting
    static final String PARTITION_COLUMN_NAME = "partition";

    /**
     * Content column.
     */
    @VisibleForTesting
    static final String CONTENT_COLUMN_NAME = "content";

    /**
     * Expires at column.
     */
    @VisibleForTesting
    static final String EXPIRES_AT_COLUMN_NAME = "expiresAt";

    /**
     * Current version of the schema.
     */
    private static final int VERSION = 1;

    /**
     * Current schema
     */
    private static final ContentValues SCHEMA = getContentValues("", "", new Document<>(), Calendar.getInstance().getTimeInMillis());

    final DatabaseManager mDatabaseManager;

    public DocumentCache(Context context) {
        mDatabaseManager = new DatabaseManager(context, DATABASE, TABLE, VERSION, SCHEMA, new DatabaseManager.Listener() {

            @Override
            public void onCreate(SQLiteDatabase db) {
                /* No need to do anything on create because `DatabaseManager` creates the table. */
            }

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                /*
                 * No need to do anything on upgrade because this is the first version of the schema
                 * and the rest is handled by `DatabaseManager`.
                 */
                return false;
            }
        });
    }

    public <T> void write(Document<T> document, WriteOptions writeOptions) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to upsert %s:%s document to cache", document.getPartition(), document.getId()));
        Calendar expiresAt = Calendar.getInstance();
        expiresAt.add(Calendar.SECOND, writeOptions.getDeviceTimeToLive());
        ContentValues values = getContentValues(
                document.getId(),
                document.getPartition(),
                document,
                expiresAt.getTimeInMillis());
        mDatabaseManager.upsert(values);
    }

    public <T> Document<T> read(String partition, String documentId, Class<T> documentType, ReadOptions readOptions) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, String.format("Trying to read %s:%s document from cache", partition, documentId));

        /* Read from the DB */
        Cursor cursor = null;
        ContentValues values;
        try {
            cursor = mDatabaseManager.getCursor(
                        getPartitionAndDocumentIdQueryBuilder(),
                        null,
                        new String[]{ partition, documentId },
                        EXPIRES_AT_COLUMN_NAME + " DESC");
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to read from cache: ", e);
            return new Document<>("Failed to read from cache.", e);
        }

        /* We only expect one value as we do upserts in the `write` method */
        values = mDatabaseManager.nextValues(cursor);
        if (cursor != null && values != null) {
            if (readOptions.isExpired(values.getAsLong(EXPIRES_AT_COLUMN_NAME))) {
                mDatabaseManager.delete(cursor.getLong(0));
                return new Document<>(new StorageException("Document was found in the cache, but it was expired. The cached document has been invalidated."));
            }
            Document<T> document = Utils.parseDocument(values.getAsString(CONTENT_COLUMN_NAME), documentType);
            write(document, new WriteOptions(readOptions.getDeviceTimeToLive()));
            document.setIsFromCache(true);
            return document;
        }
        return new Document<>(new StorageException("Document was not found in the cache."));
    }

    public void delete(String partition, String documentId) {
        AppCenterLog.debug(LOG_TAG, String.format("Trying to delete %s:%s document from cache", partition, documentId));
        try {
            mDatabaseManager.delete(
                    String.format("%s = ? AND %s = ?", PARTITION_COLUMN_NAME, DOCUMENT_ID_COLUMN_NAME),
                    new String[] { partition, documentId });
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to delete from cache: ", e);
        }
    }

    private static SQLiteQueryBuilder getPartitionAndDocumentIdQueryBuilder() {
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(PARTITION_COLUMN_NAME + " = ?");
        builder.appendWhere(" AND ");
        builder.appendWhere(DOCUMENT_ID_COLUMN_NAME + " = ?");
        return builder;
    }

    private static <T> ContentValues getContentValues(String documentId, String partition, Document<T> document, long expiresAt) {
        ContentValues values = new ContentValues();
        values.put(DOCUMENT_ID_COLUMN_NAME, documentId);
        values.put(PARTITION_COLUMN_NAME, partition);
        values.put(CONTENT_COLUMN_NAME, Utils.getGson().toJson(document));
        values.put(EXPIRES_AT_COLUMN_NAME, expiresAt);
        return values;
    }
}
