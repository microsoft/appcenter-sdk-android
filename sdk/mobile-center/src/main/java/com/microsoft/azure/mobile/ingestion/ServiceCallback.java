/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.ingestion;

/**
 * The callback used for client side asynchronous operations.
 */
public interface ServiceCallback {

    /**
     * Implement this method to handle successful REST call results.
     */
    void onCallSucceeded();

    /**
     * Implement this method to handle REST call failures.
     *
     * @param e the exception thrown from the pipeline.
     */
    void onCallFailed(Exception e);
}