/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
        if (mNetworkStateHelper.isNetworkConnected()) {
            call.run();
        } else {
            mCalls.add(call);
            AppCenterLog.debug(LOG_TAG, "Call triggered with no network connectivity, waiting network to become available...");
        }
        return call;
    }

    @Override
    public synchronized void close() throws IOException {
        mNetworkStateHelper.removeListener(this);
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
        if (connected && mCalls.size() > 0) {
            AppCenterLog.debug(LOG_TAG, "Network is available. " + mCalls.size() + " pending call(s) to submit now.");
            for (Call call : mCalls) {
                call.run();
            }
            mCalls.clear();
        }

        /*
         * Loss of network may not lead to call failure. Also, cancelling the ongoing requests
         * can cause requests duplication, so let it fail and then retry when we're online again.
         */
    }

    private synchronized void cancelCall(Call call) {
        if (call.mServiceCall != null) {
            call.mServiceCall.cancel();
        }
        mCalls.remove(call);
    }

    /**
     * Call wrapper logic.
     */
    private class Call extends HttpClientCallDecorator {

        Call(HttpClient decoratedApi, String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
            super(decoratedApi, url, method, headers, callTemplate, serviceCallback);
        }

        @Override
        public void cancel() {
            cancelCall(this);
        }
    }
}
