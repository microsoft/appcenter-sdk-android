/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

/**
 * Pending operation.
 */
public class PendingOperation {

    private String mTable;

    private String mOperation;

    private String mPartition;

    private String mDocumentId;

    private String mDocument;

    private String mETag;

    private long mExpirationTime;

    public PendingOperation(String table, String operation, String partition, String documentId, String document, long expirationTime) {
        mTable = table;
        mOperation = operation;
        mPartition = partition;
        mDocumentId = documentId;
        mDocument = document;
        mExpirationTime = expirationTime;
    }

    /**
     * @return table name the operation is performed on
     */
    public String getTable() {
        return mTable;
    }

    /**
     * @return operation name.
     */
    public String getOperation() {
        return mOperation;
    }

    /**
     * @return partition name.
     */
    public String getPartition() {
        return mPartition;
    }

    /**
     * @return document ID.
     */
    public String getDocumentId() {
        return mDocumentId;
    }

    /**
     * @return document object.
     */
    public String getDocument() {
        return mDocument;
    }

    /**
     * @param document object.
     */
    public void setDocument(String document) {
        mDocument = document;
    }

    /**
     * @return Cosmos DB eTag.
     */
    public String getETag() {
        return mETag;
    }

    /**
     * @param eTag setter.
     */
    public void setETag(String eTag) {
        mETag = eTag;
    }

    /**
     * @return document expiration time.
     */
    public long getExpirationTime() {
        return mExpirationTime;
    }
}
