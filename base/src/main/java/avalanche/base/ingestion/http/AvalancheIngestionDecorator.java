package avalanche.base.ingestion.http;

import java.io.IOException;

import avalanche.base.ingestion.AvalancheIngestion;

public abstract class AvalancheIngestionDecorator implements AvalancheIngestion {

    protected final AvalancheIngestion mDecoratedApi;

    public AvalancheIngestionDecorator(AvalancheIngestion decoratedApi) {
        this.mDecoratedApi = decoratedApi;
    }

    @Override
    public void close() throws IOException {
        mDecoratedApi.close();
    }
}
