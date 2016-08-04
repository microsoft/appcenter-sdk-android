package avalanche.core.ingestion.http;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCall;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.utils.NetworkStateHelper;

/**
 * Decorator pausing calls while network is down.
 */
public class AvalancheIngestionNetworkStateHandler extends AvalancheIngestionDecorator implements NetworkStateHelper.Listener {

    /**
     * Network state helper.
     */
    private final NetworkStateHelper mNetworkStateHelper;

    /**
     * All pending calls.
     */
    private final Set<Call> mCalls = new HashSet<>();

    /**
     * Init.
     *
     * @param decoratedApi       decorated API.
     * @param networkStateHelper network state helper.
     */
    public AvalancheIngestionNetworkStateHandler(AvalancheIngestion decoratedApi, NetworkStateHelper networkStateHelper) {
        super(decoratedApi);
        mNetworkStateHelper = networkStateHelper;
        mNetworkStateHelper.addListener(this);
    }

    @Override
    public ServiceCall sendAsync(UUID appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
        Call ingestionCall = new Call(mDecoratedApi, appSecret, installId, logContainer, serviceCallback);
        synchronized (mCalls) {
            mCalls.add(ingestionCall);
            if (mNetworkStateHelper.isNetworkConnected())
                ingestionCall.run();
        }
        return ingestionCall;
    }

    @Override
    public void close() throws IOException {
        mNetworkStateHelper.removeListener(this);
        synchronized (mCalls) {
            for (Call call : mCalls)
                call.pauseCall();
            mCalls.clear();
        }
        super.close();
    }

    @Override
    public void onNetworkStateUpdated(boolean connected) {
        synchronized (mCalls) {
            for (Call call : mCalls)
                if (connected)
                    call.run();
                else
                    call.pauseCall();
        }
    }

    /**
     * Call wrapper logic.
     */
    private class Call extends AvalancheIngestionCallDecorator implements Runnable, ServiceCallback {

        Call(AvalancheIngestion decoratedApi, UUID appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            super(decoratedApi, appSecret, installId, logContainer, serviceCallback);
        }

        @Override
        public void run() {
            synchronized (mCalls) {
                mServiceCall = mDecoratedApi.sendAsync(mAppSecret, mInstallId, mLogContainer, this);
            }
        }

        @Override
        public void cancel() {
            synchronized (mCalls) {
                mCalls.remove(this);
                pauseCall();
            }
        }

        public void pauseCall() {
            synchronized (mCalls) {
                if (mServiceCall != null)
                    mServiceCall.cancel();
            }
        }

        @Override
        public void success() {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            synchronized (mCalls) {
                if (mCalls.contains(this)) {
                    super.success();
                    mCalls.remove(this);
                }
            }
        }

        @Override
        public void failure(Throwable t) {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            synchronized (mCalls) {
                if (mCalls.contains(this)) {
                    mServiceCallback.failure(t);
                    mCalls.remove(this);
                }
            }
        }
    }
}
