/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.exception;

/**
 * Details of the remote operation execution failures.
 */
public class DocumentError extends Exception {

    /**
     * @param exception that occurred during the network state change.
     */
    public DocumentError(Throwable exception) {
        super(exception);
    }

}