package com.microsoft.azure.mobile.persistence;

import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;

public class DatabasePersistenceAsync {

    /**
     * Thread name for persistence to access database.
     */
    @VisibleForTesting
    public static final String THREAD_NAME = "DatabasePersistenceThread";

    /**
     * Android "timer" using a background thread loop.
     */
    private final Handler mHandler;

    /**
     * The Persistence instance
     */
    private final Persistence mPersistence;

    public DatabasePersistenceAsync(Persistence persistence) {
        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();
        this.mHandler = new Handler(thread.getLooper());
        this.mPersistence = persistence;
    }

    /**
     * Writes a log asynchronously to the storage with the given {@code group}.
     *
     * @param group    The group of the storage for the log.
     * @param log      The log to be placed in the storage.
     * @param callback The callback to be called after the operation is completed.
     */
    public void putLog(@NonNull final String group, @NonNull final Log log, @Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mPersistence.putLog(group, log);
                    onSuccess(callback, null);
                } catch (Persistence.PersistenceException e) {
                    onFailure(callback, e);
                }
            }
        });
    }

    /**
     * Deletes a log asynchronously with the give ID from the {@code group}.
     * Use {@link #deleteLogs(String, String, DatabasePersistenceAsyncCallback)} if callback needs to be used.
     *
     * @param group The group of the storage for logs.
     * @param id    The ID for a set of logs.
     */
    public void deleteLogs(@NonNull String group, @NonNull String id) {
        deleteLogs(group, id, null);
    }

    /**
     * Deletes a log asynchronously with the give ID from the {@code group}.
     *
     * @param group    The group of the storage for logs.
     * @param id       The ID for a set of logs.
     * @param callback The callback to be called after the operation is completed.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public void deleteLogs(@NonNull final String group, @NonNull final String id, @Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPersistence.deleteLogs(group, id);
                onSuccess(callback, null);
            }
        });
    }

    /**
     * Deletes all logs asynchronously for the given {@code group}
     * Use {@link #deleteLogs(String, DatabasePersistenceAsyncCallback)} if callback needs to be used.
     *
     * @param group The group of the storage for logs.
     */
    public void deleteLogs(String group) {
        deleteLogs(group, (DatabasePersistenceAsyncCallback) null);
    }

    /**
     * Deletes all logs asynchronously for the given {@code group}
     *
     * @param group    The group of the storage for logs.
     * @param callback The callback to be called after the operation is completed.
     */
    @SuppressWarnings("WeakerAccess")
    public void deleteLogs(final String group, @Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPersistence.deleteLogs(group);
                onSuccess(callback, null);
            }
        });
    }

    /**
     * Gets the number of logs asynchronously for the given {@code group}.
     *
     * @param group    The group of the storage for logs.
     * @param callback The callback to be called with the number of logs for the given {@code group} after the operation is completed.
     */
    public void countLogs(@NonNull final String group, @Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                int count = mPersistence.countLogs(group);
                onSuccess(callback, count);
            }
        });
    }

    /**
     * Gets an array of logs asynchronously for the given {@code group}.
     *
     * @param group    The group of the storage for logs.
     * @param limit    The max number of logs to be returned.
     * @param outLogs  A list to receive {@link Log} objects.
     * @param callback The callback to be called with an ID for {@code outLogs} after the operation is completed.
     *                 The result can be {@code null} if no logs exist.
     */
    public void getLogs(@NonNull final String group, @IntRange(from = 0) final int limit, @NonNull final List<Log> outLogs, @Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String id = mPersistence.getLogs(group, limit, outLogs);
                onSuccess(callback, id);
            }
        });
    }

    /**
     * Clears all associations between logs and IDs returned by {@link #getLogs(String, int, List, DatabasePersistenceAsyncCallback)} asynchronously.
     * Use {@link #clearPendingLogState(DatabasePersistenceAsyncCallback)} if callback needs to be used.
     *
     */
    public void clearPendingLogState() {
        clearPendingLogState(null);
    }

    /**
     * Clears all associations between logs and IDs returned by {@link #getLogs(String, int, List, DatabasePersistenceAsyncCallback)} asynchronously.
     *
     * @param callback The callback to be called after the operation is completed.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public void clearPendingLogState(@Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mPersistence.clearPendingLogState();
                onSuccess(callback, null);
            }
        });
    }

    /**
     * Closes Persistence service.
     * Use {@link #close(DatabasePersistenceAsyncCallback)} if callback needs to be used.
     */
    public void close() {
        close(null);
    }

    /**
     * Closes Persistence service.
     *
     * @param callback The callback to be called after the operation is completed.
     */
    public void close(@Nullable final DatabasePersistenceAsyncCallback callback) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                try {
                    mPersistence.close();
                    onSuccess(callback, null);
                } catch (IOException e) {
                    onFailure(callback, e);
                }
                mHandler.removeCallbacks(this);
            }
        });
    }

    /**
     * Wait for all current tasks to complete. It does not wait for future tasks.
     *
     * @param timeout the maximum time to wait in millis.
     * @throws InterruptedException if the current thread is interrupted.
     */
    public void waitForCurrentTasksToComplete(long timeout) throws InterruptedException {
        final Semaphore semaphore = new Semaphore(0);
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                semaphore.release();
                MobileCenterLog.debug(LOG_TAG, "Persistence tasks completed.");
            }
        });
        if (!semaphore.tryAcquire(timeout, TimeUnit.MILLISECONDS)) {
            MobileCenterLog.error(LOG_TAG, "Timeout waiting for database tasks to complete.");
        }
    }

    /**
     * Helper method for onSuccess callback.
     */
    private void onSuccess(DatabasePersistenceAsyncCallback callback, Object result) {
        if (callback != null)
            callback.onSuccess(result);
    }

    /**
     * Helper method for onFailure callback.
     */
    @VisibleForTesting
    void onFailure(DatabasePersistenceAsyncCallback callback, Exception e) {
        if (callback != null)
            callback.onFailure(e);
    }

    /**
     * The callback used for Persistence asynchronous operations.
     */
    public interface DatabasePersistenceAsyncCallback {

        /**
         * To be called with result when Persistence operation completed successfully.
         *
         * @param result The result of the operation.
         */
        void onSuccess(Object result);

        /**
         * To be called with an exception when Persistence operation failed.
         *
         * @param e An exception that caused the failure.
         */
        void onFailure(Exception e);
    }

    /**
     * Abstract callback for {@link DatabasePersistenceAsync}. Do nothing when {@link #onFailure(Exception)} is called.
     */
    public abstract static class AbstractDatabasePersistenceAsyncCallback implements DatabasePersistenceAsyncCallback {

        @Override
        public final void onFailure(Exception e) {
        }
    }
}
