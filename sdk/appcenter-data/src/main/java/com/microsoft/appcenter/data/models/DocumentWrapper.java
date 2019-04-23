/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.data.Constants;
import com.microsoft.appcenter.data.Utils;
import com.microsoft.appcenter.data.exception.StorageException;

/**
 * A document coming back from CosmosDB.
 */
public class DocumentWrapper<T> {

    @SerializedName(value = Constants.PARTITION_KEY_FIELD_NAME)
    private String mPartition;

    @SerializedName(value = Constants.ID_FIELD_NAME)
    private String mId;

    @SerializedName(value = Constants.ETAG_FIELD_NAME)
    private String mETag;

    @SerializedName(value = Constants.TIMESTAMP_FIELD_NAME)
    private long mLastUpdatedDate;

    @SerializedName(value = Constants.DOCUMENT_FIELD_NAME)
    private T mDocument;

    private transient StorageException mError;

    private transient boolean mFromCache;

    private transient String mPendingOperation;

    public DocumentWrapper() {
    }

    public DocumentWrapper(T document, String partition, String id) {
        mPartition = partition;
        mId = id;
        mDocument = document;
    }

    public DocumentWrapper(T document, String partition, String id, String eTag, long timestamp) {
        this(document, partition, id);
        mETag = eTag;
        mLastUpdatedDate = timestamp;
        mDocument = document;
    }

    public DocumentWrapper(Throwable exception) {
        mError = new StorageException(exception);
    }

    public DocumentWrapper(String message, Throwable exception) {
        mError = new StorageException(message, exception);
    }

    public DocumentWrapper(StorageException exception) {
        mError = exception;
    }

    /**
     * Get the deserialized document value.
     *
     * @return Deserialized document.
     */
    public T getDeserializedValue() {
        return mDocument;
    }

    /**
     * Get the error if the document is in this state.
     *
     * @return Document error.
     */
    public StorageException getError() {
        return mError;
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
     * Get last time the document was updated in CosmosDB in UTC unix epoch.
     *
     * @return UTC unix timestamp.
     */
    public long getLastUpdatedDate() {
        return mLastUpdatedDate;
    }

    /**
     * Get the document in its JSON form.
     *
     * @return The document in its JSON form.
     */
    public String getJsonValue() {
        return toString();
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
        return getError() != null;
    }
}
