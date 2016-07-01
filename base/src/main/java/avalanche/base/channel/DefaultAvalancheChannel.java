package avalanche.base.channel;

import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import avalanche.base.ingestion.HttpException;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.IdHelper;
import avalanche.base.utils.PrefStorageConstants;
import avalanche.base.utils.StorageHelper;

public class DefaultAvalancheChannel implements AvalancheChannel {

    public static final int HIGH_PRIORITY = 0;
    public static final int REGULAR_PRIORITY = 1;


    private static final String TAG = "DefaultChannel";

    /**
     * Number of queue items which will trigger synchronization with the persistence layer.
     */
    private static final int MAX_BATCH_COUNT = 5;
    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int MAX_BATCH_INTERVALL = 3 * 1000;
    /**
     * Timer to run scheduled tasks on.
     */
    private final Timer timer;
    private final AtomicInteger logCount = new AtomicInteger(0);
    /**
     * Task to be scheduled for synchronizing at a certain max interval.
     */
    private ForwardBatchTask forwardBatchTask;

    /**
     * Creates and initializes a new instance.
     */
    public DefaultAvalancheChannel() {
        timer = new Timer("HockeyApp User Metrics Sender Queue", true);
    }


    @Override
    public void enqueue(Log log) {
        enqueue(log, REGULAR_PRIORITY);
    }

    @Override
    public void enqueue(Log log, int priority) {
        if (log == null) {
            AvalancheLog.warn(TAG, "Tried to enqueue empty log");
            return;
        }

        //TODO Persist the log

        //Check if counter is 0, increase by 1 and kick of timertask
        if(logCount.compareAndSet(0, 1)) {
            forwardBatchTask = new ForwardBatchTask();
            timer.schedule(forwardBatchTask, MAX_BATCH_INTERVALL);
        }
        else if (logCount.compareAndSet(MAX_BATCH_COUNT, 0)) {
            //We have reached the max batch count. Reset counter to 0 and forward batch.
            forward();
        }

    }


    @Override
    public void success() {

    }

    @Override
    public void failure(Throwable t) {

    }

    private void forward() {
        //TODO get batch from persistence and forward to ingestion

        AvalancheIngestionHttp ingestion = new AvalancheIngestionHttp();
        UUID installId = IdHelper.getInstallId();
        String appId = StorageHelper.PreferencesStorage.getString(PrefStorageConstants.KEY_APP_ID);
        LogContainer logContainer = null;

        ingestion.sendAsync(appId, installId, logContainer, new ServiceCallback() {
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
        if (t instanceof HttpException) {
            int httpStatusCode = ((HttpException) t).getStatusCode();
            if ((httpStatusCode == 408) || (httpStatusCode == 429) || (httpStatusCode >= 500)) {
                //TODO Persistence - flag batch as retryable
            } else {
                //TODO Persistence - delete batch
            }
        } else {
            //TODO Persistence – flag as retryable
        }
    }

    /**
     * Task to fire off after batch time interval has passed.
     */
    private class ForwardBatchTask extends TimerTask {

        public ForwardBatchTask() {
        }

        @Override
        public void run() {
            forward();
        }
    }

}