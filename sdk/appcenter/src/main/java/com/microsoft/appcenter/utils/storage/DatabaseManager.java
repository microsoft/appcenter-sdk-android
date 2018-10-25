package com.microsoft.appcenter.utils.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
     * Allowed multiple for maximum sizes.
     */
    @VisibleForTesting
    static final int ALLOWED_SIZE_MULTIPLE = 4096;

    /**
     * Application context instance.
     */
    private final Context mContext;

    /**
     * Database name.
     */
    private final String mDatabase;

    /**
     * Table name.
     */
    private final String mTable;

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
     * @param context  The application context.
     * @param database The database name.
     * @param table    The table name.
     * @param version  The version of current schema.
     * @param schema   The schema.
     * @param listener The error listener.
     */
    public DatabaseManager(Context context, String database, String table, int version,
                           ContentValues schema, Listener listener) {
        mContext = context;
        mDatabase = database;
        mTable = table;
        mSchema = schema;
        mListener = listener;
        mSQLiteOpenHelper = new SQLiteOpenHelper(context, database, null, version) {

            @Override
            public void onCreate(SQLiteDatabase db) {

                /* Generate a schema from specimen. */
                StringBuilder sql = new StringBuilder("CREATE TABLE `");
                sql.append(mTable);
                sql.append("` (oid INTEGER PRIMARY KEY AUTOINCREMENT");
                for (Map.Entry<String, Object> col : mSchema.valueSet()) {
                    sql.append(", `").append(col.getKey()).append("` ");
                    Object val = col.getValue();
                    if (val instanceof Double || val instanceof Float) {
                        sql.append("REAL");
                    } else if (val instanceof Number || val instanceof Boolean) {
                        sql.append("INTEGER");
                    } else if (val instanceof byte[]) {
                        sql.append("BLOB");
                    } else {
                        sql.append("TEXT");
                    }
                }
                sql.append(");");
                db.execSQL(sql.toString());
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                /* Upgrade by destroying the old table unless managed. */
                if (!mListener.onUpgrade(db, oldVersion, newVersion)) {
                    db.execSQL("DROP TABLE `" + mTable + "`");
                    onCreate(db);
                }
            }
        };
    }

    /**
     * Converts a cursor to an entry.
     *
     * @param cursor The cursor to be converted to an entry.
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
     * Stores the entry to the table. If the table is full, the oldest logs are discarded until the
     * new one can fit. If the log is larger than the max table size, database will be cleared and
     * the log is not inserted.
     *
     * @param values The entry to be stored.
     * @return If a log was inserted, the database identifier. Otherwise -1.
     */
    @SuppressWarnings("TryFinallyCanBeTryWithResources")
    public long put(@NonNull ContentValues values) {
        try {
            while (true) {
                try {

                    /* Insert data. */
                    return getDatabase().insertOrThrow(mTable, null, values);
                } catch (SQLiteFullException e) {

                    /* Delete the oldest log. */
                    Cursor cursor = getCursor(null, null, null, null, true);
                    try {
                        if (cursor.moveToNext()) {
                            delete(cursor.getLong(0));
                        } else {
                            return -1;
                        }
                    } finally {
                        cursor.close();
                    }
                }
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, String.format("Failed to insert values (%s) to database.", values.toString()), e);
        }
        return -1;
    }

    /**
     * Deletes the entry by the identifier from the database.
     *
     * @param id The database identifier.
     */
    public void delete(@IntRange(from = 0) long id) {
        delete(PRIMARY_KEY, id);
    }

    /**
     * Deletes the entries by the identifier from the database.
     *
     * @param idList The list of database identifiers.
     */
    public void delete(@NonNull List<Long> idList) {
        if (idList.size() <= 0) {
            return;
        }
        try {
            getDatabase().execSQL(String.format("DELETE FROM " + mTable + " WHERE " + PRIMARY_KEY + " IN (%s);", TextUtils.join(", ", idList)));
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, String.format("Failed to delete IDs (%s) from database.", Arrays.toString(idList.toArray())), e);
        }
    }

    /**
     * Deletes the entries that matches key == value.
     *
     * @param key   The optional key for query.
     * @param value The optional value for query.
     */
    public void delete(@Nullable String key, @Nullable Object value) {
        try {
            getDatabase().delete(mTable, key + " = ?", new String[]{String.valueOf(value)});
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, String.format("Failed to delete values that match key=\"%s\" and value=\"%s\" from database.", key, value), e);
        }
    }

    /**
     * Gets the entry by the identifier.
     *
     * @param id The database identifier.
     * @return An entry for the identifier or null if not found.
     */
    public ContentValues get(@IntRange(from = 0) long id) {
        return get(PRIMARY_KEY, id);
    }

    /**
     * Gets the entry that matches key == value.
     *
     * @param key   The optional key for query.
     * @param value The optional value for query.
     * @return A matching entry.
     */
    public ContentValues get(@Nullable String key, @Nullable Object value) {
        try {
            Cursor cursor = getCursor(key, value, null, null, false);
            ContentValues values = cursor.moveToFirst() ? buildValues(cursor, mSchema) : null;
            cursor.close();
            return values;
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, String.format("Failed to get values that match key=\"%s\" and value=\"%s\" from database.", key, value), e);
        }
        return null;
    }

    /**
     * Gets a scanner to iterate all values those match
     * key1 == value1 and key2 not matching any values from the list in value2Filter.
     *
     * @param key1         The optional key1 for query.
     * @param value1       The optional value1 for query.
     * @param key2         The optional key2 to filter the query.
     * @param value2Filter The optional value filter for key2.
     * @param idOnly       true to return only identifier, false to return all fields.
     * @return A scanner to iterate all values.
     */
    public Scanner getScanner(String key1, Object value1, String key2, Collection<String> value2Filter, boolean idOnly) {
        return new Scanner(key1, value1, key2, value2Filter, idOnly);
    }

    /**
     * Clears the table in the database.
     */
    public void clear() {
        try {
            getDatabase().delete(mTable, null, null);
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Failed to clear the table.", e);
        }
    }

    /**
     * Closes database.
     */
    @Override
    public void close() {
        try {
            getDatabase().close();
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Failed to close the database.", e);
        }
    }

    /**
     * Gets the count of records in the table.
     *
     * @return The number of records in the table, or <code>-1</code> if operation failed.
     */
    public final long getRowCount() {
        try {
            return DatabaseUtils.queryNumEntries(getDatabase(), mTable);
        } catch (RuntimeException e) {
            AppCenterLog.error(AppCenter.LOG_TAG, "Failed to get row count of database.", e);
        }
        return -1;
    }

    /**
     * Gets a cursor for all rows in the table, all rows where key matches value if specified.
     *
     * @param key1         The first key to match values against.
     * @param value1       The value to match against first key.
     * @param key2         The second key to match values against.
     * @param value2Filter The list of values to exclude matching the second key.
     * @param idOnly       Return only row identifier if true, return all fields otherwise.
     * @return A cursor for all rows that matches the given criteria.
     * @throws RuntimeException If an error occurs.
     */
    Cursor getCursor(String key1, Object value1, String key2, Collection<String> value2Filter, boolean idOnly) throws RuntimeException {

        /* Build a query to get values. */
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.setTables(mTable);
        List<String> selectionArgsList = new ArrayList<>();

        /* Add filter for key1 = value1, with null value matching. */
        if (key1 != null) {
            if (value1 == null) {
                builder.appendWhere(key1 + " IS NULL");
            } else {
                builder.appendWhere(key1 + " = ?");
                selectionArgsList.add(value1.toString());
            }
        }

        /* Append value filter to exclude values matching key2, if key2 and value2Filter were both specified. */
        if (key2 != null && value2Filter != null && !value2Filter.isEmpty()) {
            if (key1 != null) {
                builder.appendWhere(" AND ");
            }
            builder.appendWhere(key2);
            builder.appendWhere(" NOT IN (");
            StringBuilder inBuilder = new StringBuilder();
            for (String value2 : value2Filter) {
                inBuilder.append("?,");
                selectionArgsList.add(value2);
            }
            inBuilder.deleteCharAt(inBuilder.length() - 1);
            builder.appendWhere(inBuilder.toString());
            builder.appendWhere(")");
        }

        /* Convert list to array. */
        String[] selectionArgs;
        if (selectionArgsList.isEmpty()) {
            selectionArgs = null;
        } else {
            selectionArgs = selectionArgsList.toArray(new String[selectionArgsList.size()]);
        }

        /* Query database. */
        String[] projectionIn = idOnly ? new String[]{PRIMARY_KEY} : null;
        return builder.query(getDatabase(), projectionIn, null, selectionArgs, null, null, PRIMARY_KEY);
    }

    /**
     * Gets SQLite database.
     *
     * @return SQLite database.
     * @throws RuntimeException if an error occurs.
     */
    @VisibleForTesting
    SQLiteDatabase getDatabase() throws RuntimeException {

        /* Try opening database. */
        try {
            return mSQLiteOpenHelper.getWritableDatabase();
        } catch (RuntimeException e) {

            /* First error, try to delete database (may be corrupted). */
            mContext.deleteDatabase(mDatabase);

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
     * Gets an array of column names in the table.
     *
     * @return An array of column names.
     */
    @VisibleForTesting
    String[] getColumnNames() {
        return getCursor(null, null, null, null, false).getColumnNames();
    }

    /**
     * Set maximum SQLite database size.
     *
     * @param maxStorageSizeInBytes Maximum SQLite database size.
     * @return true if database size was set, otherwise false.
     */
    public boolean setMaxSize(long maxStorageSizeInBytes) {
        SQLiteDatabase db = getDatabase();
        long newMaxSize = db.setMaximumSize(maxStorageSizeInBytes);

        /* SQLite always use the next multiple of 4KB as maximum size. */
        long expectedMultipleMaxSize = (long) Math.ceil((double) maxStorageSizeInBytes / (double) ALLOWED_SIZE_MULTIPLE) * ALLOWED_SIZE_MULTIPLE;

        /* So to check the resize works, we need to check new max size against the next multiple of 4KB. */
        if (newMaxSize != expectedMultipleMaxSize) {
            AppCenterLog.error(LOG_TAG, "Could not change maximum database size to " + maxStorageSizeInBytes + " bytes, current maximum size is " + newMaxSize + " bytes.");
            return false;
        }
        if (maxStorageSizeInBytes == newMaxSize) {
            AppCenterLog.info(LOG_TAG, "Changed maximum database size to " + newMaxSize + " bytes.");
        } else {
            AppCenterLog.info(LOG_TAG, "Changed maximum database size to " + newMaxSize + " bytes (next multiple of 4KiB).");
        }
        return true;
    }

    /**
     * Gets the maximum size of the database.
     *
     * @return The maximum size of database in bytes.
     */
    public long getMaxSize() {
        return getDatabase().getMaximumSize();
    }

    /**
     * Database listener.
     */
    public interface Listener {

        /**
         * Called when upgrade is performed on the database.
         * You can use this callback to alter table schema without losing data.
         * If schema is not migrated, return false and table will be recreated with new schema,
         * deleting old data.
         *
         * @param db         database being upgraded.
         * @param oldVersion version of the schema the database was at open time.
         * @param newVersion new version of the schema.
         * @return true if upgrade was managed, false to drop/create table.
         */
        @SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused"})
        boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
    }

    /**
     * Scanner specification.
     */
    public class Scanner implements Iterable<ContentValues>, Closeable {

        /**
         * First filter key.
         */
        private final String key1;

        /**
         * Filter value for key1.
         */
        private final Object value1;

        /**
         * Second filter key.
         */
        private final String key2;

        /**
         * Filter values to exclude matching key2.
         */
        private final Collection<String> value2Filter;

        /**
         * Return only IDs flags (SQLite implementation only).
         */
        private final boolean idOnly;

        /**
         * SQLite cursor.
         */
        private Cursor cursor;

        /**
         * Initializes a cursor with optional filter.
         */
        private Scanner(String key1, Object value1, String key2, Collection<String> value2Filter, boolean idOnly) {
            this.key1 = key1;
            this.value1 = value1;
            this.key2 = key2;
            this.value2Filter = value2Filter;
            this.idOnly = idOnly;
        }

        @Override
        public void close() {

            /* Close cursor. */
            if (cursor != null) {
                try {
                    cursor.close();
                    cursor = null;
                } catch (RuntimeException e) {
                    AppCenterLog.error(AppCenter.LOG_TAG, "Failed to close the scanner.", e);
                }
            }
        }

        @NonNull
        @Override
        public Iterator<ContentValues> iterator() {
            try {

                /* Close cursor first if it was being used. */
                close();
                cursor = getCursor(key1, value1, key2, value2Filter, idOnly);

                /* Wrap cursor as iterator. */
                return new Iterator<ContentValues>() {

                    /**
                     * If null, cursor needs to be moved to next.
                     */
                    Boolean hasNext;

                    @Override
                    public boolean hasNext() {
                        if (hasNext == null) {
                            try {
                                hasNext = cursor.moveToNext();
                            } catch (RuntimeException e) {

                                /* Consider no next on errors. */
                                hasNext = false;

                                /* Close cursor. */
                                try {
                                    cursor.close();
                                } catch (RuntimeException e1) {
                                    AppCenterLog.warn(AppCenter.LOG_TAG, "Closing cursor failed", e1);
                                }
                                cursor = null;
                            }
                        }
                        return hasNext;
                    }

                    @Override
                    public ContentValues next() {

                        /* Check next. */
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        hasNext = null;

                        /* Build object. */
                        return buildValues(cursor, mSchema);
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            } catch (RuntimeException e) {
                AppCenterLog.error(AppCenter.LOG_TAG, "Failed to get iterator of the scanner.", e);
            }
            return Collections.<ContentValues>emptyList().iterator();
        }

        public int getCount() {
            try {
                if (cursor == null) {
                    cursor = getCursor(key1, value1, key2, value2Filter, idOnly);
                }
                return cursor.getCount();
            } catch (RuntimeException e) {
                AppCenterLog.error(AppCenter.LOG_TAG, "Failed to get count of the scanner.", e);
            }
            return -1;
        }
    }
}
