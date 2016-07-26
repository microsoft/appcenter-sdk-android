package avalanche.core.channel;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.http.AvalancheIngestionHttp;
import avalanche.core.ingestion.http.AvalancheIngestionNetworkStateHandler;
import avalanche.core.ingestion.http.AvalancheIngestionRetryer;
import avalanche.core.ingestion.http.DefaultUrlConnectionFactory;
import avalanche.core.ingestion.http.HttpUtils;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.persistence.AvalancheDatabasePersistence;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.IdHelper;
import avalanche.core.utils.NetworkStateHelper;

public class DefaultAvalancheChannel implements AvalancheChannel {

    /* TODO: Moved to feature class. Use getGroupName() instead. Will be removed soon. */
    public static final String ERROR_GROUP = "group_error";
    public static final String ANALYTICS_GROUP = "group_analytics";

    /**
     * Synchronization lock.
     */
    private static final Object LOCK = new Object();

    /**
     * TAG used in logging.
     */
    private static final String TAG = "AvalancheChannel";

    /**
     * Number of metrics queue items which will trigger synchronization with the persistence layer.
     */
    private static final int ANALYTICS_COUNT = 50;

    /**
     * Number of error queue items which will trigger synchronization with the persistence layer.
     */
    private static final int ERROR_COUNT = 1;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int ANALYTICS_INTERVAL = 3 * 1000;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int ERROR_INTERVAL = 3 * 1000;

    /**
     * Maximum number of requests being sent for analytics group.
     */
    private static final int ANALYTICS_MAX_PARALLEL_REQUESTS = 3;

    /**
     * Maximum number of requests being sent for error group.
     */
    private static final int ERROR_MAX_PARALLEL_REQUESTS = 3;

    /**
     * The appKey that's required for forwarding to ingestion.
     */
    private final UUID mAppKey;

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
     * The persistence object used to store events in the local storage.
     */
    private AvalanchePersistence mPersistence;

    /**
     * The ingestion object used to send batches to the server.
     */
    private AvalancheIngestion mIngestion;

    /**
     * Is channel enabled?
     */
    private boolean mEnabled;

    /**
     * Creates and initializes a new instance.
     */
    public DefaultAvalancheChannel(@NonNull Context context, @NonNull UUID appKey, @NonNull LogSerializer logSerializer) {
        mAppKey = appKey;
        mInstallId = IdHelper.getInstallId();
        mPersistence = new AvalancheDatabasePersistence();
        mPersistence.setLogSerializer(logSerializer);
        AvalancheIngestionHttp api = new AvalancheIngestionHttp(new DefaultUrlConnectionFactory(), logSerializer);
        api.setBaseUrl("http://avalanche-perf.westus.cloudapp.azure.com:8081"); //TODO make that a parameter
        AvalancheIngestionRetryer retryer = new AvalancheIngestionRetryer(api);
        mIngestion = new AvalancheIngestionNetworkStateHandler(retryer, NetworkStateHelper.getSharedInstance(context));
        mIngestionHandler = new Handler(Looper.getMainLooper());
        mGroupStates = new LinkedHashMap<>(); // FIXME unit tests seems to make an assumption about order of triggerIngestion calls in triggerIngestion
        mGroupStates.put(ANALYTICS_GROUP, new GroupState(ANALYTICS_GROUP, ANALYTICS_COUNT, ANALYTICS_INTERVAL, ANALYTICS_MAX_PARALLEL_REQUESTS));
        mGroupStates.put(ERROR_GROUP, new GroupState(ERROR_GROUP, ERROR_COUNT, ERROR_INTERVAL, ERROR_MAX_PARALLEL_REQUESTS));
        mEnabled = true;
    }

    /**
     * Overloaded constructor with limited visibility that allows for dependency injection.
     *
     * @param context       the context
     * @param appKey        the appKey
     * @param ingestion     ingestion object for dependency injection
     * @param persistence   persistence object for dependency injection
     * @param logSerializer log serializer object for dependency injection
     */
    @VisibleForTesting
    DefaultAvalancheChannel(@NonNull Context context, @NonNull UUID appKey, @NonNull AvalancheIngestion ingestion, @NonNull AvalanchePersistence persistence, @NonNull LogSerializer logSerializer) {
        this(context, appKey, logSerializer);
        mPersistence = persistence;
        mIngestion = ingestion;
    }

    /**
     * Setter for persistence object, to be used for dependency injection.
     *
     * @param mPersistence the persistence object.
     */
    @VisibleForTesting
    void setPersistence(AvalanchePersistence mPersistence) {
        this.mPersistence = mPersistence;
    }

