package avalanche.core.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Database manager for SQLite with failover to in-memory.
 */
public class DatabaseManager implements Closeable {

    /**
     * Primary key name.
     */
    public static final String PRIMARY_KEY = "oid";

    /**
     * Selection (WHERE clause) pattern for primary key search.
     */
    private static final String PRIMARY_KEY_SELECTION = "oid = ?";

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
     * Maximum number of records allowed in the table.
     */
    private final int mMaxNumberOfRecords;

    /**
     * Error listener instance.
     */
    private final ErrorListener mErrorListener;

    /**
     * SQLite helper instance.
     */
    private final SQLiteOpenHelper mSQLiteOpenHelper;

    /**
     * In-memory database if SQLite cannot be used.
     */
    private Map<Long, ContentValues> mIMDB;

    /**
     * In-memory auto increment.
     */
    private long mIMDBAutoInc;

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
    protected DatabaseManager(Context context, String database, String table, int version,
                              ContentValues schema, ErrorListener listener) {
        this(context, database, table, version, schema, 0, listener);
    }

    /**
     * Initializes the table in the database.
     *
     * @param context    The application context.
     * @param database   The database name.
     * @param table      The table name.
     * @param version    The version of current schema.
     * @param schema     The schema.
     * @param maxRecords The maximum number of records allowed in the table. {@code 0} for no preset limit.
     * @param listener   The error listener.
     */
    DatabaseManager(Context context, String database, String table, int version,
                    ContentValues schema, int maxRecords, ErrorListener listener) {
        mContext = context;
        mDatabase = database;
        mTable = table;
        mSchema = schema;
        mMaxNumberOfRecords = maxRecords;
        mErrorListener = listener;

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
                    if (val instanceof Double || val instanceof Float)
                        sql.append("REAL");
                    else if (val instanceof Number || val instanceof Boolean)
                        sql.append("INTEGER");
                    else if (val instanceof byte[])
                        sql.append("BLOB");
                    else
                        sql.append("TEXT");
                }
                sql.append(");");
                db.execSQL(sql.toString());
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                /* For now we upgrade by destroying the old table. */
                db.execSQL("DROP TABLE `" + mTable + "`");
                onCreate(db);
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
            if (cursor.isNull(i))
                continue;
            String key = cursor.getColumnName(i);
            if (key.equals(PRIMARY_KEY))
                values.put(key, cursor.getLong(i));
            else {
                Object specimen = schema.get(key);
                if (specimen instanceof byte[])
                    values.put(key, cursor.getBlob(i));
                else if (specimen instanceof Double)
                    values.put(key, cursor.getDouble(i));
                else if (specimen instanceof Float)
                    values.put(key, cursor.getFloat(i));
                else if (specimen instanceof Integer)
                    values.put(key, cursor.getInt(i));
                else if (specimen instanceof Long)
                    values.put(key, cursor.getLong(i));
                else if (specimen instanceof Short)
                    values.put(key, cursor.getShort(i));
                else if (specimen instanceof Boolean)
                    values.put(key, cursor.getInt(i) == 1);
                else
                    values.put(key, cursor.getString(i));
            }
        }
        return values;
    }

    /**
     * Stores the entry to the table.
     *
     * @param values The entry to be stored.
     * @return A database identifier
     */
    public long put(@NonNull ContentValues values) {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                /* Insert data. */
                long id = getDatabase().insertOrThrow(mTable, null, values);

                /* Purge oldest entry if it hits the limit. */
                if (mMaxNumberOfRecords < getRowCount() && mMaxNumberOfRecords > 0) {
                    Cursor cursor = getCursor(null, null);
                    cursor.moveToNext();
                    delete(cursor.getLong(0));
                    cursor.close();
                }

                return id;
            } catch (RuntimeException e) {
                switchToInMemory("put", e);
            }
        }

        /* Store the values to in-memory database. */
        values.put(PRIMARY_KEY, mIMDBAutoInc);
        mIMDB.put(mIMDBAutoInc, values);
        return mIMDBAutoInc++;
    }

    /**
     * Updates the entry for the identifier.
     *
     * @param id     The existing database identifier.
     * @param values The entry to be updated.
     * @return true if the values updated successfully, false otherwise.
     */
    public boolean update(@IntRange(from = 0) long id, @NonNull ContentValues values) {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                return 0 < getDatabase().update(mTable, values, PRIMARY_KEY_SELECTION, new String[]{String.valueOf(id)});
            } catch (RuntimeException e) {
                switchToInMemory("update", e);
            }
        }

        /* Updates the values in in-memory database if the identifier exists there. */
        ContentValues existValues = mIMDB.get(id);
        if (existValues == null)
            return false;
        existValues.putAll(values);
        return true;
    }

    /**
     * Deletes the entry by the identifier from the database.
     *
     * @param id The database identifier.
     */
    public void delete(@IntRange(from = 0) long id) {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                getDatabase().delete(mTable, PRIMARY_KEY_SELECTION, new String[]{String.valueOf(id)});
            } catch (RuntimeException e) {
                switchToInMemory("delete", e);
            }
        }

        /* Deletes the values from in-memory database. */
        else {
            mIMDB.remove(id);
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
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                Cursor cursor = getCursor(key, value);
                ContentValues values = cursor.moveToFirst() ? buildValues(cursor, mSchema) : null;
                cursor.close();
                return values;
            } catch (RuntimeException e) {
                switchToInMemory("get", e);
            }
        }

        /* Get the values from in-memory database. */
        else if (PRIMARY_KEY.equals(key)) {
            if (value == null || !(value instanceof Number)) {
                throw new IllegalArgumentException("Primary key should be a number type and cannot be null");
            }
            return mIMDB.get(((Number) value).longValue());
        } else {
            for (ContentValues values : mIMDB.values()) {
                Object object = values.get(key);
                if (object != null && object.equals(value))
                    return values;
            }
        }

        return null;
    }

    /**
     * Gets a scanner to iterate all values those match key == value.
     *
     * @param key   The optional key for query.
     * @param value The optional value for query.
     * @return A scanner to iterate all values.
     */
    public Scanner getScanner(String key, Object value) {
        return new Scanner(key, value);
    }

    /**
     * Clears the table in the database.
     */
    public void clear() {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                getDatabase().delete(mTable, null, null);
            } catch (RuntimeException e) {
                switchToInMemory("clear", e);
            }
        }

        /* Clear in-memory database. */
        else {
            mIMDB.clear();
        }
    }

    /**
     * Closes database and clean up in-memory database.
     *
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                getDatabase().close();
            } catch (RuntimeException e) {
                switchToInMemory("close", e);
            }
        }

        /* Close in-memory database. */
        else {
            mIMDB.clear();
            mIMDB = null;
        }
    }

    /**
     * Gets the count of records in the table.
     *
     * @return The number of records in the table.
     */
    long getRowCount() {
        /* Try SQLite. */
        if (mIMDB == null) {
            try {
                return DatabaseUtils.queryNumEntries(getDatabase(), mTable);
            } catch (RuntimeException e) {
                switchToInMemory("count", e);
            }
        }

        /* Get row count of in-memory database. */
        return mIMDB.size();
    }

    /**
     * Gets a cursor for all rows in the table, all rows where key matches value if specified.
     *
     * @param key   The optional key for query.
     * @param value The optional value for query.
     * @return A cursor for all rows that matches the given criteria.
     * @throws RuntimeException If an error occurs.
     */
    Cursor getCursor(String key, Object value) throws RuntimeException {
        /* Build a query to get values. */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(mTable);
        String[] selectionArgs;
        if (key == null)
            selectionArgs = null;
        else {
            builder.appendWhere(key + " = ?");
            selectionArgs = new String[]{String.valueOf(value.toString())};
        }

        /* Query database. */
        return builder.query(getDatabase(), null, null, selectionArgs, null, null, PRIMARY_KEY);
    }

    /**
     * Gets SQLite database.
     *
     * @return SQLite database.
     * @throws RuntimeException if an error occurs.
     */
    private SQLiteDatabase getDatabase() throws RuntimeException {
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
     * Switches to in-memory management, triggers error listener.
     *
     * @param operation The operation that triggered the error.
     * @param exception The exception that triggered the switch.
     */
    private void switchToInMemory(String operation, RuntimeException exception) {
        /* Create an in-memory database. */
        mIMDB = new LinkedHashMap<Long, ContentValues>() {
            @Override
            protected boolean removeEldestEntry(Entry<Long, ContentValues> eldest) {
                return mMaxNumberOfRecords < size() && mMaxNumberOfRecords > 0;
            }
        };

        /* Trigger error listener. */
        if (mErrorListener != null)
            mErrorListener.onError(operation, exception);
    }

    /**
     * Error listener for DatabaseManager.
     */
    interface ErrorListener {
        /**
         * Notifies an exception
         */
        void onError(String operation, RuntimeException e);
    }

    /**
     * Scanner specification.
     */
    public class Scanner implements Iterable<ContentValues>, Closeable {
        /**
         * Filter key.
         */
        private final String key;

        /**
         * Filter value.
         */
        private final Object value;

        /**
         * SQLite cursor.
         */
        private Cursor cursor;

        /**
         * Initializes a cursor with optional filter.
         */
        private Scanner(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void close() {
            /* Close cursor. */
            if (cursor != null)
                try {
                    cursor.close();
                } catch (RuntimeException e) {
                    switchToInMemory("scan", e);
                }
        }

        @Override
        public Iterator<ContentValues> iterator() {
            /* Try SQLite. */
            if (mIMDB == null) {
                try {
                    /* Close cursor first if it was being used. */
                    close();
                    cursor = getCursor(key, value);

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
                                    cursor.close();

                                    /* Switch to in-memory database. */
                                    switchToInMemory("scan", e);
                                }
                            }
                            return hasNext;
                        }

                        @Override
                        public ContentValues next() {
                            /* Check next. */
                            if (!hasNext())
                                throw new NoSuchElementException();
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
                    switchToInMemory("scan", e);
                }
            }

            /* Scanner for in-memory database. */
            return new Iterator<ContentValues>() {

                /** In memory map iterator that we wrap because of the filter logic. */
                final Iterator<ContentValues> iterator = mIMDB.values().iterator();

                /** True if we moved the iterator but not retrieved the value. */
                boolean advanced;

                /** Next value. */
                ContentValues next;

                @Override
                public boolean hasNext() {
                    /* Iterator needs to be moved to the next. */
                    if (!advanced) {
                        next = null;
                        while (iterator.hasNext()) {
                            ContentValues nextCandidate = iterator.next();
                            if (key == null || value.equals(nextCandidate.get(key))) {
                                next = nextCandidate;
                                break;
                            }
                        }
                        advanced = true;
                    }
                    return next != null;
                }

                @Override
                public ContentValues next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    advanced = false;
                    return next;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
