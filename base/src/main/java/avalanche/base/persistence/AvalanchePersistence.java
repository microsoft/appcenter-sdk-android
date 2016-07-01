package avalanche.base.persistence;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.List;

import avalanche.base.ingestion.models.Log;

/**
 * Abstract class for AvalanchePersistence service.
 */
public abstract class AvalanchePersistence {

    /**
     * Storage capacity in number of logs
     */
    private static final int CAPACITY = 300;

    /**
     * Writes a log to the storage with the given {@code key}.
     *
     * @param key The key of the storage for the log.
     * @param log The log to be placed in the storage.
     * @throws PersistenceException Exception will be thrown if persistence cannot write a log to the storage.
     */
    public abstract void putLog(@NonNull String key, @NonNull Log log) throws PersistenceException;

    /**
     * Deletes a log with the give ID from the key.
     *
     * @param key The key of the storage for the log.
     * @param id  The ID for a set of logs.
     */
    public abstract void deleteLog(@NonNull String key, @NonNull String id);

    /**
     * Gets an array of logs for the given {@code key}.
     *
     * @param key     The key of the storage for the log.
     * @param limit   The max number of logs to be returned. {@code 0} for all logs in the storage.
     * @param outLogs A list to receive {@link Log} objects.
     * @return An ID for {@code outLogs}.
     */
    public abstract String getLogs(@NonNull String key, @IntRange(from = 0) int limit, List<Log> outLogs);

    /**
     * Thrown when {@link AvalanchePersistence} cannot write a log to the storage.
     */
    public static class PersistenceException extends Exception {
        public PersistenceException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }
    }
}
