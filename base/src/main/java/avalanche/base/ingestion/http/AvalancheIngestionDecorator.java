package avalanche.base.ingestion.http;

import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCall;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.models.LogContainer;

public abstract class AvalancheIngestionDecorator implements AvalancheIngestion {

    protected final AvalancheIngestion mDecoratedApi;

    public AvalancheIngestionDecorator(AvalancheIngestion decoratedApi) {
        this.mDecoratedApi = decoratedApi;
    }

    @Override
    public ServiceCall sendAsync(UUID appKey, UUID installId, LogContainer logContainer, ServiceCallback serviceCallback) throws IllegalArgumentException {
        return mDecoratedApi.sendAsync(appKey, installId, logContainer, serviceCallback);
    }
}
