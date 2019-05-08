/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

/**
 * Pending operation.
 */
public class PendingOperation {

    private final String mTable;

    private String mOperation;

    private final String mPartition;

    private final String mDocumentId;

    private final String mDocument;

    private String mETag;

    private final long mExpirationTime;

    private final long mDownloadTime;

    private final long mOperationTime;

    public PendingOperation(String table, String operation, String partition, String documentId, String document, long expirationTime, long downloadTime, long operationTime) {
        mTable = table;
        mOperation = operation;
        mPartition = partition;
        mDocumentId = documentId;
        mDocument = document;
        mExpirationTime = expirationTime;
        mDownloadTime = downloadTime;
        mOperationTime = operationTime;
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
     * @param operation name.
     */
    public void setOperation(String operation) {
        mOperation = operation;
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

    /**
     * @return document download time.
     */
    public long getDownloadTime() {
        return mDownloadTime;
    }

    /**
     * @return operation time.
     */
    public long getOperationTime() {
        return mOperationTime;
    }
}
