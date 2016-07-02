package avalanche.base.channel;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
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
     * Timer to run scheduled tasks on for metrics queue.
     */
    private final Timer mAnalyticsTimer;

    /**
     * Timer to run scheduled tasks on for error queue.
     */
    private final Timer mErrorTimer;

    private final AtomicInteger errorCounter = new AtomicInteger(0);
    private final AtomicInteger analyticsCounter = new AtomicInteger(0);
    private final UUID mAppKey;
    private final UUID mInstallId;

    /**
     * Task to be scheduled for forwarding crashes at a certain max interval.
     */
    private TriggerIngestionTask forwardCrashesTask;

    /**
     * Task to be scheduled for forwarding metrics at a certain max interval.
     */
    private TriggerIngestionTask forwardAnalyticsTask;

    private final DefaultAvalanchePersistence mPersistence;

    /**
     * Creates and initializes a new instance.
     */
    public DefaultAvalancheChannel() {
        mAnalyticsTimer = new Timer("Avalanche Metrics Queue", true);
        mErrorTimer = new Timer("Avalanche Error Queue", true);
        mAppKey = getAppKey();
        mInstallId = IdHelper.getInstallId();
        mPersistence = new DefaultAvalanchePersistence();
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        if (log == null) {
            AvalancheLog.warn(TAG, "Tried to enqueue empty log");
            return;
        }

        try {
            mPersistence.putLog(queueName, log);
            countAndTime(queueName);
        }
        catch (AvalanchePersistence.PersistenceException e) {
            //TODO (bereimol) add error handling
        }
    }

    private void countAndTime(@GroupNameDef String queueName) {
        if (queueName.equals(GROUP_ANALYTICS)) {
            //Check if counter is 0, increase by 1 and kick of timer task
            if (analyticsCounter.compareAndSet(0, 1)) {
                forwardAnalyticsTask = new TriggerIngestionTask(queueName);
                mAnalyticsTimer.schedule(forwardAnalyticsTask, MAX_BATCH_INTERVAL_ANALYTICS);
            } else if (analyticsCounter.compareAndSet(MAX_BATCH_COUNT_ANALYTICS, 0)) {
                //We have reached the max batch count. Reset counter to 0 and triggerIngestion batch.
                triggerIngestion(GROUP_ANALYTICS);
            }
        } else {
            //Check if counter is 0, increase by 1 and kick of timertask
            if (errorCounter.compareAndSet(0, 1)) {
                forwardCrashesTask = new TriggerIngestionTask(queueName);
                mErrorTimer.schedule(forwardCrashesTask, MAX_BATCH_INTERVAL_ERROR);
            } else if (errorCounter.compareAndSet(MAX_BATCH_COUNT_ERROR, 0)) {
                //We have reached the max batch count. Reset counter to 0 and triggerIngestion batch.
                triggerIngestion(GROUP_ERROR);
            }
        }
    }

    private void triggerIngestion(@GroupNameDef String groupName) {
        int limit = groupName.equals(GROUP_ANALYTICS) ? 5 : 1;
        ArrayList<Log> list = new ArrayList<>(limit);
        final String batchId = mPersistence.getLogs(groupName, limit, list);

        //TODO (bereimol) add keeping track of batchID
        if((!TextUtils.isEmpty(batchId)) && (list.size() > 0)) {
            LogContainer logContainer = new LogContainer();
            logContainer.setLogs(list);
            ingestLogs(logContainer);
        }
    }

    private void ingestLogs(LogContainer logContainer) {
        AvalancheIngestionHttp ingestion = new AvalancheIngestionHttp();
        if (mAppKey == null) {
            AvalancheLog.error("Appkey is null");
            return;
        }
        if (mInstallId == null) {
            AvalancheLog.error("InstallId is null");
            return;
        }

        ingestion.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {
                    @Override
                    public void success() {
                        //TODO Persistence - delete batch
                    }

                    @Override
                    public void failure(Throwable t) {
                        handleFailure(t);
                    }
                }
        );
    }

    private void handleFailure(Throwable t) {

    }

    private UUID getAppKey() {
        String appKeyString = StorageHelper.PreferencesStorage.getString(PrefStorageConstants.KEY_APP_ID);
        UUID appKey = UUID.fromString(appKeyString);

        return appKey;
    }

    /**
     * Task to fire off after batch time interval has passed.
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