    @Override
    public boolean isEnabled() {
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
    public void setEnabled(boolean enabled) {
        synchronized (LOCK) {
            if (enabled)
                mEnabled = true;
            else
                suspend(true);
        }
    }

    /**
     * Delete all persisted logs for the given group.
     *
     * @param groupName the group name.
     */
    @Override
    public void clear(String groupName) {
        mPersistence.deleteLogs(groupName);
    }

    /**
     * Stop sending logs until app restarted or the channel is enabled again.
     *
     * @param deleteLogs in addition to suspending, if this is true, delete all logs from persistence.
     */
    private void suspend(boolean deleteLogs) {
        synchronized (LOCK) {
            mEnabled = false;
            for (GroupState groupState : mGroupStates.values()) {
                resetThresholds(groupState.mName);
                groupState.mSendingBatches.clear();
            }
            try {
                mIngestion.close();
            } catch (IOException e) {
                AvalancheLog.error("Failed to close ingestion", e);
            }
            if (deleteLogs)
                mPersistence.clear();
            else
                mPersistence.clearPendingLogState();
        }
    }

    /**
     * Reset the counter for a group and restart the timer.
     *
     * @param groupName the group name.
     */
    private void resetThresholds(@GroupNameDef String groupName) {
        synchronized (LOCK) {
            GroupState groupState = mGroupStates.get(groupName);
            setCounter(groupName, 0);
            mIngestionHandler.removeCallbacks(groupState.mRunnable);
            if (mEnabled)
                mIngestionHandler.postDelayed(groupState.mRunnable, groupState.mBatchTimeInterval);
        }
    }

    @VisibleForTesting
    int getCounter(@GroupNameDef String groupName) {
        synchronized (LOCK) {
            return mGroupStates.get(groupName).mPendingLogCount;
        }
    }

    /**
     * Update group pending log counter.
     *
     * @param counter new counter value.
     */
    private void setCounter(@GroupNameDef String groupName, int counter) {
        synchronized (LOCK) {
            mGroupStates.get(groupName).mPendingLogCount = counter;
        }
    }

    /**
     * Setter for ingestion dependency, intended to be used for dependency injection.
     *
     * @param ingestion the ingestion object.
     */
    void setIngestion(AvalancheIngestion ingestion) {
        this.mIngestion = ingestion;
    }

    /**
     * This will reset the counters and timers for the event groups and trigger ingestion immediately.
     * Intended to be used after disabling and re-enabling the Channel.
     */
    public void triggerIngestion() {
        synchronized (LOCK) {
            if (mEnabled) {
                for (String groupName : mGroupStates.keySet())
                    triggerIngestion(groupName);
            }
        }
    }

    /**
     * This will, if we're not using the limit for pending batches, trigger sending of a new request.
     * It will also reset the counters for sending out items for both the number of items enqueued and
     * the handlers. It will do this even if we don't have reached the limit
     * of pending batches or the time interval.
     *
     * @param groupName the group name
     */
    private void triggerIngestion(@GroupNameDef @NonNull String groupName) {
        synchronized (LOCK) {
            AvalancheLog.debug("triggerIngestion(" + groupName + ")");

            if (TextUtils.isEmpty(groupName) || (mAppKey == null) || (mInstallId == null) || !mEnabled) {
                return;
            }

            //Reset counter and timer
            resetThresholds(groupName);
            GroupState groupState = mGroupStates.get(groupName);
            int limit = groupState.mMaxLogsPerBatch;

            //Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending.
            //condition to stop recursion
            if (groupState.mSendingBatches.size() == groupState.mMaxParallelBatches) {
                AvalancheLog.debug(TAG, "Already sending " + groupState.mMaxParallelBatches + " batches of analytics data to the server.");
                return;
            }

            //Get a batch from persistence
            ArrayList<Log> list = new ArrayList<>(0);
            String batchId = mPersistence.getLogs(groupName, limit, list);

            //Add batchIds to the list of batchIds and forward to ingestion for real
            if ((!TextUtils.isEmpty(batchId)) && (list.size() > 0)) {
                LogContainer logContainer = new LogContainer();
                logContainer.setLogs(list);

                groupState.mSendingBatches.add(batchId);
                ingestLogs(groupName, batchId, logContainer);

                //if we have sent a batch that was the maximum amount of logs, we trigger sending once more
                //to make sure we send data that was stored on disc
                if (list.size() == limit) {
                    triggerIngestion(groupName);
                }
            }
        }
    }

    /**
     * Forward LogContainer to Ingestion and implement callback to handle success or failure.
     *
     * @param groupName    the GroupName for each batch
     * @param batchId      the ID of the batch
     * @param logContainer a LogContainer object containing several logs
     */
    private void ingestLogs(@NonNull final String groupName, @NonNull final String batchId, @NonNull LogContainer logContainer) {
        AvalancheLog.debug(TAG, "ingestLogs(" + groupName + "," + batchId + ")");
        mIngestion.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {
                    @Override
                    public void success() {
                        handleSendingSuccess(groupName, batchId);
                    }

                    @Override
                    public void failure(Throwable t) {
                        handleSendingFailure(groupName, batchId, t);
                    }
                }
        );
    }

