package com.microsoft.azure.mobile.utils.async;

public interface SimpleFuture<T> {

    T get();

    SimpleFuture<T> thenApply(SimpleFunction<T> function);

    boolean isDone();
}
