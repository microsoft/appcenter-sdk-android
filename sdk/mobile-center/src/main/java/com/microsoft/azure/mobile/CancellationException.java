package com.microsoft.azure.mobile;

/**
 * Thrown when {@link MobileCenter} received a cancellation sending request to the server.
 */
public class CancellationException extends Exception {

    @SuppressWarnings("SameParameterValue")
    public CancellationException(String detailMessage) {
        super(detailMessage);
    }
}
