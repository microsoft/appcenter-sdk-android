/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.TimeToLive;

/**
 * Java object representation of a row in Local Document storage in a SqLite table.
 */
public class LocalDocument {

    private final String mTable;

    private String mOperation;

    private final String mPartition;

    private final String mDocumentId;

    private final String mDocument;

    private String mETag;

    /**
     * Expiration time in milliseconds.
     */
    private final long mExpirationTime;

    /**
     * Download time in milliseconds.
     */
    private final long mDownloadTime;

    /**
     * Operation time in milliseconds.
     */
    private final long mOperationTime;

    public LocalDocument(String table, String operation, String partition, String documentId, String document, long expirationTime, long downloadTime, long operationTime) {
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

    /**
     * @return whether the document is expired
     */
    public boolean isExpired() {
        if (mExpirationTime == TimeToLive.INFINITE) {
            return false;
        }
        return mExpirationTime <= System.currentTimeMillis();
    }

    /**
     * @return whether the document is a pending operation or has been synchronized with the DB
     */
    public boolean hasPendingOperation() {
        return mOperation != null;
    }
}
