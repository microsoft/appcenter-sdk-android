/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.exception.StorageException;

/**
 * A document coming back from CosmosDB.
 */
public class Document<T> {

    @SerializedName(value = Constants.PARTITION_KEY_FIELD_NAME)
    private String mPartition;

    @SerializedName(value = Constants.ID_FIELD_NAME)
    private String mId;

    @SerializedName(value = Constants.ETAG_FIELD_NAME)
    private String mETag;

    @SerializedName(value = Constants.TIMESTAMP_FIELD_NAME)
    private long mTimestamp;

    @SerializedName(value = Constants.DOCUMENT_FIELD_NAME)
    private T mDocument;

    private transient DocumentError mDocumentError;

    private transient boolean mFromCache;

    private transient String mPendingOperation;

    public Document() {
    }

    public Document(T document, String partition, String id) {
        mPartition = partition;
        mId = id;
        mDocument = document;
    }

    public Document(T document, String partition, String id, String eTag, long timestamp) {
        this(document, partition, id);
        mETag = eTag;
        mTimestamp = timestamp;
        mDocument = document;
    }

    public Document(Throwable exception) {
        mDocumentError = new DocumentError(exception);
    }

    public Document(String message, Throwable exception) {
        mDocumentError = new DocumentError(new StorageException(message, exception));
    }

    /**
     * Get document.
     *
     * @return Deserialized document.
     */
    public T getDocument() {
        return mDocument;
    }

    /**
     * Get document error.
     *
     * @return Document error.
     */
    public DocumentError getDocumentError() {
        return mDocumentError;
    }

    /**
     * Get document partition.
     *
     * @return Document partition.
     */
    public String getPartition() {
        return mPartition;
    }

    /**
     * Get document id.
     *
     * @return Document id.
     */
    public String getId() {
        return mId;
    }

    /**
     * Get ETag.
     *
     * @return ETag.
     */
    public String getETag() {
        return mETag;
    }

    /**
     * Get document generated in UTC unix epoch.
     *
     * @return UTC unix timestamp.
     */
    public long getTimestamp() {
        return mTimestamp;
    }

    /**
     * Get the document in string.
     *
     * @return Serialized document.
     */
    @NonNull
    @Override
    public String toString() {
        return Utils.getGson().toJson(this);
    }

    /**
     * Get the flag indicating if data was retrieved from the local cache (for offline mode)
     */
    public boolean isFromCache() {
        return mFromCache;
    }

    /**
     * Set the flag indicating if data was retrieved from the local cache (for offline mode)
     */
    public void setFromCache(boolean fromCache) {
        mFromCache = fromCache;
    }

    /**
     * Get the pending operation value.
     */
    public String getPendingOperation() {
        return mPendingOperation;
    }

    /**
     * Set the pending operation value.
     *
     * @param pendingOperation The pending operation saved in the local storage.
     */
    public void setPendingOperation(String pendingOperation) {
        mPendingOperation = pendingOperation;
    }

    /**
     * @return whether the document has an error associated with it.
     */
    public boolean hasFailed() {
        return getDocumentError() != null;
    }
}
