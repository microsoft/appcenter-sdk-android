package com.microsoft.azure.mobile.ingestion.http;

import com.microsoft.azure.mobile.ingestion.Ingestion;
import com.microsoft.azure.mobile.ingestion.ServiceCall;
import com.microsoft.azure.mobile.ingestion.ServiceCallback;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;

import java.util.UUID;

/**
 * Helper class used to share logic with multiple decorators.
 */
abstract class IngestionCallDecorator implements Runnable, ServiceCall, ServiceCallback {

    /**
     * Decorated API.
     */
    final Ingestion mDecoratedApi;

    /**
     * Application secret.
     */
    final UUID mAppSecret;

    /**
     * Installation identifier.
     */
    final UUID mInstallId;

    /**
     * Log container.
     */
    final LogContainer mLogContainer;

    /**
     * Callback.
     */
    final ServiceCallback mServiceCallback;

    /**
     * Call.
     */
    ServiceCall mServiceCall;

    IngestionCallDecorator(Ingestion decoratedApi, UUID appSecret, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
        mDecoratedApi = decoratedApi;
        mAppSecret = appSecret;
        mInstallId = installId;
        mLogContainer = logContainer;
        mServiceCallback = serviceCallback;
    }

    @Override
    public synchronized void cancel() {
        mServiceCall.cancel();
    }

    @Override
    public synchronized void run() {
        mServiceCall = mDecoratedApi.sendAsync(mAppSecret, mInstallId, mLogContainer, this);
    }

    @Override
    public void onCallSucceeded() {
        mServiceCallback.onCallSucceeded();
    }
}
