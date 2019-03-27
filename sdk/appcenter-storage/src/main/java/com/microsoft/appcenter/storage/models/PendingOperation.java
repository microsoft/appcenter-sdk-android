/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

/**
 * Pending operation
 */
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

    /**
     * @return operation name.
     */
    public String getOperation() {
        return operation;
    }

    /**
     * @return partition name.
     */
    public String getPartition() {
        return partition;
    }

    /**
     * @return document ID
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * @return document object
     */
    public String getDocument() {
        return document;
    }

    /**
     * @return Cosmos DB etag
     */
    public String getEtag() {
        return etag;
    }

    /**
     * @param etag setter
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }

    /**
     * @return document expiration time
     */
    public long getExpirationTime() {
        return expirationTime;
    }
}
