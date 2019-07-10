/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.persistence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SQLiteUtils;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Flags.NORMAL;
import static com.microsoft.appcenter.utils.storage.DatabaseManager.PRIMARY_KEY;
import static com.microsoft.appcenter.utils.storage.DatabaseManager.SELECT_PRIMARY_KEY;

@SuppressWarnings("TryFinallyCanBeTryWithResources")
public class DatabasePersistence extends Persistence {

    /**
     * Version of the schema that introduced api key and type field.
     */
    @VisibleForTesting
    static final int VERSION_TYPE_API_KEY = 2;

    /**
     * Version of the schema that introduced target key field.
     */
    @VisibleForTesting
    static final int VERSION_TARGET_KEY = 3;

    /**
     * Version of the schema that introduced persistence priority for logs.
     */
    @VisibleForTesting
    static final int VERSION_PRIORITY_KEY = 4;

    /**
     * Table name.
     */
    @VisibleForTesting
    static final String TABLE = "logs";

    /**
     * Name of group column in the table.
     */
    @VisibleForTesting
    static final String COLUMN_GROUP = "persistence_group";

    /**
     * Name of log column in the table.
     */
    @VisibleForTesting
    static final String COLUMN_LOG = "log";

    /**
     * Name of date column in the table.
     * Value is stored in milliseconds.
     */
    @VisibleForTesting
    static final String COLUMN_TIMESTAMP = "timestamp";

    /**
     * Name of target token column in the table.
     */
    @VisibleForTesting
    static final String COLUMN_TARGET_TOKEN = "target_token";

    /**
     * Name of target token column in the table.
     */
    @VisibleForTesting
    static final String COLUMN_DATA_TYPE = "type";

    /**
     * Project identifier part of the target token in clear text (the target token key).
     */
    @VisibleForTesting
    static final String COLUMN_TARGET_KEY = "target_key";

    /**
     * Priority.
     */
    @VisibleForTesting
    static final String COLUMN_PRIORITY = "priority";

    /**
     * Table schema for Persistence.
     */
    @VisibleForTesting
    static final ContentValues SCHEMA = getContentValues("", "", "", "", "", 0, 0L);

    /**
     * Database name.
     */
    @VisibleForTesting
    static final String DATABASE = "com.microsoft.appcenter.persistence";

    /**
     * Current version of the schema.
     */
    private static final int VERSION = 5;

    /**
     * Priority index.
     */
    private static final String INDEX_PRIORITY = "ix_" + TABLE + "_" + COLUMN_PRIORITY;

    /**
     * Order by clause to select logs.
     */
    private static final String GET_SORT_ORDER = COLUMN_PRIORITY + " DESC, " + PRIMARY_KEY;

    /**
     * Size limit (in bytes) for a database row log payload.
     * A separate file is used if payload is larger.
     */
    private static final int PAYLOAD_MAX_SIZE = (int) (1.9 * 1024 * 1024);

    /**
     * Sub path for directory where to store large payloads.
     */
    private static final String PAYLOAD_LARGE_DIRECTORY = "/appcenter/database_large_payloads";

    /**
     * Large payload file extension.
     */
    private static final String PAYLOAD_FILE_EXTENSION = ".json";

    /**
     * Database manager instance to access Persistence database.
     */
    @VisibleForTesting
    final DatabaseManager mDatabaseManager;

    /**
     * Pending log groups. Key is a UUID and value is a list of database identifiers.
     */
    @VisibleForTesting
    final Map<String, List<Long>> mPendingDbIdentifiersGroups;

    /**
     * Pending logs across all groups.
     */
    @VisibleForTesting
    final Set<Long> mPendingDbIdentifiers;

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * Base directory to store large payloads outside of SQLite.
     */
    private final File mLargePayloadDirectory;

    /**
     * Initializes variables with default values.
     *
     * @param context application context.
     */
    public DatabasePersistence(Context context) {
        this(context, VERSION, SCHEMA);
    }