    /**
     * The actual implementation to react to sending a batch to the server successfully.
     *
     * @param groupName The group name
     * @param batchId   The batch ID
     */
    private void handleSendingSuccess(@NonNull final String groupName, @NonNull final String batchId) {
        synchronized (LOCK) {
            GroupState groupState = mGroupStates.get(groupName);

            mPersistence.deleteLogs(groupName, batchId);
            boolean removeBatchIdSuccessful = groupState.mSendingBatches.remove(batchId);
            if (!removeBatchIdSuccessful) {
                AvalancheLog.warn(TAG, "Error removing batchId after successfully sending data.");
            }

            triggerIngestion(groupName);
        }
    }

    /**
     * The actual implementation to react to not being able to send a batch to the server.
     * Will disable the sender in case of a recoverable error.
     * Will delete batch of data in case of a non-recoverable error.
     *
     * @param groupName the group name
     * @param batchId   the batch ID
     * @param t         the error
     */
    private void handleSendingFailure(@NonNull final String groupName, @NonNull final String batchId, @NonNull final Throwable t) {
        if (!HttpUtils.isRecoverableError(t))
            mPersistence.deleteLogs(groupName, batchId);
        if (!mGroupStates.get(groupName).mSendingBatches.remove(batchId))
            AvalancheLog.warn(TAG, "Error removing batchId after sending failure.");
        suspend(false);
    }

    /**
     * Actual implementation of enqueue logic. Will increase counters, triggers of batching logic.
     *
     * @param log       the Log to be enqueued
     * @param queueName the queue to use
     */
    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        try {
            // persist log with an absolute timestamp, we'll convert to relative just before sending
            log.setToffset(System.currentTimeMillis());
            mPersistence.putLog(queueName, log);

            //Increment counters and schedule ingestion if we are not disabled
            if (!mEnabled) {
                AvalancheLog.warn(TAG, "Channel is disabled, event was saved to disk.");
            } else {
                scheduleIngestion(queueName);
            }
        } catch (AvalanchePersistence.PersistenceException e) {
            AvalancheLog.error(TAG, "Error persisting event with exception: " + e.toString());
        }
    }

    /**
     * This will check the counters for each event group and will either trigger ingestion immediately or schedule ingestion at the
     * interval specified for the group.
     *
     * @param groupName the group name
     */
    private void scheduleIngestion(@GroupNameDef String groupName) {
        synchronized (LOCK) {
            GroupState groupState = mGroupStates.get(groupName);
            int counter = groupState.mPendingLogCount;
            int maxCount = groupState.mMaxLogsPerBatch;
            if (counter == 0) {
                //Kick of timer if the counter is 0 and cancel previously running timer
                resetThresholds(groupName);
            }

            //increment counter
            counter = counter + 1;
            if (counter == maxCount) {
                counter = 0;
                //We have reached the max batch count or a multiple of it. Trigger ingestion.
                triggerIngestion(groupName);
            }

            //set the counter property
            setCounter(groupName, counter);
        }
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
        final int mBatchTimeInterval;
        /**
         * Maximum number of batches in parallel.
         */
        final int mMaxParallelBatches;
        /**
         * Batches being currently sent to ingestion.
         */
        final Collection<String> mSendingBatches = new ArrayList<>();

        /**
         * Pending log count not part of a batch yet.
         */
        int mPendingLogCount;

        /**
         * Runnable that triggers ingestion of this group data
         * and triggers itself in {@link #mBatchTimeInterval} ms.
         */
        final Runnable mRunnable = new Runnable() {

            @Override
            public void run() {
                if (mPendingLogCount > 0) {
                    triggerIngestion(mName);
                }
                mIngestionHandler.postDelayed(this, mBatchTimeInterval);
            }
        };

        /**
         * Init.
         *
         * @param name               group name.
         * @param maxLogsPerBatch    max batch size.
         * @param batchTimeInterval  batch interval in ms.
         * @param maxParallelBatches max number of parallel batches.
         */
        GroupState(String name, int maxLogsPerBatch, int batchTimeInterval, int maxParallelBatches) {
            mName = name;
            mMaxLogsPerBatch = maxLogsPerBatch;
            mBatchTimeInterval = batchTimeInterval;
            mMaxParallelBatches = maxParallelBatches;
        }
    }
}
