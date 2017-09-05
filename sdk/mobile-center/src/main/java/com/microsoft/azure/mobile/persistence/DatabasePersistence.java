package com.microsoft.azure.mobile.persistence;

import android.content.ContentValues;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.storage.DatabaseManager;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;
import static com.microsoft.azure.mobile.utils.storage.StorageHelper.DatabaseStorage;

public class DatabasePersistence extends Persistence {

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
     * Database name.
     */
    private static final String DATABASE = "com.microsoft.azure.mobile.persistence";

    /**
     * Table name.
     */
    private static final String TABLE = "logs";

    /**
     * Table schema for Persistence.
     */
    private static final ContentValues SCHEMA = getContentValues("", "");

    /**
     * Database storage instance to access Persistence database.
     */
    final DatabaseStorage mDatabaseStorage;

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
     * Initializes variables.
     */
    public DatabasePersistence() {
        this(DATABASE, TABLE, 1);
    }

    /**
     * Initializes variables.
     *
     * @param database The database name
     * @param table    The table name
     * @param version  The version of current schema.
     */
    DatabasePersistence(String database, String table, @SuppressWarnings("SameParameterValue") int version) {
        this(database, table, version, Persistence.DEFAULT_CAPACITY);
    }

    /**
     * Initializes variables.
     *
     * @param database   The database name
     * @param table      The table name
     * @param version    The version of current schema.
     * @param maxRecords The maximum number of records allowed in the table.
     */
    DatabasePersistence(String database, String table, int version, int maxRecords) {
        mPendingDbIdentifiersGroups = new HashMap<>();
        mPendingDbIdentifiers = new HashSet<>();
        mDatabaseStorage = DatabaseStorage.getDatabaseStorage(database, table, version, SCHEMA, maxRecords,
                new DatabaseStorage.DatabaseErrorListener() {
                    @Override
                    public void onError(String operation, RuntimeException e) {
                        MobileCenterLog.error(LOG_TAG, "Cannot complete an operation (" + operation + ")", e);
                    }
                });
    }

    /**
     * Instantiates {@link ContentValues} with the give values.
     *
     * @param group The group of the storage for the log.
     * @param logJ  The JSON string for a log.
     * @return A {@link ContentValues} instance.
     */
    private static ContentValues getContentValues(@Nullable String group, @Nullable String logJ) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_GROUP, group);
        values.put(COLUMN_LOG, logJ);
        return values;
    }

    @Override
    public void putLog(@NonNull String group, @NonNull Log log) throws PersistenceException {
        /* Convert log to JSON string and put in the database. */
        try {
            MobileCenterLog.debug(LOG_TAG, "Storing a log to the Persistence database for log type " + log.getType() + " with " + log.getSid());
            mDatabaseStorage.put(getContentValues(group, getLogSerializer().serializeLog(log)));
        } catch (JSONException e) {
            throw new PersistenceException("Cannot convert to JSON string", e);
        }
    }

    @Override
    public void deleteLogs(@NonNull String group, @NonNull String id) {
        /* Log. */
        MobileCenterLog.debug(LOG_TAG, "Deleting logs from the Persistence database for " + group + " with " + id);
        MobileCenterLog.debug(LOG_TAG, "The IDs for deleting log(s) is/are:");

        List<Long> dbIdentifiers = mPendingDbIdentifiersGroups.remove(group + id);
        if (dbIdentifiers != null) {
            for (Long dbIdentifier : dbIdentifiers) {
                MobileCenterLog.debug(LOG_TAG, "\t" + dbIdentifier);
                mDatabaseStorage.delete(dbIdentifier);
                mPendingDbIdentifiers.remove(dbIdentifier);
            }
        }
    }

    @Override
    public void deleteLogs(String group) {
        /* Log. */
        MobileCenterLog.debug(LOG_TAG, "Deleting all logs from the Persistence database for " + group);

        /* Delete from database. */
        mDatabaseStorage.delete(COLUMN_GROUP, group);

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

        /* Query database and get scanner. */
        DatabaseStorage.DatabaseScanner scanner = mDatabaseStorage.getScanner(COLUMN_GROUP, group);
        int count = scanner.getCount();
        scanner.close();
        return count;
    }

    @Override
    @Nullable
    public String getLogs(@NonNull String group, @IntRange(from = 0) int limit, @NonNull List<Log> outLogs) {
        /* Log. */
        MobileCenterLog.debug(LOG_TAG, "Trying to get " + limit + " logs from the Persistence database for " + group);

        /* Query database and get scanner. */
        DatabaseStorage.DatabaseScanner scanner = mDatabaseStorage.getScanner(COLUMN_GROUP, group);

        /* Add logs to output parameter after deserialization if logs are not already sent. */
        int count = 0;
        Map<Long, Log> candidates = new TreeMap<>();
        List<Long> failedDbIdentifiers = new ArrayList<>();
        for (Iterator<ContentValues> iterator = scanner.iterator(); iterator.hasNext() && count < limit; ) {
            ContentValues values = iterator.next();
            Long dbIdentifier = values.getAsLong(DatabaseManager.PRIMARY_KEY);

            /* If the log is already in pending state, then skip. Otherwise put the log to candidate container. */
            if (!mPendingDbIdentifiers.contains(dbIdentifier)) {
                try {
                    /* Deserialize JSON to Log. */
                    candidates.put(dbIdentifier, getLogSerializer().deserializeLog(values.getAsString(COLUMN_LOG)));
                    count++;
                } catch (JSONException e) {
                    /* If it is not able to deserialize, delete and get another log. */
                    MobileCenterLog.error(LOG_TAG, "Cannot deserialize a log in the database", e);

                    /* Put the failed identifier to delete. */
                    failedDbIdentifiers.add(dbIdentifier);
                }
            }
        }
        scanner.close();

        /* Delete any logs that cannot be deserialized. */
        if (failedDbIdentifiers.size() > 0) {
            mDatabaseStorage.delete(failedDbIdentifiers);
            MobileCenterLog.warn(LOG_TAG, "Deleted logs that cannot be deserialized");
        }

        /* No logs found. */
        if (candidates.size() <= 0) {
            MobileCenterLog.debug(LOG_TAG, "No logs found in the Persistence database at the moment");
            return null;
        }

        /* Generate an ID. */
        String id = UUIDUtils.randomUUID().toString();

        /* Log. */
        MobileCenterLog.debug(LOG_TAG, "Returning " + candidates.size() + " log(s) with an ID, " + id);
        MobileCenterLog.debug(LOG_TAG, "The SID/ID pairs for returning log(s) is/are:");

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
            MobileCenterLog.debug(LOG_TAG, "\t" + entry.getValue().getSid() + " / " + dbIdentifier);
        }

        /* Update pending IDs. */
        mPendingDbIdentifiersGroups.put(group + id, pendingDbIdentifiersGroup);
        return id;
    }

    @Override
    public void clearPendingLogState() {
        mPendingDbIdentifiers.clear();
        mPendingDbIdentifiersGroups.clear();
        MobileCenterLog.debug(LOG_TAG, "Cleared pending log states");
    }

    @Override
    public void close() throws IOException {
        mDatabaseStorage.close();
    }
}