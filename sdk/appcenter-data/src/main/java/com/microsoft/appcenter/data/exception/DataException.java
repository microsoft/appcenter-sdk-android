/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.exception;

public class DataException extends Exception {

    public DataException(String message, Exception exception) {
        super(message, exception);
    }

    public DataException(String message) {
        super(message);
    }

    public DataException(Exception exception) {
        super(exception);
    }
}
