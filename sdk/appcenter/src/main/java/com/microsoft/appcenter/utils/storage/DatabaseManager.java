/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.Closeable;
import java.util.Arrays;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Database manager for SQLite.
 */
public class DatabaseManager implements Closeable {

    /**
     * Primary key name.
     */
    public static final String PRIMARY_KEY = "oid";

    /**
     * Primary key selection for {@link #getCursor(SQLiteQueryBuilder, String[], String[], String)}.
     */
    public static final String[] SELECT_PRIMARY_KEY = {PRIMARY_KEY};

    /**
     * Application context instance.
     */
    private final Context mContext;

    /**
     * Database name.
     */
    private final String mDatabase;

    /**
     * Default table name. Used as default value in methods which require a table name to work.
     */
    private final String mDefaultTable;

    /**
     * Schema, e.g. a specimen with dummy values to have keys and their corresponding value's type.
     */
    private final ContentValues mSchema;

    /**
     * Listener instance.
     */
    private final Listener mListener;

    /**
     * SQLite helper instance.
     */
    private SQLiteOpenHelper mSQLiteOpenHelper;

    /**
     * Initializes the table in the database.
     *
     * @param context      The application context.
     * @param database     The database name.
     * @param defaultTable The default table name.
     * @param version      The version of current schema.
     * @param schema       The schema.
     * @param listener     The error listener.
     */
    public DatabaseManager(Context context, String database, String defaultTable, int version,
                           ContentValues schema, final String sqlCreateCommand, @NonNull Listener listener) {
        mContext = context;
        mDatabase = database;
        mDefaultTable = defaultTable;
        mSchema = schema;
        mListener = listener;
        mSQLiteOpenHelper = new SQLiteOpenHelper(context, database, null, version) {

            @Override
            public void onCreate(SQLiteDatabase db) {
                db.execSQL(sqlCreateCommand);
                mListener.onCreate(db);
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                mListener.onUpgrade(db, oldVersion, newVersion);
            }
        };
    }

