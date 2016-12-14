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
    public synchronized ServiceCall sendAsync(String appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
        Call ingestionCall = new Call(mDecoratedApi, appSecret, installId, logContainer, serviceCallback);
        mCalls.add(ingestionCall);
        if (mNetworkStateHelper.isNetworkConnected())
            ingestionCall.run();
        return ingestionCall;
    }

    @Override
    public synchronized void close() throws IOException {
        mNetworkStateHelper.removeListener(this);
        for (Call call : mCalls)
            pauseCall(call);
        mCalls.clear();
        super.close();
    }

    @Override
    public synchronized void onNetworkStateUpdated(boolean connected) {
        for (Call call : mCalls)
            if (connected)
                call.run();
            else
                pauseCall(call);
    }

    private synchronized void callRunAsync(Call call) {
        call.mServiceCall = call.mDecoratedApi.sendAsync(call.mAppSecret, call.mInstallId, call.mLogContainer, call);
    }

    private synchronized void cancelCall(Call call) {
        mCalls.remove(call);
        pauseCall(call);
    }

    private synchronized void pauseCall(Call call) {
        if (call.mServiceCall != null)
            call.mServiceCall.cancel();
    }

    /**
     * Guard against multiple calls since this call can be retried on network state change.
     */
    private synchronized void onCallSucceeded(Call call) {
        if (mCalls.contains(call)) {
            call.mServiceCallback.onCallSucceeded();
            mCalls.remove(call);
        }
    }

    /**
     * Guard against multiple calls since this call can be retried on network state change.
     */
    private synchronized void onCallFailed(Call call, Exception e) {
        if (mCalls.contains(call)) {
            call.mServiceCallback.onCallFailed(e);
            mCalls.remove(call);
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
            callRunAsync(this);
        }

        @Override
        public void cancel() {
            cancelCall(this);
        }

        @Override
        public void onCallSucceeded() {
            IngestionNetworkStateHandler.this.onCallSucceeded(this);
        }

        @Override
        public void onCallFailed(Exception e) {
            IngestionNetworkStateHandler.this.onCallFailed(this, e);
        }
    }
}
