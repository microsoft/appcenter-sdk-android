package avalanche.base.channel;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.HttpUtils;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.persistence.AvalanchePersistence;
import avalanche.base.utils.AvalancheLog;

public class DefaultAvalancheChannel implements AvalancheChannel {
    /**
     * Constant marking event of the error group.
     */
    public static final String ERROR_GROUP = "group_error";

    /**
     * Constant marking event of the analytics group.
     */
    public static final String ANALYTICS_GROUP = "group_analytics";

    /**
     * Synchronization lock.
     */
    private static final Object LOCK = new Object();

    /**
     * TAG used in logging.
     */
    private static final String TAG = "DefaultChannel";

    /**
     * Number of metrics queue items which will trigger synchronization with the persistence layer.
     */
    private static final int ANALYTICS_COUNT = 5;

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
     * Maximum number of pending event batches per event group.
     */
    private static final int MAX_PENDING_COUNT = 3;
    /**
     * The installId that's required for forwarding to ingestion.
     */
    private final UUID mInstallId;
    /**
     * ArrayList of batchIds for error batches.
     */
    private final List<String> mErrorBatchIds = new ArrayList<>(0);
    /**
     * ArrayList of batchIds for analytics batches.
     */
    private final List<String> mAnalyticsBatchIds = new ArrayList<>(0);
    /**
     * Handler for triggering ingestion of events.
     */
    private final Handler mIngestionHandler;
    /**
     * The persistence object used to store events in the local storage.
     */
    private AvalanchePersistence mPersistence;
    /**
     * The ingestion object used to send batches to the server.
     */
    private AvalancheIngestion mIngestion;
    /**
     * The appKey that's required for forwarding to ingestion.
     */
    private UUID mAppKey = null;
    /**
     * Counter for error events.
     */
    private int mErrorCounter = 0;
    /**
     * Counter for analytics events.
     */
    private int mAnalyticsCounter = 0;
    /**
     * Property that indicates disabled channel.
     */
    private boolean mDisabled;
    /**
     * Runnable that triggers ingestion of analytics data and triggers itself in ANALYTICS_INTERVAL
     * amount of ms.
     */
    private final Runnable mAnalyticsRunnable = new Runnable() {
        @Override
        public void run() {
            triggerIngestion(ANALYTICS_GROUP);
            mIngestionHandler.postDelayed(this, ANALYTICS_INTERVAL);
        }
    };
    /**
     * Runnable that triggers ingestion of error data and triggers itself in ERROR_INTERVAL
     * amount of ms.
     */
    private final Runnable mErrorRunnable = new Runnable() {
        @Override
        public void run() {
            triggerIngestion(ERROR_GROUP);
            mIngestionHandler.postDelayed(this, ERROR_INTERVAL);
        }
    };

    /**
     * Creates and initializes a new instance.
     */
    public DefaultAvalancheChannel(@NonNull UUID appKey, @NonNull UUID installId, @NonNull AvalanchePersistence persistence, @NonNull AvalancheIngestion ingestion) {
        mAppKey = appKey;
        mInstallId = installId;
        mPersistence = persistence;
        mIngestion = ingestion;
        mIngestionHandler = new Handler(Looper.getMainLooper());

        mDisabled = false;
    }

    boolean isDisabled() {
        return mDisabled;
    }

    /**
     * Set the disabled flag. If true, the channel will continue to persist data but not forward any item to ingestion.
     * The most common use-case would be to set it to true and enable sending again after the channel has disabled itself after receiving
     * a recoverable error (most likely related to a server issue).
     *
     * @param disabled flag to disable the Channel.
     */
    void setDisabled(boolean disabled) {
        mDisabled = disabled;
    }

    int getErrorCounter() {
        return mErrorCounter;
    }

    int getAnalyticsCounter() {
        return mAnalyticsCounter;
    }

    //TODO(bereimol) add missing comments
    void setIngestion(AvalancheIngestion ingestion) {
        this.mIngestion = ingestion;
    }

