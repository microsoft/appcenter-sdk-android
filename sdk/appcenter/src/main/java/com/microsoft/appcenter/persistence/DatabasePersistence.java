package com.microsoft.appcenter.persistence;

import android.content.ContentValues;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.DatabaseManager;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.utils.storage.StorageHelper.DatabaseStorage;

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
    private static final String DATABASE = "com.microsoft.appcenter.persistence";

    /**
     * Table name.
     */
    private static final String TABLE = "logs";

    /**
     * Table schema for Persistence.
     */
    private static final ContentValues SCHEMA = getContentValues("", "");

    /**
     * Size limit for a database row log payload.
     * A separate file is used if payload is larger.
     */
    private static final int PAYLOAD_MAX_SIZE = (int) (1.9 * 1024 * 1024);

    /**
     * Sub path for directory where to store large payloads.
     */
    private static final String PAYLOAD_LARGE_DIRECTORY = "/appcenter/database_large_payloads";

    private static final String PAYLOAD_FILE_EXTENSION = ".payload";

    private static final String FILE_SCHEME = "file://";

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

    private final File mLargePayloadDirectory;

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
                        AppCenterLog.error(LOG_TAG, "Cannot complete an operation (" + operation + ")", e);
                    }
                });
        mLargePayloadDirectory = new File(Constants.FILES_PATH + PAYLOAD_LARGE_DIRECTORY);
        //noinspection ResultOfMethodCallIgnored we handle errors at read/write time for each file.
        mLargePayloadDirectory.mkdirs();
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
            AppCenterLog.debug(LOG_TAG, "Storing a log to the Persistence database for log type " + log.getType() + " with sid=" + log.getSid());
            String payload = getLogSerializer().serializeLog(log);
            ContentValues contentValues;
            if (payload.length() >= PAYLOAD_MAX_SIZE) {
                contentValues = getContentValues(group, null);
            } else {
                contentValues = getContentValues(group, payload);
            }
            long databaseId = mDatabaseStorage.put(contentValues);
            AppCenterLog.debug(LOG_TAG, "Stored a log to the Persistence database for log type " + log.getType() + " with databaseId=" + databaseId);
            if (payload.length() >= PAYLOAD_MAX_SIZE) {
                AppCenterLog.debug(LOG_TAG, "Payload is larger than what SQLite supports, storing payload in a separate file.");
                File directory = getLargePayloadGroupDirectory(group);
                //noinspection ResultOfMethodCallIgnored
                directory.mkdir();
                File payloadFile = getLargePayloadFile(directory, databaseId);
                StorageHelper.InternalStorage.write(payloadFile, payload);
                contentValues = getContentValues(group, FILE_SCHEME + payloadFile);
                mDatabaseStorage.update(databaseId, contentValues);
                AppCenterLog.debug(LOG_TAG, "Payload written to " + payloadFile);
            }
        } catch (JSONException e) {
            throw new PersistenceException("Cannot convert to JSON string", e);
        } catch (IOException e) {
            throw new PersistenceException("Cannot save large payload in a file", e);
        }
    }

    @NonNull
    private File getLargePayloadGroupDirectory(String group) {
        return new File(mLargePayloadDirectory, group);
    }

    @NonNull
    private File getLargePayloadFile(File directory, long databaseId) {
        return new File(directory, databaseId + PAYLOAD_FILE_EXTENSION);
    }

    @Override
    public void deleteLogs(@NonNull String group, @NonNull String id) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Deleting logs from the Persistence database for " + group + " with " + id);
        AppCenterLog.debug(LOG_TAG, "The IDs for deleting log(s) is/are:");

        List<Long> dbIdentifiers = mPendingDbIdentifiersGroups.remove(group + id);
        File directory = getLargePayloadGroupDirectory(group);
        if (dbIdentifiers != null) {
            for (Long dbIdentifier : dbIdentifiers) {
                AppCenterLog.debug(LOG_TAG, "\t" + dbIdentifier);
                mDatabaseStorage.delete(dbIdentifier);
                mPendingDbIdentifiers.remove(dbIdentifier);
                //noinspection ResultOfMethodCallIgnored
                getLargePayloadFile(directory, dbIdentifier).delete();
            }
        }
    }

    @Override
    public void deleteLogs(String group) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Deleting all logs from the Persistence database for " + group);

        /* Delete from database. */
        mDatabaseStorage.delete(COLUMN_GROUP, group);

        /* Delete from pending state. */
        for (Iterator<String> iterator = mPendingDbIdentifiersGroups.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            if (key.startsWith(group)) {
                iterator.remove();
            }
        }

        /* Delete large payload files */
        File directory = getLargePayloadGroupDirectory(group);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    @Override
    public int countLogs(@NonNull String group) {

        /* Query database and get scanner. */
        DatabaseStorage.DatabaseScanner scanner = mDatabaseStorage.getScanner(COLUMN_GROUP, group, true);
        int count = scanner.getCount();
        scanner.close();
        return count;
    }

    @Override
    @Nullable
    public String getLogs(@NonNull String group, @IntRange(from = 0) int limit, @NonNull List<Log> outLogs) {

        /* Log. */
        AppCenterLog.debug(LOG_TAG, "Trying to get " + limit + " logs from the Persistence database for " + group);

        /* Query database and get scanner. */
        DatabaseStorage.DatabaseScanner scanner = mDatabaseStorage.getScanner(COLUMN_GROUP, group);

        /* Add logs to output parameter after deserialization if logs are not already sent. */
        int count = 0;
        Map<Long, Log> candidates = new TreeMap<>();
        List<Long> failedDbIdentifiers = new ArrayList<>();
        for (Iterator<ContentValues> iterator = scanner.iterator(); iterator.hasNext() && count < limit; ) {
            ContentValues values = iterator.next();
            Long dbIdentifier = values.getAsLong(DatabaseManager.PRIMARY_KEY);

            /*
             * When we can't even read the identifier (in this case ContentValues is most likely empty).
             * That probably means it contained a record larger than 5MB and we hit the cursor limit.
             * Get rid of first non pending log.
             */
            if (dbIdentifier == null) {
                AppCenterLog.error(LOG_TAG, "Empty database record, probably content was larger than 1.4MB, need to delete as it's now corrupted");
                DatabaseStorage.DatabaseScanner idScanner = mDatabaseStorage.getScanner(COLUMN_GROUP, group, true);
                for (ContentValues idValues : idScanner) {
                    Long invalidId = idValues.getAsLong(DatabaseManager.PRIMARY_KEY);
                    if (!mPendingDbIdentifiers.contains(invalidId) && !candidates.containsKey(invalidId)) {

                        /* Found the record to delete that we could not read when selecting all fields. */
                        mDatabaseStorage.delete(invalidId);
                        AppCenterLog.error(LOG_TAG, "Empty database corrupted empty record deleted, id=" + invalidId);
                        break;
                    }
                }
                idScanner.close();
                continue;
            }

            /* If the log is already in pending state, then skip. Otherwise put the log to candidate container. */
            if (!mPendingDbIdentifiers.contains(dbIdentifier)) {
                try {

                    /* Deserialize JSON to Log. */
                    String logPayload;
                    String databasePayload = values.getAsString(COLUMN_LOG);
                    if (databasePayload != null && databasePayload.startsWith(FILE_SCHEME)) {
                        File file = new File(databasePayload.substring(FILE_SCHEME.length()));
                        AppCenterLog.debug(LOG_TAG, "Read payload file " + file);
                        logPayload = StorageHelper.InternalStorage.read(file);
                    } else {
                        logPayload = databasePayload;
                    }
                    if (logPayload == null) {
                        throw new JSONException("Log payload is null");
                    }
                    candidates.put(dbIdentifier, getLogSerializer().deserializeLog(logPayload));
                    count++;
                } catch (JSONException e) {

                    /* If it is not able to deserialize, delete and get another log. */
                    AppCenterLog.error(LOG_TAG, "Cannot deserialize a log in the database", e);

                    /* Put the failed identifier to delete. */
                    failedDbIdentifiers.add(dbIdentifier);
                }
            }
        }
        scanner.close();

        /* Delete any logs that cannot be deserialized. */
        if (failedDbIdentifiers.size() > 0) {
            mDatabaseStorage.delete(failedDbIdentifiers);
            AppCenterLog.warn(LOG_TAG, "Deleted logs that cannot be deserialized");
        }

        /* No logs found. */
        if (candidates.size() <= 0) {
            AppCenterLog.debug(LOG_TAG, "No logs found in the Persistence database at the moment");
            return null;
        }

        /* Generate an ID. */
        String id = UUIDUtils.randomUUID().toString();

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
    public void close() throws IOException {
        mDatabaseStorage.close();
    }
}