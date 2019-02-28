package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Utils;

/** A document coming back from CosmosDB. */
public class Document<T> {
    @SerializedName(value = "PartitionKey")
    private String partition;

    @SerializedName(value = "id")
    private String id;

    @SerializedName(value = "_etag")
    private String eTag;

    @SerializedName(value = "_ts")
    private long timestamp;

    @SerializedName(value = "document")
    private T document;

    private transient DocumentError documentError;

    public Document() {
    }

    public Document(T document, String partition, String id) {
        this.partition = partition;
        this.id = id;
        this.document = document;
    }

    /**
     * Create document from error.
     * @param exception Error when retrieving the document.
     */
    public Document(Exception exception) {
        this.documentError = new DocumentError(exception);
    }

    /**
     * Get document.
     * @return Deserialized document.
     */
    public T getDocument() {
        return document;
    }

    /**
     * Get document error.
     * @return Document error.
     */
    public DocumentError getError() {
        return documentError;
    }

    /**
     * Get document partition.
     * @return Document partition.
     */
    public String getPartition() {
        return partition;
    }

    /**
     * Get document id.
     * @return Document id.
     */
    public String getId() {
        return id;
    }

    /**
     * Get Etag.
     * @return Etag.
     */
    public String getEtag() {
        return eTag;
    }

    /**
     * Get document generated in UTC unix epoch.
     * @return UTC unix timestamp.
     */
    public long getTimestamp() {
        return timestamp;
    }


    /**
     * Get the document in string.
     * @return Serialized document.
     */
    @Override
    public String toString() {
        return Utils.sGson.toJson(this);
    }

    // When caching is supported:
    // Flag indicating if data was retrieved from the local cache (for offline mode)
    // public boolean isFromCache();

}
