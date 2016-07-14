package avalanche.core.ingestion.http;

import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCall;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.models.LogContainer;

/**
 * Helper class used to share logic with multiple decorators.
 */
abstract class AvalancheIngestionCallDecorator implements Runnable, ServiceCall, ServiceCallback {

    /**
     * Decorated API.
     */
    final AvalancheIngestion mDecoratedApi;

    /**
     * Application identifier.
     */
    final UUID mAppKey;

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

    public AvalancheIngestionCallDecorator(AvalancheIngestion decoratedApi, UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) {
        mDecoratedApi = decoratedApi;
        mAppKey = appKey;
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
        mServiceCall = mDecoratedApi.sendAsync(mAppKey, mInstallId, mLogContainer, this);
    }

    @Override
    public void success() {
        mServiceCallback.success();
    }
}
