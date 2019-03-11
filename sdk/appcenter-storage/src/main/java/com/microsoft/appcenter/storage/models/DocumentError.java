// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.appcenter.storage.models;

public class DocumentError {

    private Throwable exception;

    @SuppressWarnings("WeakerAccess")
    public DocumentError(Throwable exception) {
        this.exception = exception;
    }

    public Throwable getError() {
        return exception;
    }
}
