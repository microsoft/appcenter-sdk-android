package com.microsoft.appcenter.data.future;

import com.microsoft.appcenter.data.models.DocumentWrapper;

public class DocumentWrapperFuture<T> extends DefaultDataFuture<DocumentWrapper<T>> {

    @Override
    protected DocumentWrapper createExceptionInstance(Exception e) {
        return new DocumentWrapper<>(e);
    }
}
