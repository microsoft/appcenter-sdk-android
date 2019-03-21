package com.microsoft.appcenter.storage.client;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientDecorator;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;

import java.util.Map;

public class StorageHttpClientDecorator extends HttpClientDecorator {

    private boolean mOfflineMode;

    public StorageHttpClientDecorator(HttpClient decoratedApi) {
        super(decoratedApi);
    }

    /**
     * Make an HTTP call.
     *
     * @param url             URL.
     * @param method          GET or POST.
     * @param headers         headers, can be empty.
     * @param callTemplate    callbacks to provide request body or get notification before calling.
     * @param serviceCallback callbacks to monitor the completion of the HTTP call.
     * @return a call handle to later cancel the call.
     */
    @Override
    public ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
        if (mOfflineMode) {
            serviceCallback.onCallFailed(new HttpException(-1, "Storage offline simulation mode is enabled"));
        } else {
            mDecoratedApi.callAsync(url, method, headers, callTemplate, serviceCallback);
        }

        return null;
    }

    /**
     * @return offline mode state
     */
    public boolean isOfflineMode() {
        return mOfflineMode;
    }

    /**
     * @param offlineMode sets offline mode state
     */
    public void setOfflineMode(boolean offlineMode) {
        this.mOfflineMode = offlineMode;
    }
}
