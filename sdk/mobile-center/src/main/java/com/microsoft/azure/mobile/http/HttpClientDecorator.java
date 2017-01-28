package com.microsoft.azure.mobile.http;

import java.io.IOException;

abstract class HttpClientDecorator implements HttpClient {

    final HttpClient mDecoratedApi;

    HttpClientDecorator(HttpClient decoratedApi) {
        mDecoratedApi = decoratedApi;
    }

    @Override
    public void close() throws IOException {
        mDecoratedApi.close();
    }
}
