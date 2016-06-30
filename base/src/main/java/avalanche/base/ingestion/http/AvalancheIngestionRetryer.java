package avalanche.base.ingestion.http;

import android.os.Handler;
import android.os.Looper;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.AvalancheLog;

public class AvalancheIngestionRetryer extends AvalancheIngestionDecorator {

    /**
     * Retry intervals to use, array index is to use the value for each retry. When we used all the array values, we give up and forward the last error.
     */
    private static final long[] RETRY_INTERVALS = new long[]{
            TimeUnit.SECONDS.toMillis(10),
            TimeUnit.MINUTES.toMillis(5),
            TimeUnit.MINUTES.toMillis(20)
    };

    /**
     * Retry intervals for this instance.
     */
    private final long[] mRetryIntervals;

    /**
     * Android "timer" using the main thread loop.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Random object for interval randomness.
     */
    private final Random mRandom = new Random();

    public AvalancheIngestionRetryer(AvalancheIngestion decoratedApi) {
        this(decoratedApi, RETRY_INTERVALS);
    }

    protected AvalancheIngestionRetryer(AvalancheIngestion decoratedApi, long... retryIntervals) {
        super(decoratedApi);
        mRetryIntervals = retryIntervals;
    }

    @Override
    public ServiceCall sendAsync(String appId, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {

        /* Wrap the call with the retry logic and call delegate. */
        RetryableCall retryableCall = new RetryableCall(appId, installId, logContainer, serviceCallback);
        retryableCall.run();
        return retryableCall;
    }

    /**
     * Retry wrapper logic.
     */
    private class RetryableCall implements Runnable, ServiceCall, ServiceCallback {

        /**
         * Wrapped parameter.
         */
        private final String mAppId;

        /**
         * Wrapped parameter.
         */
        private final UUID mInstallId;

        /**
         * Wrapped parameter.
         */
        private final LogContainer mLogContainer;

        /**
         * Wrapped parameter.
         */
        private final ServiceCallback mServiceCallback;

        /**
         * Delegate call to be able to cancel the underlying request.
         */
        private ServiceCall mDecoratedServiceCall;

        /**
         * Current retry counter. 0 means its the first try.
         */
        private int mRetryCount;

        RetryableCall(String appId, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            mAppId = appId;
            mInstallId = installId;
            mLogContainer = logContainer;
            mServiceCallback = serviceCallback;
        }

        @Override
        public synchronized void run() {
            mDecoratedServiceCall = mDecoratedApi.sendAsync(mAppId, mInstallId, mLogContainer, this);
        }

        @Override
        public synchronized void cancel() {
            mHandler.removeCallbacks(this);
            mDecoratedServiceCall.cancel();
        }

        @Override
        public void success() {
            mServiceCallback.success();
        }

        @Override
        public void failure(Throwable t) {
            if (mRetryCount < mRetryIntervals.length && HttpUtils.isRecoverableError(t)) {
                long delay = mRetryIntervals[mRetryCount++] / 2;
                delay += mRandom.nextInt((int) delay);
                AvalancheLog.warn("Try #" + mRetryCount + " failed and will be retried in " + delay + " ms", t);
                mHandler.postDelayed(this, delay);
            } else
                mServiceCallback.failure(t);
        }
    }
}
