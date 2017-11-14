package com.microsoft.appcenter.http;

/**
 * The callback used for client side asynchronous operations.
 */
public interface ServiceCallback {

    /**
     * Implement this method to handle successful REST call results.
     *
     * @param payload HTTP payload.
     */
    void onCallSucceeded(String payload);

    /**
     * Implement this method to handle REST call failures.
     *
     * @param e the exception thrown from the pipeline.
     */
    void onCallFailed(Exception e);
}