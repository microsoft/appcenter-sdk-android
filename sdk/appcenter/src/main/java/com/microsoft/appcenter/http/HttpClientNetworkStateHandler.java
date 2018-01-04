package com.microsoft.appcenter.http;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Decorator pausing calls while network is down.
 */
public class HttpClientNetworkStateHandler extends HttpClientDecorator implements NetworkStateHelper.Listener {

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
    public HttpClientNetworkStateHandler(HttpClient decoratedApi, NetworkStateHelper networkStateHelper) {
        super(decoratedApi);
        mNetworkStateHelper = networkStateHelper;
        mNetworkStateHelper.addListener(this);
    }

    @Override
    public synchronized ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
        Call call = new Call(mDecoratedApi, url, method, headers, callTemplate, serviceCallback);
        mCalls.add(call);
        if (mNetworkStateHelper.isNetworkConnected()) {
            call.run();
        } else {
            AppCenterLog.debug(LOG_TAG, "Call triggered with no network connectivity, waiting network to become available...");
        }
        return call;
    }

    @Override
    public synchronized void close() throws IOException {
        mNetworkStateHelper.removeListener(this);
        for (Call call : mCalls) {
            pauseCall(call);
        }
        mCalls.clear();
        super.close();
    }

    @Override
    public void reopen() {
        mNetworkStateHelper.addListener(this);
        super.reopen();
    }

    @Override
    public synchronized void onNetworkStateUpdated(boolean connected) {
        if (connected) {
            AppCenterLog.debug(LOG_TAG, "Network is available. " + mCalls.size() + " pending call(s) to submit now.");
        } else {
            AppCenterLog.debug(LOG_TAG, "Network is down. Pausing " + mCalls.size() + " network call(s).");
        }
        for (Call call : mCalls) {
            if (connected) {
                call.run();
            } else {
                pauseCall(call);
            }
        }
    }

    private synchronized void callRunAsync(Call call) {
        call.mServiceCall = call.mDecoratedApi.callAsync(call.mUrl, call.mMethod, call.mHeaders, call.mCallTemplate, call);
    }

    private synchronized void cancelCall(Call call) {
        mCalls.remove(call);
        pauseCall(call);
    }

    private synchronized void pauseCall(Call call) {
        if (call.mServiceCall != null) {
            call.mServiceCall.cancel();
        }
    }

    /**
     * Guard against multiple calls since this call can be retried on network state change.
     */
    private synchronized void onCallSucceeded(Call call, String payload) {
        if (mCalls.contains(call)) {
            call.mServiceCallback.onCallSucceeded(payload);
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
    private class Call extends HttpClientCallDecorator implements Runnable, ServiceCallback {

        Call(HttpClient decoratedApi, String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
            super(decoratedApi, url, method, headers, callTemplate, serviceCallback);
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
        public void onCallSucceeded(String payload) {
            HttpClientNetworkStateHandler.this.onCallSucceeded(this, payload);
        }

        @Override
        public void onCallFailed(Exception e) {
            HttpClientNetworkStateHandler.this.onCallFailed(this, e);
        }
    }
}