    /**
     * Converts a cursor to an entry.
     *
     * @param cursor The cursor to be converted to an entry.
     * @param schema The schema with value types.
     * @return An entry converted from the cursor.
     */
    private static ContentValues buildValues(Cursor cursor, ContentValues schema) {
        ContentValues values = new ContentValues();
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            if (cursor.isNull(i)) {
                continue;
            }
            String key = cursor.getColumnName(i);
            if (key.equals(PRIMARY_KEY)) {
                values.put(key, cursor.getLong(i));
            } else {
                Object specimen = schema.get(key);
                if (specimen instanceof byte[]) {
                    values.put(key, cursor.getBlob(i));
                } else if (specimen instanceof Double) {
                    values.put(key, cursor.getDouble(i));
                } else if (specimen instanceof Float) {
                    values.put(key, cursor.getFloat(i));
                } else if (specimen instanceof Integer) {
                    values.put(key, cursor.getInt(i));
                } else if (specimen instanceof Long) {
                    values.put(key, cursor.getLong(i));
                } else if (specimen instanceof Short) {
                    values.put(key, cursor.getShort(i));
                } else if (specimen instanceof Boolean) {
                    values.put(key, cursor.getInt(i) == 1);
                } else {
                    values.put(key, cursor.getString(i));
                }
            }
        }
        return values;
    }

    /**
     * Converts a cursor to an entry.
     *
     * @param cursor The cursor to be converted to an entry.
     * @return An entry converted from the cursor.
     */
    public ContentValues buildValues(Cursor cursor) {
        return buildValues(cursor, mSchema);
    }

    /**
     * Get next entry from the cursor.
     *
     * @param cursor The cursor to be converted to an entry.
     * @return An entry converted from the cursor.
     */
    @Nullable
    public ContentValues nextValues(Cursor cursor) {
        try {
            if (cursor.moveToNext()) {
                return buildValues(cursor);
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to get next cursor value: ", e);
        }
        return null;
    }

    /**
     * Stores the entry to the table. If the table is full, the oldest logs are discarded until the
     * new one can fit. If the log is larger than the max table size, database will be cleared and
     * the log is not inserted.
     *
     * @param values         The entry to be stored.
     * @param priorityColumn When storage full and deleting data, use this column to determine which entries to delete first.
     * @return If a log was inserted, the database identifier. Otherwise -1.
     */
    public long put(@NonNull ContentValues values, @NonNull String priorityColumn) {
        Long id = null;
        Cursor cursor = null;
        try {
            while (id == null) {
                try {

                    /* Insert data. */
                    id = getDatabase().insertOrThrow(mDefaultTable, null, values);
                } catch (SQLiteFullException e) {

                    /* Delete the oldest log. */
                    AppCenterLog.debug(LOG_TAG, "Storage is full, trying to delete the oldest log that has the lowest priority which is lower or equal priority than the new log");
                    if (cursor == null) {
                        String priority = values.getAsString(priorityColumn);
                        SQLiteQueryBuilder queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
                        queryBuilder.appendWhere(priorityColumn + " <= ?");
                        cursor = getCursor(queryBuilder, SELECT_PRIMARY_KEY, new String[]{priority}, priorityColumn + " , " + PRIMARY_KEY);
                    }
                    if (cursor.moveToNext()) {
                        long deletedId = cursor.getLong(0);
                        delete(deletedId);
                        AppCenterLog.debug(LOG_TAG, "Deleted log id=" + deletedId);
                    } else {
                        throw e;
                    }
                }
            }
        } catch (RuntimeException e) {
            id = -1L;
            AppCenterLog.error(LOG_TAG, String.format("Failed to insert values (%s) to database %s.", values.toString(), mDatabase), e);
        }
        if (cursor != null) {
            try {
                cursor.close();
            } catch (RuntimeException ignore) {
            }
        }
        return id;
    }

    /**
     * Deletes the entry by the identifier from the database.
     *
     * @param id The database identifier.
     */
    public void delete(@IntRange(from = 0) long id) {
        delete(mDefaultTable, PRIMARY_KEY, id);
    }

    /**
     * Deletes the entries that matches key == value.
     *
     * @param key   The optional key for query.
     * @param value The optional value for query.
     * @return the number of rows affected.
     */
    public int delete(@NonNull String key, @Nullable Object value) {
        return delete(mDefaultTable, key, value);
    }

    /**
     * Deletes the entries that matches key == value.
     *
     * @param table The table to perform the operation on.
     * @param key   The optional key for query.
     * @param value The optional value for query.
     * @return the number of rows affected.
     */
    private int delete(@NonNull String table, @NonNull String key, @Nullable Object value) {
        String[] whereArgs = new String[]{String.valueOf(value)};
        try {
            return getDatabase().delete(table, key + " = ?", whereArgs);
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, String.format("Failed to delete values that match condition=\"%s\" and values=\"%s\" from database %s.", key + " = ?", Arrays.toString(whereArgs), mDatabase), e);
            return 0;
        }
    }

    /**
     * Clears the table in the database.
     */
    public void clear() {
        try {
            getDatabase().delete(mDefaultTable, null, null);
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to clear the table.", e);
        }
    }

    /**
     * Closes database.
     */
    @Override
    public void close() {
        try {

            /* Close opened database (do not force open). */
            mSQLiteOpenHelper.close();
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to close the database.", e);
        }
    }

    /**
     * Gets the count of records in the table.
     *
     * @return The number of records in the table, or <code>-1</code> if operation failed.
     */
    public final long getRowCount() {
        try {
            return DatabaseUtils.queryNumEntries(getDatabase(), mDefaultTable);
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to get row count of database.", e);
            return -1;
        }
    }

    /**
     * Gets a cursor for all rows in the table, all rows where key matches value if specified.
     *
     * @param queryBuilder  The query builder that contains SQL query.
     * @param columns       Columns to select, null for all.
     * @param selectionArgs The array of values for selection.
     * @param sortOrder     Sorting order (ORDER BY clause without ORDER BY itself).
     * @return A cursor for all rows that matches the given criteria.
     * @throws RuntimeException If an error occurs.
     */
    public Cursor getCursor(@Nullable SQLiteQueryBuilder queryBuilder, String[] columns, @Nullable String[] selectionArgs, @Nullable String sortOrder) throws RuntimeException {
        return getCursor(mDefaultTable, queryBuilder, columns, selectionArgs, sortOrder);
    }

    /**
     * Gets a cursor for all rows in the table, all rows where key matches value if specified.
     *
     * @param table         The table to perform the operation on.
     * @param queryBuilder  The query builder that contains SQL query.
     * @param columns       Columns to select, null for all.
     * @param selectionArgs The array of values for selection.
     * @param sortOrder     Sorting order (ORDER BY clause without ORDER BY itself).
     * @return A cursor for all rows that matches the given criteria.
     * @throws RuntimeException If an error occurs.
     */
    Cursor getCursor(@NonNull String table, @Nullable SQLiteQueryBuilder queryBuilder, String[] columns, @Nullable String[] selectionArgs, @Nullable String sortOrder) throws RuntimeException {
        if (queryBuilder == null) {
            queryBuilder = SQLiteUtils.newSQLiteQueryBuilder();
        }
        queryBuilder.setTables(table);
        return queryBuilder.query(getDatabase(), columns, null, selectionArgs, null, null, sortOrder);
    }

    /**
     * Gets SQLite database.
     *
     * @return SQLite database.
     * @throws RuntimeException if an error occurs.
     */
    @VisibleForTesting
    SQLiteDatabase getDatabase() {

        /* Try opening database. */
        try {
            return mSQLiteOpenHelper.getWritableDatabase();
        } catch (RuntimeException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to open database. Trying to delete database (may be corrupted).", e);

            /* First error, try to delete database (may be corrupted). */
            if (mContext.deleteDatabase(mDatabase)) {
                AppCenterLog.info(LOG_TAG, "The database was successfully deleted.");
            } else {
                AppCenterLog.warn(LOG_TAG, "Failed to delete database.");
            }

            /* Retry, let exception thrown if it fails this time. */
            return mSQLiteOpenHelper.getWritableDatabase();
        }
    }

    /**
     * Sets {@link SQLiteOpenHelper} instance.
     *
     * @param helper A {@link SQLiteOpenHelper} instance to be used for accessing database.
     */
    @VisibleForTesting
    void setSQLiteOpenHelper(@NonNull SQLiteOpenHelper helper) {
        mSQLiteOpenHelper.close();
        mSQLiteOpenHelper = helper;
    }

    /**
     * Set maximum SQLite database size.
     *
     * @param maxStorageSizeInBytes Maximum SQLite database size.
     * @return true if database size was set, otherwise false.
     */
    public boolean setMaxSize(long maxStorageSizeInBytes) {
        try {
            SQLiteDatabase db = getDatabase();
            long newMaxSize = db.setMaximumSize(maxStorageSizeInBytes);

            /* SQLite always use the next multiple of page size as maximum size. */
            long pageSize = db.getPageSize();
            long expectedMultipleMaxSize = maxStorageSizeInBytes / pageSize;
            if (maxStorageSizeInBytes % pageSize != 0) {
                expectedMultipleMaxSize++;
            }
            expectedMultipleMaxSize *= pageSize;

            /* So to check the resize works, we need to check new max size against the next multiple of page size. */
            if (newMaxSize != expectedMultipleMaxSize) {
                AppCenterLog.error(LOG_TAG, "Could not change maximum database size to " + maxStorageSizeInBytes + " bytes, current maximum size is " + newMaxSize + " bytes.");
                return false;
            }
            if (maxStorageSizeInBytes == newMaxSize) {
                AppCenterLog.info(LOG_TAG, "Changed maximum database size to " + newMaxSize + " bytes.");
            } else {
                AppCenterLog.info(LOG_TAG, "Changed maximum database size to " + newMaxSize + " bytes (next multiple of page size).");
            }
            return true;
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Could not change maximum database size.", e);
            return false;
        }
    }

    /**
     * Gets the maximum size of the database.
     *
     * @return The maximum size of database in bytes.
     */
    public long getMaxSize() {
        try {
            return getDatabase().getMaximumSize();
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Could not get maximum database size.", e);
            return -1;
        }
    }

    /**
     * Database listener.
     */
    public interface Listener {

        /**
         * Called when the database has been created.
         *
         * @param db The database.
         **/
        void onCreate(SQLiteDatabase db);

        /**
         * Called when upgrade is performed on the database.
         * You can use this callback to alter table schema without losing data.
         * If schema is not migrated, return false and table will be recreated with new schema,
         * deleting old data.
         *
         * @param db         database being upgraded.
         * @param oldVersion version of the schema the database was at open time.
         * @param newVersion new version of the schema.
         */
        void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }
}
