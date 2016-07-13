package avalanche.base.ingestion.http;

import java.io.IOException;

import avalanche.base.ingestion.AvalancheIngestion;

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
