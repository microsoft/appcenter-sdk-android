package com.microsoft.appcenter.http;

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

    @Override
    public void reopen() {
        mDecoratedApi.reopen();
    }
}
