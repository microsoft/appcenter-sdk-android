package com.microsoft.appcenter.storage.models;

public class PendingOperation {
    private String operation;
    
    private String partition;
    private String documentId;
    private String document;
    private String etag;
    private long expirationTime;

    public PendingOperation(String operation, String partition, String documentId, String document, long expirationTime) {
        this.operation = operation;
        this.partition = partition;
        this.documentId = documentId;
        this.document = document;
        this.expirationTime = expirationTime;
    }

    public String getOperation() {
        return operation;
    }

    public String getPartition() {
        return partition;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getDocument() {
        return document;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
