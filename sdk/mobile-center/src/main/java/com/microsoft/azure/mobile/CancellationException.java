package com.microsoft.azure.mobile;

/**
 * Thrown when {@link MobileCenter} received a cancellation sending request to the server.
 */
public class CancellationException extends Exception {

    public CancellationException() {
        super("Request cancelled because Channel is disabled.");
    }
}
