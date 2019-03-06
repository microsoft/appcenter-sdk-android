package com.microsoft.appcenter.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.storage.models.Document;
import com.microsoft.appcenter.storage.models.ReadOptions;
import com.microsoft.appcenter.storage.models.WriteOptions;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import java.util.Calendar;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

public class DocumentCache {

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
     * Last modified column.
     */
    @VisibleForTesting
    static final String LAST_MODIFIED_COLUMN_NAME = "lastModified";

    /**
     * Current version of the schema.
     */
    private static final int VERSION = 1;

    /**
     * Current schema
     */
    private static final ContentValues SCHEMA = getContentValues("", "", new Document<>(), 0);

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

        /* Log. */
        AppCenterLog.debug(LOG_TAG, String.format("Trying to write %s:%s document from cache", document.getPartition(), document.getId()));

        /* TODO: check if there's a record already and update. See if there are existing methods for it. */
        ContentValues values = getContentValues(document.getId(), document.getPartition(), document, Calendar.getInstance().getTimeInMillis());
        mDatabaseManager.put(values);
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
                        LAST_MODIFIED_COLUMN_NAME + " DESC");
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to read from cache: ", e);
        }

        /* We only expect one value */
        values = mDatabaseManager.nextValues(cursor);
        if (cursor != null && values != null) {
            if (readOptions.isExpired(values.getAsLong(LAST_MODIFIED_COLUMN_NAME))) {
                mDatabaseManager.delete(cursor.getLong(0));
                return null;
            }

            return Utils.parseDocument(values.getAsString(CONTENT_COLUMN_NAME), documentType);
        }
        return null;
    }

    public <T> void delete(String partition, String documentId) {
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

    private static <T> ContentValues getContentValues(String documentId, String partition, Document<T> document, long lastModified) {
        ContentValues values = new ContentValues();
        values.put(DOCUMENT_ID_COLUMN_NAME, documentId);
        values.put(PARTITION_COLUMN_NAME, partition);
        values.put(CONTENT_COLUMN_NAME, Utils.getGson().toJson(document));
        values.put(LAST_MODIFIED_COLUMN_NAME, lastModified);
        return values;
    }
}
