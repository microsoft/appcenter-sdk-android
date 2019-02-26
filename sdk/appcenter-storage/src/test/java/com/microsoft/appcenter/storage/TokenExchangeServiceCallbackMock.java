package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;

public class TokenExchangeServiceCallbackMock extends TokenExchange.TokenExchangeServiceCallback {

    private TokenResult _tokenResult;

    @Override
    public void completeFuture(Exception e) {

    }

    @Override
    public void callCosmosDb(TokenResult tokenResult) {
        this._tokenResult = tokenResult;
    }

    public TokenResult getTokenResult() {
        return _tokenResult;
    }
}