    /**
     * Initializes variables.
     *
     * @param context application context.
     * @param version The version of current schema.
     * @param schema  schema.
     */
    DatabasePersistence(Context context, int version, @SuppressWarnings("SameParameterValue") ContentValues schema) {
        mContext = context;
        mPendingDbIdentifiersGroups = new HashMap<>();
        mPendingDbIdentifiers = new HashSet<>();
        mDatabaseManager = new DatabaseManager(context, DATABASE, TABLE, version, schema, new DatabaseManager.Listener() {

            private void createPriorityIndex(SQLiteDatabase db) {
                db.execSQL("CREATE INDEX `" + INDEX_PRIORITY + "` ON " + TABLE + " (`" + COLUMN_PRIORITY + "`)");
            }

            @Override
            public void onCreate(SQLiteDatabase db) {
                createPriorityIndex(db);
            }

            @Override
            public boolean onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

                /* Update columns only if needed, this callback is called only on upgrade. */
                if (oldVersion < VERSION_TYPE_API_KEY) {
                    db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN `" + COLUMN_TARGET_TOKEN + "` TEXT");
                    db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN `" + COLUMN_DATA_TYPE + "` TEXT");
                }
                if (oldVersion < VERSION_TARGET_KEY) {
                    db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN `" + COLUMN_TARGET_KEY + "` TEXT");
                }
                if (oldVersion < VERSION_PRIORITY_KEY) {
                    db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN `" + COLUMN_PRIORITY + "` INTEGER DEFAULT " + NORMAL);
                }
                db.execSQL("ALTER TABLE " + TABLE + " ADD COLUMN `" + COLUMN_TIMESTAMP + "` INTEGER DEFAULT 0");
                createPriorityIndex(db);
                return true;
            }
        });
        mLargePayloadDirectory = new File(Constants.FILES_PATH + PAYLOAD_LARGE_DIRECTORY);

