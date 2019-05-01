/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.async;

/**
 * Tiny subset of CompletableFuture usable on Java 7.
 * Does not throw any exception.
 */
public interface AppCenterFuture<T> {

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result.
     */
    T get();

    /**
     * Execute the consumer once the computation is completed with the result.
     * The consumer function is called in the U.I. thread.
     *
     * @param function the action to perform upon completion.
     */
    void thenAccept(AppCenterConsumer<T> function);

    /**
     * Returns true if completed.
     *
     * @return true if completed, false otherwise.
     */
    @SuppressWarnings("unused")
    boolean isDone();
}
