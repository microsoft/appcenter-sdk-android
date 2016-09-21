package com.microsoft.sonoma.core.channel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.sonoma.core.Sonoma;
import com.microsoft.sonoma.core.ingestion.Ingestion;
import com.microsoft.sonoma.core.ingestion.ServiceCallback;
import com.microsoft.sonoma.core.ingestion.http.HttpUtils;
import com.microsoft.sonoma.core.ingestion.http.IngestionHttp;
import com.microsoft.sonoma.core.ingestion.http.IngestionNetworkStateHandler;
import com.microsoft.sonoma.core.ingestion.http.IngestionRetryer;
import com.microsoft.sonoma.core.ingestion.models.Device;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.LogContainer;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;
import com.microsoft.sonoma.core.persistence.DatabasePersistence;
import com.microsoft.sonoma.core.persistence.Persistence;
import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.IdHelper;
import com.microsoft.sonoma.core.utils.NetworkStateHelper;
import com.microsoft.sonoma.core.utils.SonomaLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DefaultChannel implements Channel {

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * The application secret for the ingestion service.
     */
    private final UUID mAppSecret;

    /**
     * The installId that's required for forwarding to ingestion.
     */
    private final UUID mInstallId;

    /**
     * Handler for triggering ingestion of events.
     */
    private final Handler mIngestionHandler;

    /**
     * Channel state per log group.
     */
    private final Map<String, GroupState> mGroupStates;

    /**
     * Global listeners.
     */
    private final Collection<Listener> mListeners;

    /**
     * The persistence object used to store events in the local storage.
     */
    private final Persistence mPersistence;

    /**
     * The ingestion object used to send batches to the server.
     */
    private final Ingestion mIngestion;

    /**
     * Is channel enabled?
     */
    private boolean mEnabled;

    /**
     * Device properties.
     */
    private Device mDevice;

    /**
     * Creates and initializes a new instance.
     *
     * @param context       The context.
     * @param appSecret     The application secret.
     * @param logSerializer The log serializer.
     */
    public DefaultChannel(@NonNull Context context, @NonNull UUID appSecret, @NonNull LogSerializer logSerializer) {
        this(context, appSecret, buildDefaultPersistence(logSerializer), buildDefaultIngestion(context, logSerializer));
    }

    /**
     * Overloaded constructor with limited visibility that allows for dependency injection.
     *
     * @param context     The context.
     * @param appSecret   The application secret.
     * @param persistence Persistence object for dependency injection.
     * @param ingestion   Ingestion object for dependency injection.
     */
    @VisibleForTesting
    DefaultChannel(@NonNull Context context, @NonNull UUID appSecret, @NonNull Persistence persistence, @NonNull Ingestion ingestion) {
        mContext = context;
        mAppSecret = appSecret;
        mInstallId = IdHelper.getInstallId();
        mIngestionHandler = new Handler(Looper.getMainLooper());
        mGroupStates = new HashMap<>();
        mListeners = new HashSet<>();
        mPersistence = persistence;
        mIngestion = ingestion;
        mEnabled = true;
    }

    /**
     * Init ingestion for default constructor.
     */
    private static Ingestion buildDefaultIngestion(@NonNull Context context, @NonNull LogSerializer logSerializer) {
        IngestionHttp api = new IngestionHttp(logSerializer);
        IngestionRetryer retryer = new IngestionRetryer(api);
        return new IngestionNetworkStateHandler(retryer, NetworkStateHelper.getSharedInstance(context));
    }

    /**
     * Init persistence for default constructor.
     */
    private static Persistence buildDefaultPersistence(@NonNull LogSerializer logSerializer) {
        Persistence persistence = new DatabasePersistence();
        persistence.setLogSerializer(logSerializer);
        return persistence;
    }

    @Override
    public synchronized void addGroup(String groupName, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, GroupListener groupListener) {

        /* Init group. */
        SonomaLog.debug(Sonoma.LOG_TAG, "addGroup(" + groupName + ")");
        mGroupStates.put(groupName, new GroupState(groupName, maxLogsPerBatch, batchTimeInterval, maxParallelBatches, groupListener));

        /* Count pending logs. */
        mGroupStates.get(groupName).mPendingLogCount = mPersistence.countLogs(groupName);

        /* Schedule sending any pending log. */
        checkPendingLogs(groupName);
    }

    @Override
    public synchronized void removeGroup(String groupName) {
        GroupState groupState = mGroupStates.remove(groupName);
        if (groupState != null) {
            cancelTimer(groupState);
        }
    }

    @Override
    public synchronized boolean isEnabled() {
        return mEnabled;
    }

    /**
     * Set the enabled flag. If false, the channel will continue to persist data but not forward any item to ingestion.
     * The most common use-case would be to set it to false and enable sending again after the channel has disabled itself after receiving
     * a recoverable error (most likely related to a server issue).
     *
     * @param enabled flag to enable or disable the channel.
     */
    @Override
    public synchronized void setEnabled(boolean enabled) {
        if (mEnabled == enabled)
            return;
        if (enabled) {
            mEnabled = true;
            for (String groupName : mGroupStates.keySet())
                checkPendingLogs(groupName);
        } else
            suspend(true);
    }

    @Override
    public void setServerUrl(String serverUrl) {
        mIngestion.setServerUrl(serverUrl);
    }

    /**
     * Delete all persisted logs for the given group.
     *
     * @param groupName the group name.
     */
    @Override
    public synchronized void clear(String groupName) {
        mPersistence.deleteLogs(groupName);
    }

    @Override
    public synchronized void invalidateDeviceCache() {
        mDevice = null;
    }

    /**
     * Stop sending logs until app is restarted or the channel is enabled again.
     *
     * @param deleteLogs in addition to suspending, if this is true, delete all logs from persistence.
     */
    private void suspend(boolean deleteLogs) {
        mEnabled = false;
        for (GroupState groupState : mGroupStates.values()) {
            cancelTimer(groupState);
            groupState.mSendingBatches.clear();
        }
        try {
            mIngestion.close();
        } catch (IOException e) {
            SonomaLog.error(Sonoma.LOG_TAG, "Failed to close ingestion", e);
        }
        if (deleteLogs)
            mPersistence.clear();
        else
            mPersistence.clearPendingLogState();
    }

    private void cancelTimer(GroupState groupState) {
        if (groupState.mScheduled) {
            groupState.mScheduled = false;
            mIngestionHandler.removeCallbacks(groupState.mRunnable);
        }
    }

    @VisibleForTesting
    @SuppressWarnings("SameParameterValue")
    synchronized int getCounter(@NonNull String groupName) {
        return mGroupStates.get(groupName).mPendingLogCount;
    }

    /**
     * This will, if we're not using the limit for pending batches, trigger sending of a new request.
     * It will also reset the counters for sending out items for both the number of items enqueued and
     * the handlers. It will do this even if we don't have reached the limit
     * of pending batches or the time interval.
     *
     * @param groupName the group name
     */
    private synchronized void triggerIngestion(final @NonNull String groupName) {
        if (!mEnabled) {
            return;
        }
        final GroupState groupState = mGroupStates.get(groupName);
        SonomaLog.debug(Sonoma.LOG_TAG, "triggerIngestion(" + groupName + ") pendingLogCount=" + groupState.mPendingLogCount);
        cancelTimer(groupState);

        /* Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending. */
        if (groupState.mSendingBatches.size() == groupState.mMaxParallelBatches) {
            SonomaLog.debug(Sonoma.LOG_TAG, "Already sending " + groupState.mMaxParallelBatches + " batches of analytics data to the server.");
            return;
        }

        /* Get a batch from persistence. */
        List<Log> batch = new ArrayList<>(groupState.mMaxLogsPerBatch);
        final String batchId = mPersistence.getLogs(groupName, groupState.mMaxLogsPerBatch, batch);
        if (batchId != null) {

            /* Call group listener before sending logs to ingestion service. */
            if (groupState.mListener != null) {
                for (Log log : batch) {
                    groupState.mListener.onBeforeSending(log);
                }
            }

            /* Decrement counter. */
            groupState.mPendingLogCount -= batch.size();
            SonomaLog.debug(Sonoma.LOG_TAG, "ingestLogs(" + groupName + "," + batchId + ") pendingLogCount=" + groupState.mPendingLogCount);

            /* Remember this batch. */
            groupState.mSendingBatches.put(batchId, batch);

            /* Send logs. */
            LogContainer logContainer = new LogContainer();
            logContainer.setLogs(batch);
            mIngestion.sendAsync(mAppSecret, mInstallId, logContainer, new ServiceCallback() {

                        @Override
                        public void onCallSucceeded() {
                            handleSendingSuccess(groupState, batchId);
                        }

                        @Override
                        public void onCallFailed(Exception e) {
                            handleSendingFailure(groupState, batchId, e);
                        }
                    }
            );

            /* Check for more pending logs. */
            checkPendingLogs(groupName);
        }
    }

    /**
     * The actual implementation to react to sending a batch to the server successfully.
     *
     * @param groupState The group state.
     * @param batchId    The batch ID.
     */
    private synchronized void handleSendingSuccess(@NonNull final GroupState groupState, @NonNull final String batchId) {
        String groupName = groupState.mName;
        mPersistence.deleteLogs(groupName, batchId);
        List<Log> removedLogsForBatchId = groupState.mSendingBatches.remove(batchId);
        GroupListener groupListener = groupState.mListener;
        if (groupListener != null) {
            for (Log log : removedLogsForBatchId)
                groupListener.onSuccess(log);
        }
        checkPendingLogs(groupName);
    }

    /**
     * The actual implementation to react to not being able to send a batch to the server.
     * Will disable the sender in case of a recoverable error.
     * Will delete batch of data in case of a non-recoverable error.
     *
     * @param groupState the group state
     * @param batchId    the batch ID
     * @param e          the exception
     */
    private synchronized void handleSendingFailure(@NonNull final GroupState groupState, @NonNull final String batchId, @NonNull final Exception e) {
        String groupName = groupState.mName;
        SonomaLog.error(Sonoma.LOG_TAG, "Sending logs groupName=" + groupName + " id=" + batchId + " failed", e);
        List<Log> removedLogsForBatchId = groupState.mSendingBatches.remove(batchId);
        if (!HttpUtils.isRecoverableError(e))
            mPersistence.deleteLogs(groupName, batchId);
        else
            groupState.mPendingLogCount += removedLogsForBatchId.size();
        GroupListener groupListener = groupState.mListener;
        if (groupListener != null) {
            for (Log log : removedLogsForBatchId)
                groupListener.onFailure(log, e);
        }
        suspend(false);
    }

    /**
     * Actual implementation of enqueue logic. Will increase counters, triggers of batching logic.
     *
     * @param log       the Log to be enqueued
     * @param groupName the queue to use
     */
    @Override
    public synchronized void enqueue(@NonNull Log log, @NonNull String groupName) {

        /* Check group name is registered. */
        GroupState groupState = mGroupStates.get(groupName);
        if (groupState == null) {
            SonomaLog.error(Sonoma.LOG_TAG, "Invalid group name:" + groupName);
            return;
        }

        /* Call listeners so that they can decorate the log. */
        for (Listener listener : mListeners)
            listener.onEnqueuingLog(log, groupName);

        /* Attach device properties to every log if its not already attached by a feature. */
        if (log.getDevice() == null) {

            /* Generate device properties only once per process life time. */
            if (mDevice == null) {
                try {
                    mDevice = DeviceInfoHelper.getDeviceInfo(mContext);
                } catch (DeviceInfoHelper.DeviceInfoException e) {
                    SonomaLog.error(Sonoma.LOG_TAG, "Device log cannot be generated", e);
                    return;
                }
            }

            /* Attach device properties. */
            log.setDevice(mDevice);
        }

        /* Set an absolute timestamp, we'll convert to relative just before sending. Don't do it if the feature already set a timestamp.*/
        if (log.getToffset() == 0L)
            log.setToffset(System.currentTimeMillis());

        /* Persist log. */
        try {

            /* Save log in database. */
            mPersistence.putLog(groupName, log);
            groupState.mPendingLogCount++;
            SonomaLog.debug(Sonoma.LOG_TAG, "enqueue(" + groupName + ") pendingLogCount=" + groupState.mPendingLogCount);

            /* Increment counters and schedule ingestion if we are not disabled. */
            if (!mEnabled) {
                SonomaLog.warn(Sonoma.LOG_TAG, "Channel is disabled, event was saved to disk.");
            } else {
                checkPendingLogs(groupName);
            }
        } catch (Persistence.PersistenceException e) {
            SonomaLog.error(Sonoma.LOG_TAG, "Error persisting event with exception: " + e.toString());
        }
    }

    /**
     * Check for logs to trigger immediately or schedule with a timer or does nothing if no logs.
     *
     * @param groupName the group name.
     */
    private synchronized void checkPendingLogs(@NonNull String groupName) {
        GroupState groupState = mGroupStates.get(groupName);
        long pendingLogCount = groupState.mPendingLogCount;
        SonomaLog.debug(Sonoma.LOG_TAG, "checkPendingLogs(" + groupName + ") pendingLogCount=" + pendingLogCount);
        if (pendingLogCount >= groupState.mMaxLogsPerBatch)
            triggerIngestion(groupName);
        else if (pendingLogCount > 0 && !groupState.mScheduled) {
            groupState.mScheduled = true;
            mIngestionHandler.postDelayed(groupState.mRunnable, groupState.mBatchTimeInterval);
        }
    }

    @Override
    public synchronized void addListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public synchronized void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * State for a specific log group.
     */
    private class GroupState {

        /**
         * Group name
         */
        final String mName;

        /**
         * Maximum log count per batch.
         */
        final int mMaxLogsPerBatch;

        /**
         * Time to wait before 2 batches, in ms.
         */
        final long mBatchTimeInterval;

        /**
         * Maximum number of batches in parallel.
         */
        final int mMaxParallelBatches;

        /**
         * Batches being currently sent to ingestion.
         */
        final Map<String, List<Log>> mSendingBatches = new HashMap<>();

        /**
         * A listener for a feature.
         */
        final GroupListener mListener;

        /**
         * Pending log count not part of a batch yet.
         */
        int mPendingLogCount;

        /**
         * Is timer scheduled.
         */
        boolean mScheduled;

        /**
         * Runnable that triggers ingestion of this group data
         * and triggers itself in {@link #mBatchTimeInterval} ms.
         */
        final Runnable mRunnable = new Runnable() {

            @Override
            public void run() {
                mScheduled = false;
                triggerIngestion(mName);
            }
        };

        /**
         * Init.
         *
         * @param name               group name.
         * @param maxLogsPerBatch    max batch size.
         * @param batchTimeInterval  batch interval in ms.
         * @param maxParallelBatches max number of parallel batches.
         * @param listener           listener for a feature.
         */
        GroupState(String name, int maxLogsPerBatch, long batchTimeInterval, int maxParallelBatches, GroupListener listener) {
            mName = name;
            mMaxLogsPerBatch = maxLogsPerBatch;
            mBatchTimeInterval = batchTimeInterval;
            mMaxParallelBatches = maxParallelBatches;
            mListener = listener;
        }
    }
}
