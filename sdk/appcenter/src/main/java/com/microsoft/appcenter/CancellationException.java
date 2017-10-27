package com.microsoft.appcenter;

/**
 * Thrown when {@link AppCenter} received a cancellation sending request to the server.
 */
public class CancellationException extends Exception {

    public CancellationException() {
        super("Request cancelled because Channel is disabled.");
    }
}
