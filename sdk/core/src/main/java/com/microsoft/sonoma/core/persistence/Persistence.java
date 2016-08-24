package com.microsoft.sonoma.core.persistence;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;

import java.util.List;

/**
 * Abstract class for Persistence service.
 */
/* TODO (jaelim): Interface vs Abstract class. Need to revisit to finalize. */
public abstract class Persistence {

    /**
     * Storage capacity in number of logs
     */
    static final int DEFAULT_CAPACITY = 300;

    /**
     * Log serializer override.
     */
    private LogSerializer mLogSerializer;

    /**
     * Writes a log to the storage with the given {@code group}.
     *
     * @param group The group of the storage for the log.
     * @param log   The log to be placed in the storage.
     * @throws PersistenceException Exception will be thrown if persistence cannot write a log to the storage.
     */
    public abstract void putLog(@NonNull String group, @NonNull Log log) throws PersistenceException;

    /**
     * Deletes a log with the give ID from the {@code group}.
     *
     * @param group The group of the storage for the log.
     * @param id    The ID for a set of logs.
     */
    public abstract void deleteLogs(@NonNull String group, @NonNull String id);

    /**
     * Deletes all logs for the given {@code group}
     *
     * @param group The group of the storage for the log.
     */
    public abstract void deleteLogs(String group);

    /**
     * Count number of logs for the given {@code group}.
     *
     * @param group The group of the storage for the log.
     * @return number of logs for the given {@code group}.
     */
    public abstract int countLogs(@NonNull String group);

    /**
     * Gets an array of logs for the given {@code group}.
     *
     * @param group   The group of the storage for the log.
     * @param limit   The max number of logs to be returned.
     * @param outLogs A list to receive {@link Log} objects.
     * @return An ID for {@code outLogs}. {@code null} if no logs exist.
     */
    @Nullable
    public abstract String getLogs(@NonNull String group, @IntRange(from = 0) int limit, @NonNull List<Log> outLogs);

    /**
     * Clears all associations between logs and ids returned by {@link #getLogs(String, int, List)} ()}.
     */
    public abstract void clearPendingLogState();

    /**
     * Clears all logs.
     */
    public abstract void clear();

    /**
     * Gets a {@link LogSerializer}.
     *
     * @return The log serializer instance.
     */
    LogSerializer getLogSerializer() {
        if (mLogSerializer == null)
            throw new IllegalStateException("logSerializer not configured");
        return mLogSerializer;
    }

    /**
     * Sets a {@link LogSerializer}.
     *
     * @param logSerializer The log serializer instance.
     */
    public void setLogSerializer(@NonNull LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
    }

    /**
     * Thrown when {@link Persistence} cannot write a log to the storage.
     */
    public static class PersistenceException extends Exception {
        public PersistenceException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
