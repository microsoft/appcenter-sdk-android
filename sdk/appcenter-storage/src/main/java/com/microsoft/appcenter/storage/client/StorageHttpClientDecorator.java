package com.microsoft.appcenter.storage.client;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientDecorator;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;

import java.util.Map;

public class StorageHttpClientDecorator extends HttpClientDecorator {

    private boolean mSimulateOffline;

    public StorageHttpClientDecorator(HttpClient decoratedApi) {
        super(decoratedApi);
    }

    @Override
    public ServiceCall callAsync(String url, String method, Map<String, String> headers, CallTemplate callTemplate, ServiceCallback serviceCallback) {
        if (mSimulateOffline) {
            serviceCallback.onCallFailed(new HttpException(-1, "Storage offline simulation mode is enabled"));
        } else {
            mDecoratedApi.callAsync(url, method, headers, callTemplate, serviceCallback);
        }

        return null;
    }

    public boolean isSimulateOffline() {
        return mSimulateOffline;
    }

    public void setSimulateOffline(boolean simulateOffline) {
        this.mSimulateOffline = simulateOffline;
    }
}
