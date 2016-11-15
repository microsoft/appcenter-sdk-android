package com.microsoft.azure.mobile.ingestion.http;

import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Decorator pausing calls while network is down.
 */
public class IngestionNetworkStateHandler extends IngestionDecorator implements NetworkStateHelper.Listener {

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
    public IngestionNetworkStateHandler(Ingestion decoratedApi, NetworkStateHelper networkStateHelper) {
        super(decoratedApi);
        mNetworkStateHelper = networkStateHelper;
        mNetworkStateHelper.addListener(this);
    }

    @Override
    public ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
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
    private class Call extends IngestionCallDecorator implements Runnable, ServiceCallback {

        Call(Ingestion decoratedApi, String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
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

        void pauseCall() {
            synchronized (mCalls) {
                if (mServiceCall != null)
                    mServiceCall.cancel();
            }
        }

        @Override
        public void onCallSucceeded() {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            synchronized (mCalls) {
                if (mCalls.contains(this)) {
                    super.onCallSucceeded();
                    mCalls.remove(this);
                }
            }
        }

        @Override
        public void onCallFailed(Exception e) {

            /**
             * Guard against multiple calls since this call can be retried on network state change.
             */
            synchronized (mCalls) {
                if (mCalls.contains(this)) {
                    mServiceCallback.onCallFailed(e);
                    mCalls.remove(this);
                }
            }
        }
    }
}
