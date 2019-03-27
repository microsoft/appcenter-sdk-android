/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

/**
 * Details of the remote operation execution failures.
 */
public class DocumentError {

    private Throwable exception;

    /**
     * @param exception that occurred during the network state change.
     */
    @SuppressWarnings("WeakerAccess")
    public DocumentError(Throwable exception) {
        this.exception = exception;
    }

    /**
     * @return underlying exception.
     */
    public Throwable getError() {
        return exception;
    }
}
