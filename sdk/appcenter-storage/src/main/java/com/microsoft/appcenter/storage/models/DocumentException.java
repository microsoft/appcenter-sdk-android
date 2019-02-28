package com.microsoft.appcenter.storage.models;

public class DocumentException {
    public DocumentException(Exception exception) {
        this.exception = exception;
    }

    public Exception getException() {
        return exception;
    }

    private Exception exception;

}
