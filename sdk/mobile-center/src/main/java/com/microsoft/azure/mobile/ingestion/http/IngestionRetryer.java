package com.microsoft.azure.mobile.ingestion.http;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.net.UnknownHostException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Decorator managing retries.
 */
public class IngestionRetryer extends IngestionDecorator {

    /**
     * Retry intervals to use, array index is to use the value for each retry. When we used all the array values, we give up and forward the last error.
     */
    @VisibleForTesting
    static final long[] RETRY_INTERVALS = new long[]{
            TimeUnit.SECONDS.toMillis(10),
            TimeUnit.MINUTES.toMillis(5),
            TimeUnit.MINUTES.toMillis(20)
    };

    /**
     * Android "timer" using the main thread loop.
     */
    private final Handler mHandler;

    /**
     * Random object for interval randomness.
     */
    private final Random mRandom = new Random();

    /**
     * Init with default retry policy.
     *
     * @param decoratedApi API to decorate.
     */
    public IngestionRetryer(Ingestion decoratedApi) {
        this(decoratedApi, new Handler(Looper.getMainLooper()));
    }

    /**
     * Init.
     *
     * @param decoratedApi API to decorate.
     * @param handler      handler for timed retries.
     */
    @VisibleForTesting
    IngestionRetryer(Ingestion decoratedApi, Handler handler) {
        super(decoratedApi);
        mHandler = handler;
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {

        /* Wrap the call with the retry logic and call delegate. */
        RetryableCall retryableCall = new RetryableCall(mDecoratedApi, appSecret, installId, logContainer, serviceCallback);
        retryableCall.run();
        return retryableCall;
    }

    /**
     * Retry wrapper logic.
     */
    private class RetryableCall extends IngestionCallDecorator {

        /**
         * Current retry counter. 0 means its the first try.
         */
        private int mRetryCount;

        RetryableCall(Ingestion decoratedApi, String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            super(decoratedApi, appSecret, installId, logContainer, serviceCallback);
        }

        @Override
        public synchronized void cancel() {
            mHandler.removeCallbacks(this);
            super.cancel();
        }

        @Override
        public void onCallFailed(Exception e) {
            if (mRetryCount < RETRY_INTERVALS.length && HttpUtils.isRecoverableError(e)) {
                long delay = RETRY_INTERVALS[mRetryCount++] / 2;
                delay += mRandom.nextInt((int) delay);
                String message = "Try #" + mRetryCount + " failed and will be retried in " + delay + " ms";
                if (e instanceof UnknownHostException)
                    message += " (UnknownHostException)";
                MobileCenterLog.warn(MobileCenter.LOG_TAG, message, e);
                mHandler.postDelayed(this, delay);
            } else
                mServiceCallback.onCallFailed(e);
        }
    }
}
