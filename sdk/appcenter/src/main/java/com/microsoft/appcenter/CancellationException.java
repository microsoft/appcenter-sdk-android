/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

/**
 * Thrown when {@link AppCenter} received a cancellation sending request to the server.
 */
@SuppressWarnings("JavadocReference")
public class CancellationException extends Exception {

    public CancellationException() {
        super("Request cancelled because Channel is disabled.");
    }
}
