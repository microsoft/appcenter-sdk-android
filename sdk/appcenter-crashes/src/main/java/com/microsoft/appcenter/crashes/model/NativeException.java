package com.microsoft.appcenter.crashes.model;

public class NativeException extends RuntimeException {

    private static final String CRASH_MESSAGE = "Native exception captured by Breakpad";

    public NativeException() {
        super(CRASH_MESSAGE);
    }
}
