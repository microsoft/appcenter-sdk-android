package com.microsoft.azure.mobile;

/**
 * Thrown when {@link MobileCenter} received a cancellation sending request to the server.
 */
public class CancellationException extends Exception {

    public CancellationException() {
        this("Request cancelled because Channel is disabled.");
    }

    @SuppressWarnings("SameParameterValue")
    public CancellationException(String detailMessage) {
        super(detailMessage);
    }
}
