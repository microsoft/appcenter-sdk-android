package avalanche.base.channel;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.HttpUtils;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.persistence.AvalanchePersistence;
import avalanche.base.persistence.DefaultAvalanchePersistence;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.IdHelper;
import avalanche.base.utils.PrefStorageConstants;
import avalanche.base.utils.StorageHelper;

public class DefaultAvalancheChannel implements AvalancheChannel {

    public static final String GROUP_ERROR = "group_error";
    public static final String GROUP_ANALYTICS = "group_analytics";


    private static final String TAG = "DefaultChannel";

    /**
     * Number of metrics queue items which will trigger synchronization with the persistence layer.
     */
    private static final int MAX_BATCH_COUNT_ANALYTICS = 5;

    /**
     * Number of error queue items which will trigger synchronization with the persistence layer.
     */
    private static final int MAX_BATCH_COUNT_ERROR = 1;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int MAX_BATCH_INTERVAL_ANALYTICS = 3 * 1000;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int MAX_BATCH_INTERVAL_ERROR = 3 * 1000;
    /**
     * Synchronization lock.
     */
    private static final Object LOCK = new Object();
    /**
     * Timer to run scheduled tasks on for metrics queue.
     */
    private final Timer mAnalyticsTimer;
    /**
     * Timer to run scheduled tasks on for error queue.
     */
    private final Timer mErrorTimer;
    private final UUID mAppKey;
    private final UUID mInstallId;
    private final int MAX_PENDING_COUNT = 3;
    private final DefaultAvalanchePersistence mPersistence;
    AvalancheIngestionHttp mIngestion;
    private int errorCounter = 0;
    private int analyticsCounter = 0;
    private ArrayList<String> errorBatchIds = new ArrayList<>(0);
    private ArrayList<String> analyticsBatchIds = new ArrayList<>(0);
    /**
     * Task to be scheduled for forwarding crashes at a certain max interval.
     */
    private TriggerIngestionTask mForwardErrorsTask;
    /**
     * Task to be scheduled for forwarding metrics at a certain max interval.
     */
    private TriggerIngestionTask mForwardAnalyticsTask;
    private boolean isDisabled;

    /**
     * Creates and initializes a new instance.
     */
    public DefaultAvalancheChannel() {
        mAnalyticsTimer = new Timer("Avalanche Metrics Queue", true);
        mErrorTimer = new Timer("Avalanche Error Queue", true);

        String appKeyString = StorageHelper.PreferencesStorage.getString(PrefStorageConstants.KEY_APP_KEY);
        mAppKey = UUID.fromString(appKeyString);

        mInstallId = IdHelper.getInstallId();
        mPersistence = new DefaultAvalanchePersistence();
        mIngestion = new AvalancheIngestionHttp();

        //TODO trigger mIngestion at creation and reset disabled flag
        isDisabled = false;

        triggerIngestion(GROUP_ERROR);
        triggerIngestion(GROUP_ANALYTICS);
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        if (log == null) {
            AvalancheLog.warn(TAG, "Tried to enqueue empty log. Doing nothing.");
            return;
        }
        try {
            mPersistence.putLog(queueName, log);
            if (!isDisabled) {
                countAndTime(queueName);
            } else {
                AvalancheLog.warn(TAG, "Channel is disabled, event was saved to disk.");
            }

        } catch (AvalanchePersistence.PersistenceException e) {
            //TODO (bereimol) add error handling?
            AvalancheLog.warn(TAG, "Error persisting event with exception: " + e.toString());
        }
    }

    private void countAndTime(@GroupNameDef String queueName) {
        synchronized (LOCK) {
            if (queueName.equals(GROUP_ANALYTICS)) {
                //Check if counter is 0, increase by 1 and kick of timer task
                switch (analyticsCounter) {
                    case 0:
                        analyticsCounter = 1;
                        mForwardAnalyticsTask = new TriggerIngestionTask(queueName);
                        mAnalyticsTimer.schedule(mForwardAnalyticsTask, MAX_BATCH_INTERVAL_ANALYTICS);
                        break;
                    case MAX_BATCH_COUNT_ANALYTICS:
                        //We have reached the max batch count. Trigger ingestion!
                        triggerIngestion(GROUP_ANALYTICS);
                        break;
                    default:
                        analyticsCounter = analyticsCounter + 1;
                }
            } else if (queueName.equals(GROUP_ERROR)) {
                //Check if counter is 0, increase by 1 and kick of timertask
                if (errorCounter == 0) {
                    errorCounter = 1;
                    mForwardErrorsTask = new TriggerIngestionTask(queueName);
                    mErrorTimer.schedule(mForwardErrorsTask, MAX_BATCH_INTERVAL_ERROR);
                } else if (errorCounter == MAX_BATCH_COUNT_ERROR) {
                    //We have reached the max batch count. Trigger ingestion.
                    triggerIngestion(GROUP_ERROR);
                }
            }

        }
    }

