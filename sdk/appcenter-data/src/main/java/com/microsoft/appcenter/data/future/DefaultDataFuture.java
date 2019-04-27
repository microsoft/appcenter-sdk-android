package com.microsoft.appcenter.data.future;

import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

public abstract class DefaultDataFuture<T> extends DefaultAppCenterFuture<T> {

    public synchronized void completeWithException(Exception e) {
        complete(createExceptionInstance(e));
    }

    protected abstract T createExceptionInstance(Exception e);
}
