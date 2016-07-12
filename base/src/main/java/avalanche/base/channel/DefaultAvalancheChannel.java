package avalanche.base.channel;

import android.content.Context;
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
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.AvalancheIngestionNetworkStateHandler;
import avalanche.base.ingestion.http.AvalancheIngestionRetryer;
import avalanche.base.ingestion.http.DefaultUrlConnectionFactory;
import avalanche.base.ingestion.http.HttpUtils;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.persistence.AvalancheDatabasePersistence;
import avalanche.base.persistence.AvalanchePersistence;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.IdHelper;
import avalanche.base.utils.NetworkStateHelper;

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
    private static final String TAG = "AvalancheChannel";

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
    public DefaultAvalancheChannel(@NonNull Context context, @NonNull UUID appKey, @NonNull LogSerializer logSerializer) {
        mAppKey = appKey;
        mInstallId = IdHelper.getInstallId();
        mPersistence = new AvalancheDatabasePersistence();
        AvalancheIngestionHttp api = new AvalancheIngestionHttp();
        api.setUrlConnectionFactory(new DefaultUrlConnectionFactory());
        api.setLogSerializer(logSerializer);
        api.setBaseUrl("http://avalanche-perf.westus.cloudapp.azure.com:8081"); //TODO make that a parameter
        AvalancheIngestionRetryer retryer = new AvalancheIngestionRetryer(api);
        mIngestion = new AvalancheIngestionNetworkStateHandler(retryer, NetworkStateHelper.getSharedInstance(context));
        mIngestionHandler = new Handler(Looper.getMainLooper());
        mDisabled = false;
    }

    public void setPersistence(AvalanchePersistence mPersistence) {
        this.mPersistence = mPersistence;
    }

    /**
     * Flag to check if the channel was disabled.
     *
     * @return isDisabled.
     */
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

    /**
     * Getter to get error count for tests.
     *
     * @return the error counter
     */
    int getErrorCounter() {
        synchronized (LOCK) {
            return mErrorCounter;
        }
    }

    void setErrorCounter(int errorCounter) {
        synchronized (LOCK) {
            mErrorCounter = errorCounter;
        }
    }

    /**
     * Getter to get analytics count for tests.
     *
     * @return the analytics counter
     */
    int getAnalyticsCounter() {
        synchronized (LOCK) {
            return mAnalyticsCounter;
        }
    }

    void setAnalyticsCounter(int analyticsCounter) {
        synchronized (LOCK) {
            mAnalyticsCounter = analyticsCounter;
        }
    }

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
        //We need to trigger the maximum amount of sending immediately. Either by starting the max amount of sending immediatelly
        //or by retrieving the amount of files logs on disk and then increment the counter which will trigger more sending operations.
        synchronized (LOCK) {
            if (!mDisabled) {
                triggerIngestion(ANALYTICS_GROUP);
                triggerIngestion(ERROR_GROUP);
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


        AvalancheLog.debug("triggerIngestion(" + groupName + ")");
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
                mIngestionHandler.postDelayed(mAnalyticsRunnable, ANALYTICS_INTERVAL);

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

    //TODO: (bereimol) fetch persisted items and ingest immediately, maybe increase batchsize a little for offline usecase

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

    /**
     * Actual implementation of enqueue logic. Will increase counters, triggers of batching logic.
     *
     * @param log       the Log to be enqueued
     * @param queueName the queue to use
     */
    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        try {
            mPersistence.putLog(queueName, log);

            //Increment counters and schedule ingestion
            synchronized (LOCK) {
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
     * Decrements counter for a group.
     * Will set the counter to 0 if decrementBy is bigger then counter to avoid a negative counter.
     *
     * @param groupName   the group name
     * @param decrementBy amount to decrement the counter
     */
    private void decrement(@GroupNameDef String groupName, @IntRange(from = 0) int decrementBy) {
        synchronized (LOCK) {
            boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);
            int counter = isAnalytics ? getAnalyticsCounter() : getErrorCounter();
            counter = counter - decrementBy;

            if (counter < 0) {
                counter = 0;
            }

            if (isAnalytics) {
                setAnalyticsCounter(counter);
            } else {
                setErrorCounter(counter);
            }

            if (counter == 0) {
                if (isAnalytics) {
                    mIngestionHandler.removeCallbacks(mAnalyticsRunnable);
                } else {
                    mIngestionHandler.removeCallbacks(mErrorRunnable);
                }
            }
        }
    }

    /**
     * This will check the counters for each event group and will either trigger ingestion immediately or schedule ingestion at the
     * interval specified for the group.
     * Intended to be used from inside a synchronized-block.
     *
     * @param groupName the group name
     */
    private void scheduleIngestion(@GroupNameDef String groupName) {
        synchronized (LOCK) {

            boolean isAnalytics = groupName.equals(ANALYTICS_GROUP);

            int counter = isAnalytics ? mAnalyticsCounter : mErrorCounter;
            int maxCount = isAnalytics ? ANALYTICS_COUNT : ERROR_COUNT;

            if (counter == 0) {
                //Kick of timer if the counter is 0 and cancel previously running timer
                if (!mDisabled) {
                    if (isAnalytics) {
                        mIngestionHandler.removeCallbacks(mAnalyticsRunnable);
                        mIngestionHandler.postDelayed(mAnalyticsRunnable, ANALYTICS_INTERVAL);
                    } else {
                        mIngestionHandler.removeCallbacks(mAnalyticsRunnable);
                        mIngestionHandler.postDelayed(mErrorRunnable, ERROR_INTERVAL);
                    }
                }
                //increment counter
                counter = counter + 1;
            } else {
                //just increment the counter
                counter = counter + 1;
            }

            //set the counter property
            if (isAnalytics) {
                setAnalyticsCounter(counter);
            } else {
                setErrorCounter(counter);
            }

            if (counter % maxCount == 0) {
                //We have reached the max batch count or a multiple of it. Trigger ingestion.
                if (!mDisabled) {
                    if (isAnalytics) {
                        triggerIngestion(ANALYTICS_GROUP);
                    } else {
                        triggerIngestion(ERROR_GROUP);
                    }
                }
            }
        }
    }
}