    /**
     * This will, if we're not using the limit for pending batches, trigger sending of a new request.
     * It will also reset the counters for sending out items for both the number of items enqueued and
     * the timer until we trigger sending data again. It will do this even if we don't have reached the limit
     * of pending batches.
     *
     * @param groupName the group name
     */
    private void triggerIngestion(@GroupNameDef @NonNull String groupName) {
        if (TextUtils.isEmpty(groupName)) {
            return;
        }

        synchronized (LOCK) {
            int limit;

            boolean isAnalytics = groupName.equals(GROUP_ANALYTICS);
            if (isAnalytics) {
                //Reset the counters
                analyticsCounter = 0;
                if (mForwardAnalyticsTask != null) {
                    mForwardAnalyticsTask.cancel();
                }

                limit = MAX_BATCH_COUNT_ANALYTICS;

                //Check if we haved reached the maximum number of pending batches, log to LogCat and don't trigger another sending
                if (analyticsBatchIds.size() == MAX_PENDING_COUNT) {
                    AvalancheLog.info(TAG, "Already sending 3 batches of analytics data to the server.");

                    return;
                }

            } else {
                //Reset the counters
                errorCounter = 0;
                if (mForwardErrorsTask != null) {
                    mForwardErrorsTask.cancel();
                }
                //Check if we have reached the maximum number of pending batches, log to LogCat and don't trigger another sending

                limit = MAX_BATCH_COUNT_ERROR;

                if (errorBatchIds.size() == MAX_PENDING_COUNT) {
                    AvalancheLog.info(TAG, "Already sending 3 batches of error data to the server.");
                    return;
                }
            }

            //Get a batch from persistence
            ArrayList<Log> list = new ArrayList<>(0);
            final String batchId = mPersistence.getLogs(groupName, limit, list);

            //Add batchIds to the list of batchIds and forward to ingestion for real
            if ((!TextUtils.isEmpty(batchId)) && (list.size() > 0)) {
                LogContainer logContainer = new LogContainer();
                logContainer.setLogs(list);

                if (isAnalytics) {
                    analyticsBatchIds.add(batchId);
                } else {
                    errorBatchIds.add(batchId);
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
        if (mAppKey == null) {
            AvalancheLog.error("Appkey is null");
            return;
        }
        if (mInstallId == null) {
            AvalancheLog.error("InstallId is null");
            return;
        }

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
     * The actual implementation to react to sending a batch to the server successfully
     *
     * @param groupName The group name.
     * @param batchId   The batch ID
     */
    private void handleSendingSuccess(@NonNull final String groupName, @NonNull final String batchId) {
        synchronized (LOCK) {
            final boolean isAnalytics = groupName.equals(GROUP_ANALYTICS);

            mPersistence.deleteLog(groupName, batchId);
            boolean removeBatchIdSuccessful = isAnalytics ? analyticsBatchIds.remove(batchId) : errorBatchIds.remove(batchId);
            if (!removeBatchIdSuccessful) {
                AvalancheLog.warn(TAG, "Error removing batchId after successfully sending data.");
            }
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
        synchronized (LOCK) {
            final boolean isAnalytics = groupName.equals(GROUP_ANALYTICS);

            boolean removeBatchIdSuccessful;
            if (HttpUtils.isRecoverableError(t)) {
                removeBatchIdSuccessful = isAnalytics ? analyticsBatchIds.remove(batchId) : errorBatchIds.remove(batchId);
                if (!removeBatchIdSuccessful) {
                    AvalancheLog.warn(TAG, "Error removing batchId after recoverable error");
                    isDisabled = true;
                }
            } else {
                mPersistence.deleteLog(groupName, batchId);
                removeBatchIdSuccessful = isAnalytics ? analyticsBatchIds.remove(batchId) : errorBatchIds.remove(batchId);
                if (!removeBatchIdSuccessful) {
                    AvalancheLog.warn(TAG, "Error removing batchId after non-recoverable error sending data");
                }
            }
        }
    }

    /**
     * Task to trigger ingestion after batch time interval has passed.
     */
    private class TriggerIngestionTask extends TimerTask {
        private final
        @GroupNameDef
        String mGroup;

        public TriggerIngestionTask(@NonNull @GroupNameDef String group) {
            mGroup = group;
        }

        @Override
        public void run() {
            triggerIngestion(mGroup);
        }
    }

}