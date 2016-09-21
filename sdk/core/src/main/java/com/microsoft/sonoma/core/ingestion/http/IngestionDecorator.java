package com.microsoft.sonoma.core.ingestion.http;

import com.microsoft.sonoma.core.ingestion.Ingestion;

import java.io.IOException;

abstract class IngestionDecorator implements Ingestion {

    final Ingestion mDecoratedApi;

    IngestionDecorator(Ingestion decoratedApi) {
        mDecoratedApi = decoratedApi;
    }

    @Override
    public void setServerUrl(String serverUrl) {
        mDecoratedApi.setServerUrl(serverUrl);
    }

    @Override
    public void close() throws IOException {
        mDecoratedApi.close();
    }
}
