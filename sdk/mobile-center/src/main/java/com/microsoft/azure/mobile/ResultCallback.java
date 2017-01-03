package com.microsoft.azure.mobile;

/**
 * Callback interface for general purpose.
 */
public interface ResultCallback<T> {

    /**
     * Called when the request completed.
     *
     * @param data The data for the result.
     */
    void onResult(T data);
}
