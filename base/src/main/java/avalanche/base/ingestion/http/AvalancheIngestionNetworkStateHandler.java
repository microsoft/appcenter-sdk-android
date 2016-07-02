package avalanche.base.ingestion.http;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.utils.NetworkStateHelper;

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
    public ServiceCall sendAsync(UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
        Call ingestionCall = new Call(mDecoratedApi, appKey, installId, logContainer, serviceCallback);
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

        Call(AvalancheIngestion decoratedApi, UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
            super(decoratedApi, appKey, installId, logContainer, serviceCallback);
        }

        @Override
        public synchronized void run() {
            mServiceCall = mDecoratedApi.sendAsync(mAppKey, mInstallId, mLogContainer, this);
        }

        @Override
        public synchronized void cancel() {
            mCalls.remove(this);
            super.cancel();
        }

        public void pauseCall() {
            mServiceCall.cancel();
        }

        @Override
        public synchronized void success() {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            if (mCalls.contains(this)) {
                super.success();
                mCalls.remove(this);
            }
        }

        @Override
        public synchronized void failure(Throwable t) {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            if (mCalls.contains(this)) {

                /*
                 * Hide transient errors if the network is now down,
                 * the call may have failed because of the network change before we could cancel.
                 * We'll retry the call when network is up again.
                 */
                if (!HttpUtils.isRecoverableError(t) || mNetworkStateHelper.isNetworkConnected()) {
                    mServiceCallback.failure(t);
                    mCalls.remove(this);
                }
            }
        }
    }
}
