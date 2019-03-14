/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.model;

public class NativeException extends RuntimeException {

    private static final String CRASH_MESSAGE = "Native exception read from a minidump file";

    public NativeException() {
        super(CRASH_MESSAGE);
    }
}