        //noinspection ResultOfMethodCallIgnored we handle errors at read/write time for each file.
        mLargePayloadDirectory.mkdirs();
    }

    /**
     * Instantiates {@link ContentValues} with the give values.
     *
     * @param group       The group of the storage for the log.
     * @param logJ        The JSON string for a log.
     * @param targetToken The target token if the log is common schema.
     * @param targetKey   The project identifier part of the target token in clear text.
     * @param priority    The persistence priority.
     * @return A {@link ContentValues} instance.
     */
    private static ContentValues getContentValues(@Nullable String group, @Nullable String logJ, String targetToken, String type, String targetKey, int priority, Long timestamp) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP, group);
        values.put(COLUMN_LOG, logJ);
        values.put(COLUMN_TARGET_TOKEN, targetToken);
        values.put(COLUMN_DATA_TYPE, type);
        values.put(COLUMN_TARGET_KEY, targetKey);
        values.put(COLUMN_PRIORITY, priority);
        values.put(COLUMN_TIMESTAMP, timestamp);
        return values;
    }

    @Override
    public boolean setMaxStorageSize(long maxStorageSizeInBytes) {
        return mDatabaseManager.setMaxSize(maxStorageSizeInBytes);
    }

    @Override
    public long putLog(@NonNull Log log, @NonNull String group, @IntRange(from = Flags.NORMAL, to = Flags.CRITICAL) int flags) throws PersistenceException {

        /* Convert log to JSON string and put in the database. */
        try {
            AppCenterLog.debug(LOG_TAG, "Storing a log to the Persistence database for log type " + log.getType() + " with flags=" + flags);
            String payload = getLogSerializer().serializeLog(log);
            ContentValues contentValues;

            //noinspection CharsetObjectCanBeUsed min API level 19 required to fix this warning.
            int payloadSize = payload.getBytes("UTF-8").length;
            boolean isLargePayload = payloadSize >= PAYLOAD_MAX_SIZE;
            String targetKey;
            String targetToken;
            if (log instanceof CommonSchemaLog) {
                if (isLargePayload) {
                    throw new PersistenceException("Log is larger than " + PAYLOAD_MAX_SIZE + " bytes, cannot send to OneCollector.");
                }
                targetToken = log.getTransmissionTargetTokens().iterator().next();
                targetKey = PartAUtils.getTargetKey(targetToken);
                targetToken = CryptoUtils.getInstance(mContext).encrypt(targetToken);
            } else {
                targetKey = null;
                targetToken = null;
            }
            long maxSize = mDatabaseManager.getMaxSize();
            if (maxSize == -1) {
                throw new PersistenceException("Failed to store a log to the Persistence database.");
            }
            if (!isLargePayload && maxSize <= payloadSize) {
                throw new PersistenceException("Log is too large (" + payloadSize + " bytes) to store in database. " +
                        "Current maximum database size is " + maxSize + " bytes.");
            }
            contentValues = getContentValues(group, isLargePayload ? null : payload, targetToken, log.getType(), targetKey, Flags.getPersistenceFlag(flags, false), log.getTimestamp().getTime());
            long databaseId = mDatabaseManager.put(contentValues, COLUMN_PRIORITY);
            if (databaseId == -1) {
                throw new PersistenceException("Failed to store a log to the Persistence database for log type " + log.getType() + ".");
            }
            AppCenterLog.debug(LOG_TAG, "Stored a log to the Persistence database for log type " + log.getType() + " with databaseId=" + databaseId);
            if (isLargePayload) {
                AppCenterLog.debug(LOG_TAG, "Payload is larger than what SQLite supports, storing payload in a separate file.");
                File directory = getLargePayloadGroupDirectory(group);

                //noinspection ResultOfMethodCallIgnored we'll get an error anyway at write time.
                directory.mkdir();
                File payloadFile = getLargePayloadFile(directory, databaseId);
                try {
                    FileManager.write(payloadFile, payload);
                } catch (IOException e) {

                    /* Remove database entry if we cannot save payload as a file. */
                    mDatabaseManager.delete(databaseId);
                    throw e;
                }
                AppCenterLog.debug(LOG_TAG, "Payload written to " + payloadFile);
            }
            return databaseId;
        } catch (JSONException e) {
            throw new PersistenceException("Cannot convert to JSON string.", e);
        } catch (IOException e) {
            throw new PersistenceException("Cannot save large payload in a file.", e);
        }
    }

    @NonNull
    @VisibleForTesting
    File getLargePayloadGroupDirectory(String group) {
        return new File(mLargePayloadDirectory, group);
    }

    @NonNull
    @VisibleForTesting
    File getLargePayloadFile(File directory, long databaseId) {
        return new File(directory, databaseId + PAYLOAD_FILE_EXTENSION);
    }

    private void deleteLog(File groupLargePayloadDirectory, long id) {

        //noinspection ResultOfMethodCallIgnored SQLite delete does not have return type either.
        getLargePayloadFile(groupLargePayloadDirectory, id).delete();
        mDatabaseManager.delete(id);
    }

    @Override
    public void deleteLogs(@NonNull String group, @NonNull String id) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Deleting logs from the Persistence database for " + group + " with " + id);
        AppCenterLog.debug(LOG_TAG, "The IDs for deleting log(s) is/are:");

        /* Delete logs. */
        List<Long> dbIdentifiers = mPendingDbIdentifiersGroups.remove(group + id);
        File directory = getLargePayloadGroupDirectory(group);
        if (dbIdentifiers != null) {
            for (Long dbIdentifier : dbIdentifiers) {
                AppCenterLog.debug(LOG_TAG, "\t" + dbIdentifier);
                deleteLog(directory, dbIdentifier);
                mPendingDbIdentifiers.remove(dbIdentifier);
            }
        }
    }

    @Override
    public void deleteLogs(String group) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Deleting all logs from the Persistence database for " + group);

        /* Delete large payload files. */
        File directory = getLargePayloadGroupDirectory(group);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {

                //noinspection ResultOfMethodCallIgnored we are not checking SQLite result either.
                file.delete();
            }
        }

        //noinspection ResultOfMethodCallIgnored we are not checking SQLite result either.
        directory.delete();

        /* Delete from database. */
        int deletedCount = mDatabaseManager.delete(COLUMN_GROUP, group);
        AppCenterLog.debug(LOG_TAG, "Deleted " + deletedCount + " logs.");

        /* Delete from pending state. */
        for (Iterator<String> iterator = mPendingDbIdentifiersGroups.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (key.startsWith(group)) {
                iterator.remove();
            }
        }
    }

    @Override
    public int countLogs(@NonNull String group) {
        return countLogs(COLUMN_GROUP + " = ?", group);
    }

    @Override
    public int countLogs(@NonNull Date timestamp) {
        return countLogs(COLUMN_TIMESTAMP + " < ?", String.valueOf(timestamp.getTime()));
    }

    private int countLogs(String whereClause, String... whereArgs) {

        /* Query database and get scanner. */
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(whereClause);
        int count = 0;
        try {
            Cursor cursor = mDatabaseManager.getCursor(builder, new String[]{"COUNT(*)"}, whereArgs, null);
            try {
                cursor.moveToNext();
                count = cursor.getInt(0);
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to get logs count: ", e);
        }
        return count;
    }

    @Override
    @Nullable
    public String getLogs(@NonNull String group, @NonNull Collection<String> pausedTargetKeys, @IntRange(from = 0) int limit, @NonNull List<Log> outLogs, @Nullable Date from, @Nullable Date to) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Trying to get " + limit + " logs from the Persistence database for " + group);

        /* Query database. */
        SQLiteQueryBuilder builder = SQLiteUtils.newSQLiteQueryBuilder();
        builder.appendWhere(COLUMN_GROUP + " = ?");
        List<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(group);
        if (!pausedTargetKeys.isEmpty()) {
            StringBuilder filter = new StringBuilder();
            for (int i = 0; i < pausedTargetKeys.size(); i++) {
                filter.append("?,");
            }
            filter.deleteCharAt(filter.length() - 1);
            builder.appendWhere(" AND ");
            builder.appendWhere(COLUMN_TARGET_KEY + " NOT IN (" + filter.toString() + ")");
            selectionArgs.addAll(pausedTargetKeys);
        }

        /* Filter by time. */
        if (from != null) {
            builder.appendWhere(" AND ");
            builder.appendWhere(COLUMN_TIMESTAMP + " >= ?");
            selectionArgs.add(String.valueOf(from.getTime()));
        }
        if (to != null) {
            builder.appendWhere(" AND ");
            builder.appendWhere(COLUMN_TIMESTAMP + " < ?");
            selectionArgs.add(String.valueOf(to.getTime()));
        }

        /* Add logs to output parameter after deserialization if logs are not already sent. */
        int count = 0;
        Map<Long, Log> candidates = new LinkedHashMap<>();
        List<Long> failedDbIdentifiers = new ArrayList<>();
        File largePayloadGroupDirectory = getLargePayloadGroupDirectory(group);
        String[] selectionArgsArray = selectionArgs.toArray(new String[0]);
        Cursor cursor = null;
        ContentValues values;
        try {
            cursor = mDatabaseManager.getCursor(builder, null, selectionArgsArray, GET_SORT_ORDER);
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to get logs: ", e);
        }
        while (cursor != null &&
                (values = mDatabaseManager.nextValues(cursor)) != null &&
                count < limit) {
            Long dbIdentifier = values.getAsLong(PRIMARY_KEY);

            /*
             * When we can't even read the identifier (in this case ContentValues is most likely empty).
             * That probably means it contained a record larger than 2MB (from a previous SDK version)
             * and we hit the cursor limit.
             * Get rid of first non pending log.
             */
            if (dbIdentifier == null) {
                AppCenterLog.error(LOG_TAG, "Empty database record, probably content was larger than 2MB, need to delete as it's now corrupted.");
                List<Long> corruptedIds = getLogsIds(builder, selectionArgsArray);
                for (Long corruptedId : corruptedIds) {
                    if (!mPendingDbIdentifiers.contains(corruptedId) && !candidates.containsKey(corruptedId)) {

                        /* Found the record to delete that we could not read when selecting all fields. */
                        deleteLog(largePayloadGroupDirectory, corruptedId);
                        AppCenterLog.error(LOG_TAG, "Empty database corrupted empty record deleted, id=" + corruptedId);
                        break;
                    }
                }
                continue;
            }

            /* If the log is already in pending state, then skip. Otherwise put the log to candidate container. */
            if (!mPendingDbIdentifiers.contains(dbIdentifier)) {
                try {

                    /* Deserialize JSON to Log. */
                    String logPayload;
                    String databasePayload = values.getAsString(COLUMN_LOG);
                    if (databasePayload == null) {
                        File file = getLargePayloadFile(largePayloadGroupDirectory, dbIdentifier);
                        AppCenterLog.debug(LOG_TAG, "Read payload file " + file);
                        logPayload = FileManager.read(file);
                        if (logPayload == null) {
                            throw new JSONException("Log payload is null and not stored as a file.");
                        }
                    } else {
                        logPayload = databasePayload;
                    }
                    String databasePayloadType = values.getAsString(COLUMN_DATA_TYPE);
                    Log log = getLogSerializer().deserializeLog(logPayload, databasePayloadType);

                    /* Restore target token. */
                    String targetToken = values.getAsString(COLUMN_TARGET_TOKEN);
                    if (targetToken != null) {
                        CryptoUtils.DecryptedData data = CryptoUtils.getInstance(mContext).decrypt(targetToken, false);
                        log.addTransmissionTarget(data.getDecryptedData());
                    }

                    /* Add log to list and count. */
                    candidates.put(dbIdentifier, log);
                    count++;
                } catch (JSONException e) {

                    /* If it is not able to deserialize, delete and get another log. */
                    AppCenterLog.error(LOG_TAG, "Cannot deserialize a log in the database", e);

                    /* Put the failed identifier to delete. */
                    failedDbIdentifiers.add(dbIdentifier);
                }
            }
        }
        if (cursor != null) {
            try {
                cursor.close();
            } catch (RuntimeException ignore) {
            }
        }

        /* Delete any logs that cannot be de-serialized. */
        if (failedDbIdentifiers.size() > 0) {
            for (long id : failedDbIdentifiers) {
                deleteLog(largePayloadGroupDirectory, id);
            }
            AppCenterLog.warn(LOG_TAG, "Deleted logs that cannot be deserialized");
        }

        /* No logs found. */
        if (candidates.size() <= 0) {
            AppCenterLog.debug(LOG_TAG, "No logs found in the Persistence database at the moment");
            return null;
        }

        /* Generate an ID. */
        String id = UUID.randomUUID().toString();

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Returning " + candidates.size() + " log(s) with an ID, " + id);
        AppCenterLog.debug(LOG_TAG, "The SID/ID pairs for returning log(s) is/are:");
        List<Long> pendingDbIdentifiersGroup = new ArrayList<>();
        for (Map.Entry<Long, Log> entry : candidates.entrySet()) {
            Long dbIdentifier = entry.getKey();

            /* Change a database identifier to pending state. */
            mPendingDbIdentifiers.add(dbIdentifier);

            /* Store a database identifier to a group of the ID. */
            pendingDbIdentifiersGroup.add(dbIdentifier);

            /* Add to output parameter. */
            outLogs.add(entry.getValue());

            /* Log. */
            AppCenterLog.debug(LOG_TAG, "\t" + entry.getValue().getSid() + " / " + dbIdentifier);
        }

        /* Update pending IDs. */
        mPendingDbIdentifiersGroups.put(group + id, pendingDbIdentifiersGroup);
        return id;
    }

    @Override
    public void clearPendingLogState() {
        mPendingDbIdentifiers.clear();
        mPendingDbIdentifiersGroups.clear();
        AppCenterLog.debug(LOG_TAG, "Cleared pending log states");
    }

    @Override
    public void close() {
        mDatabaseManager.close();
    }

    private List<Long> getLogsIds(SQLiteQueryBuilder builder, String[] selectionArgs) {
        List<Long> result = new ArrayList<>();
        try {
            Cursor cursor = mDatabaseManager.getCursor(builder, SELECT_PRIMARY_KEY, selectionArgs, null);
            try {
                while (cursor.moveToNext()) {
                    ContentValues idValues = mDatabaseManager.buildValues(cursor);
                    Long id = idValues.getAsLong(PRIMARY_KEY);
                    result.add(id);
                }
            } finally {
                cursor.close();
            }
        } catch (RuntimeException e) {
            AppCenterLog.error(LOG_TAG, "Failed to get corrupted ids: ", e);
        }
        return result;
    }
}
