package avalanche.core.persistence;

import android.content.ContentValues;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import avalanche.core.ingestion.models.Log;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.DatabaseManager;
import avalanche.core.utils.UUIDUtils;

import static avalanche.core.utils.StorageHelper.DatabaseStorage;

public class AvalancheDatabasePersistence extends AvalanchePersistence implements Closeable {

    /**
     * Name of key column in the table.
     */
    public static final String COLUMN_KEY = "key";

    /**
     * Name of log column in the table.
     */
    private static final String COLUMN_LOG = "log";

    /**
     * Database name.
     */
    private static final String DATABASE = "avalanche.persistence";

    /**
     * Table name.
     */
    private static final String TABLE = "logs";

    /**
     * Table schema for persistence.
     */
    private static final ContentValues SCHEMA = getContentValues("", "");

    /**
     * Database storage instance to access persistence database.
     */
    final DatabaseStorage mDatabaseStorage;

    /**
     * Pending log groups. Key is a UUID and value is a list of database identifiers.
     */
    private final Map<String, List<Long>> mPendingDbIdentifiersGroups;

    /**
     * Pending logs across all groups.
     */
    private final Set<Long> mPendingDbIdentifiers;

    /**
     * Initializes variables.
     */
    public AvalancheDatabasePersistence() {
        this(DATABASE, TABLE, 1);
    }

    /**
     * Initializes variables.
     *
     * @param database The database name
     * @param table    The table name
     * @param version  The version of current schema.
     */
    AvalancheDatabasePersistence(String database, String table, int version) {
        this(database, table, version, AvalanchePersistence.DEFAULT_CAPACITY);
    }

    /**
     * Initializes variables.
     *
     * @param database   The database name
     * @param table      The table name
     * @param version    The version of current schema.
     * @param maxRecords The maximum number of records allowed in the table.
     */
    public AvalancheDatabasePersistence(String database, String table, int version, int maxRecords) {
        mPendingDbIdentifiersGroups = new HashMap<>();
        mPendingDbIdentifiers = new HashSet<>();
        mDatabaseStorage = DatabaseStorage.getDatabaseStorage(database, table, version, SCHEMA, maxRecords,
                new DatabaseStorage.DatabaseErrorListener() {
                    @Override
                    public void onError(String operation, RuntimeException e) {
                        AvalancheLog.error("Cannot complete an operation (" + operation + ")", e);
                    }
                });
    }

    /**
     * Instantiates {@link ContentValues} with the give values.
     *
     * @param key  The key of the storage for the log.
     * @param logJ The JSON string for a log.
     * @return A {@link ContentValues} instance.
     */
    private static ContentValues getContentValues(@Nullable String key, @Nullable String logJ) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_KEY, key);
        values.put(COLUMN_LOG, logJ);
        return values;
    }

    @Override
    public void putLog(@NonNull String key, @NonNull Log log) throws PersistenceException {
        /* Convert log to JSON string and put in the database. */
        try {
            AvalancheLog.debug("Storing a log to the persistence database for log type " + log.getType() + " with " + log.getSid());
            mDatabaseStorage.put(getContentValues(key, getLogSerializer().serializeLog(log)));
        } catch (JSONException e) {
            throw new PersistenceException("Cannot convert to JSON string", e);
        }
    }

    @Override
    public void deleteLog(@NonNull String key, @NonNull String id) {
        /* Log. */
        AvalancheLog.info("Deleting logs from the persistence database for " + key + " with " + id);
        AvalancheLog.debug("The IDs for deleting log(s) is/are:");

        List<Long> dbIdentifiers = mPendingDbIdentifiersGroups.remove(key + id);
        if (dbIdentifiers != null) {
            for (Long dbIdentifier : dbIdentifiers) {
                AvalancheLog.debug("\t" + dbIdentifier);
                mDatabaseStorage.delete(dbIdentifier);
                mPendingDbIdentifiers.remove(dbIdentifier);
            }
        }
    }

    @Override
    @Nullable
    public String getLogs(@NonNull String key, @IntRange(from = 1) int limit, @NonNull List<Log> outLogs) {
        /* Log. */
        AvalancheLog.info("Trying to get " + limit + " logs from the persistence database for " + key);

        /* Query database and get scanner. */
        DatabaseStorage.DatabaseScanner scanner = mDatabaseStorage.getScanner(COLUMN_KEY, key);

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
                    AvalancheLog.error("Cannot deserialize a log in the database", e);

                    /* Put the failed identifier to delete. */
                    failedDbIdentifiers.add(dbIdentifier);
                }
            }
        }

        /* Delete any logs that cannot be deserialized. */
        if (failedDbIdentifiers.size() > 0) {
            mDatabaseStorage.delete(failedDbIdentifiers);
            AvalancheLog.info("Deleted logs that cannot be deserialized");
        }

        /* No logs found. */
        if (candidates.size() <= 0) {
            AvalancheLog.info("No logs found in the persistence database at the moment");
            return null;
        }

        /* Generate an ID. */
        String id = UUIDUtils.randomUUID().toString();

        /* Log. */
        AvalancheLog.info("Returning " + candidates.size() + " log(s) with an ID, " + id);
        AvalancheLog.debug("The SID/ID pairs for returning log(s) is/are:");

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
            AvalancheLog.debug("\t" + entry.getValue().getSid().toString() + " / " + dbIdentifier);
        }

        /* Update pending IDs. */
        mPendingDbIdentifiersGroups.put(key + id, pendingDbIdentifiersGroup);

        return id;
    }

    @Override
    public void clearPendingLogState() {
        mPendingDbIdentifiers.clear();
        mPendingDbIdentifiersGroups.clear();
        AvalancheLog.info("Cleared pending log states");
    }

    @Override
    public void clear() {
        clearPendingLogState();
        mDatabaseStorage.clear();
        AvalancheLog.info("Deleted logs from the persistence database");
    }

    @Override
    public void close() throws IOException {
        mDatabaseStorage.close();
    }
}
