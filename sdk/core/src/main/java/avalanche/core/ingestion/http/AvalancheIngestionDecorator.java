package avalanche.core.ingestion.http;

import java.io.IOException;

import avalanche.core.ingestion.AvalancheIngestion;

abstract class AvalancheIngestionDecorator implements AvalancheIngestion {

    final AvalancheIngestion mDecoratedApi;

    AvalancheIngestionDecorator(AvalancheIngestion decoratedApi) {
        this.mDecoratedApi = decoratedApi;
    }

    @Override
    public void close() throws IOException {
        mDecoratedApi.close();
    }
}
