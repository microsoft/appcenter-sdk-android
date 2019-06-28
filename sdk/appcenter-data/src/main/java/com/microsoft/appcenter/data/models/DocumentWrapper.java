/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.data.Constants;
import com.microsoft.appcenter.data.Utils;
import com.microsoft.appcenter.data.exception.DataException;

import java.util.Date;

/**
 * A document coming back from CosmosDB.
 */
public class DocumentWrapper<T> extends DocumentMetadata {

    /**
     * Last updated time in seconds.
     * Gets deserialized from cosmosDB payload.
     * Uploading this value back to cosmosDB doesn't update the value. CosmosDB automatically forces the
     * value to be the value of the operation regardless of what it is set to at the time of serialization.
     */
    @SerializedName(value = Constants.TIMESTAMP_FIELD_NAME)
    private long mLastUpdatedDate;

    @SerializedName(value = Constants.DOCUMENT_FIELD_NAME)
    private T mDocument;

    private transient DataException mError;

    private transient boolean mFromDeviceCache;

    private transient String mPendingOperation;

    public DocumentWrapper() {
    }

    public DocumentWrapper(T document, String partition, String id) {
        super(partition, id, null);
        mDocument = document;
    }

    /**
     Init.
     * @param document        User payload.
     * @param partition       Partition containing the document.
     * @param id              Document identifier.
     * @param eTag            Document eTag.
     * @param lastUpdatedDate Last updated time in seconds.
     */
    public DocumentWrapper(T document, String partition, String id, String eTag, long lastUpdatedDate) {
        this(document, partition, id);
        mETag = eTag;
        mLastUpdatedDate = lastUpdatedDate;
    }

    /**
     Init.
     * @param document        User payload.
     * @param partition       Partition containing the document.
     * @param id              Document identifier.
     * @param eTag            Document eTag.
     * @param lastUpdatedDate Last updated time in seconds.
     * @param exception       Exception in case there is an error initializing document wrapper
     */
    public DocumentWrapper(T document, String partition, String id, String eTag, long lastUpdatedDate, DataException exception) {
        this(document, partition, id, eTag, lastUpdatedDate);
        mError = exception;
    }

    public DocumentWrapper(Exception exception) {
        mError = new DataException(exception);
    }

    public DocumentWrapper(String message, Exception exception) {
        mError = new DataException(message, exception);
    }

    public DocumentWrapper(DataException exception) {
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
    public DataException getError() {
        return mError;
    }

    /**
     * Get last time the document was updated in CosmosDB in UTC unix epoch.
     *
     * @return UTC unix timestamp.
     */
    public Date getLastUpdatedDate() {

        /* Since we use Gson, we need to keep mLastUpdatedDate in seconds for serialization. */
        return new Date(mLastUpdatedDate * 1000L);
    }

    /**
     * Get the document value in its JSON form.
     *
     * @return The document value in its JSON form.
     */
    public String getJsonValue() {
        return mDocument == null ? null : Utils.getGson().toJson(mDocument);
    }

    /**
     * Get the document wrapper as a string.
     *
     * @return Serialized document wrapper.
     */
    @NonNull
    @Override
    public String toString() {
        return Utils.getGson().toJson(this);
    }

    /**
     * Get the flag indicating if data was retrieved from the local cache (for offline mode).
     *
     * @return true if the document was retrieved from the local cache, false otherwise.
     */
    public boolean isFromDeviceCache() {
        return mFromDeviceCache;
    }

    /**
     * Set the flag indicating if data was retrieved from the local cache (for offline mode).
     *
     * @param fromCache true to indicate this data is from the local cache, false otherwise.
     */
    public void setFromCache(boolean fromCache) {
        mFromDeviceCache = fromCache;
    }

    /**
     * Get the pending operation value.
     *
     * @return the pending operation value.
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
