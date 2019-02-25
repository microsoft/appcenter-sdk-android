package com.microsoft.appcenter.storage.models;

public class DocumentError {
    public DocumentError(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    private Exception exception;

}