    /**
     * This will reset the counters and timers for the event groups and trigger ingestion immediately.
     * Intended to be used after disabling and re-enabling the Channel.
     */
    public void triggerIngestion() {
        //TODO (bereimol) trigger max amount of sending operations immediatelly
        //Explanation: this triggers sending for 1 batch, and then will trigger the next batch after 3s.
        //If the requests take longer than 3s, it will start to use the max amount of sending operations at some point or not.
        //We need to trigger the maximum amount of sending immediatelly. Either by starting the max amount of sending immediatelly
        //or by retrieving the amount of files logs on disk and then increment the counter which will trigger more sending operations.
        synchronized (LOCK) {
                triggerIngestion(ANALYTICS_GROUP);
                triggerIngestion(ERROR_GROUP);
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
    protected void triggerIngestion(@GroupNameDef @NonNull String groupName) {
        if (TextUtils.isEmpty(groupName)) {
            return;
        }
        if (mAppKey == null) {
            AvalancheLog.error("AppKey is null");
            return;
        }
        if (mInstallId == null) {
            AvalancheLog.error("InstallId is null");
            return;
        }

        synchronized (LOCK) {
            int limit;

            boolean isAnalytics = groupName.equals(ANALYTICS_GROUP); //TODO(bereimol) move to list in case we get other types
            if (isAnalytics) {
                //Restart runnable.
                mIngestionHandler.removeCallbacks(mAnalyticsRunnable);
                mIngestionHandler.postDelayed(mAnalyticsRunnable, ANALYTICS_INTERVAL); //TODO (bereimol)interfers with counter logic

                limit = ANALYTICS_COUNT;

                //Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending.
                if (mAnalyticsBatchIds.size() == MAX_PENDING_COUNT) {
                    AvalancheLog.info(TAG, "Already sending 3 batches of analytics data to the server.");
                    return;
                }

            } else {
                //Reset the counters and restart runnable.
                mIngestionHandler.removeCallbacks(mErrorRunnable);
                mIngestionHandler.postDelayed(mErrorRunnable, ERROR_INTERVAL);

                //Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending

                limit = ERROR_COUNT;

                if (mErrorBatchIds.size() == MAX_PENDING_COUNT) {
                    AvalancheLog.info(TAG, "Already sending 3 batches of error data to the server.");
                    return;
                }
            }

            //Get a batch from persistence
            ArrayList<Log> list = new ArrayList<>(0);

            String batchId = mPersistence.getLogs(groupName, limit, list);

            //Add batchIds to the list of batchIds and forward to ingestion for real
            if ((!TextUtils.isEmpty(batchId)) && (list.size() > 0)) {
                LogContainer logContainer = new LogContainer();
                logContainer.setLogs(list);

                if (isAnalytics) {
                    mAnalyticsBatchIds.add(batchId);
                } else {
                    mErrorBatchIds.add(batchId);
                }
                ingestLogs(groupName, batchId, logContainer);
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
        final int size = logContainer.getLogs().size();
        mIngestion.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {
                    @Override
                    public void success() {
                        handleSendingSuccess(groupName, batchId, size);
                    }

                    @Override
                    public void failure(Throwable t) {
                        handleSendingFailure(groupName, batchId, size, t);
                    }
                }
        );
    }

    //TODO: (bereimol) fetch persisted items and ingest immediatelly, maybe increase batchsize a little for offline usecase

    /**
     * The actual implementation to react to sending a batch to the server successfully.
     *
     * @param groupName The group name
     * @param batchId   The batch ID
     */
    private void handleSendingSuccess(@NonNull final String groupName, @NonNull final String batchId, int size) {
        synchronized (LOCK) {
            final boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);

            mPersistence.deleteLog(groupName, batchId);
            boolean removeBatchIdSuccessful = isAnalytics ? mAnalyticsBatchIds.remove(batchId) : mErrorBatchIds.remove(batchId);
            if (!removeBatchIdSuccessful) {
                AvalancheLog.warn(TAG, "Error removing batchId after successfully sending data.");
            }

            decrement(groupName, size);
            scheduleIngestion(groupName);
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
    private void handleSendingFailure(@NonNull final String groupName, @NonNull final String batchId, int size, @NonNull final Throwable t) {
        synchronized (LOCK) {
            final boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);

            boolean removeBatchIdSuccessful;
            if (HttpUtils.isRecoverableError(t)) {
                removeBatchIdSuccessful = isAnalytics ? mAnalyticsBatchIds.remove(batchId) : mErrorBatchIds.remove(batchId);
                if (!removeBatchIdSuccessful) {
                    AvalancheLog.warn(TAG, "Error removing batchId after recoverable error");
                }
                mDisabled = true;
            } else {
                mPersistence.deleteLog(groupName, batchId);
                removeBatchIdSuccessful = isAnalytics ? mAnalyticsBatchIds.remove(batchId) : mErrorBatchIds.remove(batchId);
                if (!removeBatchIdSuccessful) {
                    AvalancheLog.warn(TAG, "Error removing batchId after non-recoverable error sending data");
                }

                decrement(groupName, size);
                scheduleIngestion(groupName);
            }
        }
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        if (log == null) {
            AvalancheLog.warn(TAG, "Tried to enqueue empty log. Doing nothing.");
            return;
        }
        try {
            mPersistence.putLog(queueName, log);

            //Increment counters and schedule ingestion
            synchronized (LOCK) {
                increment(queueName);
                scheduleIngestion(queueName);
                if (mDisabled) {
                    AvalancheLog.warn(TAG, "Channel is disabled, event was saved to disk.");
                }
            }
        } catch (AvalanchePersistence.PersistenceException e) {
            //TODO (bereimol) add error handling?
            AvalancheLog.warn(TAG, "Error persisting event with exception: " + e.toString());
        }
    }

    /**
     * This will increment the counter for the event group.
     * Intended to be used from inside a synchronized-block.
     *
     * @param groupName The group name
     */
    protected void increment(@GroupNameDef String groupName) {
        boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);
        int counter = isAnalytics ? mAnalyticsCounter : mErrorCounter;

        counter = counter + 1;

        if (isAnalytics) {
            mAnalyticsCounter = counter;
        } else {
            mErrorCounter = counter;
        }
    }

    protected void decrement(@GroupNameDef String groupName, @IntRange(from = 0) int dectementBy) {
        boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);
        int counter = isAnalytics ? mAnalyticsCounter : mErrorCounter;
        counter = counter - dectementBy;

        if (counter < 0) {
            counter = 0;

        }

        if (isAnalytics) {
            mAnalyticsCounter = counter;
        } else {
            mErrorCounter = counter;
        }
    }

    /**
     * This will check the counters for each event group and will either trigger ingestion immediatelly or schedule ingestion at the
     * interval specified for the group.
     * Intended to be used from inside a synchronized-block.
     *
     * @param groupName the group name
     */
    protected void scheduleIngestion(@GroupNameDef String groupName) {
       synchronized (LOCK) {
           boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);

           int counter = isAnalytics ? mAnalyticsCounter : mErrorCounter;
           int maxCount = isAnalytics ? ANALYTICS_COUNT : ERROR_COUNT;

           if (counter == 0) {
               //Check if counter is 0, kick of timer task.
               if (isAnalytics) {
                mIngestionHandler.postDelayed(mAnalyticsRunnable, ANALYTICS_INTERVAL);
               } else {
                mIngestionHandler.postDelayed(mErrorRunnable, ERROR_INTERVAL);
               }
           } else if (counter % maxCount == 0) {
               //We have reached the max batch count or a multiple of it. Trigger ingestion.
               if (isAnalytics) {
                   triggerIngestion(ANALYTICS_GROUP);
               } else {
                   triggerIngestion(ERROR_GROUP);
               }
           }
       }
    }
}