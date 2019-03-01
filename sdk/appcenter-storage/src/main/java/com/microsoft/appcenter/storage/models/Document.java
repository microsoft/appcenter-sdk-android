package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Utils;

/**
 * A document coming back from CosmosDB
 * @param <T>
 */
public class Document<T> {
    @SerializedName(value = "PartitionKey")
    private String partition;

    @SerializedName(value = "id")
    private String id;

    @SerializedName(value = "_etag")
    private String eTag;

    @SerializedName(value = "_ts")
    private long   timestamp;

    @SerializedName(value = "document")
    private T document;

    private transient DocumentError documentError;

    public Document() {
    }

    public Document(T document, String partition, String id)
    {
        this.partition = partition;
        this.id = id;
        this.document = document;
    }

    public Document(T document, String partition, String id, String eTag, long timestamp) {
        this(document, partition, id);
        this.eTag = eTag;
        this.timestamp = timestamp;
        this.document = document;
    }

    public Document(Exception dError)
    {
        this.documentError = new DocumentError(dError);
    }

    /**
     * @return Deserialized document (or null)
     */
    public T getDocument()
    {
        return document;
    }

    // Error (or null)
    public DocumentError getError()
    {
        return documentError;
    }

    // ID + document metadata
    public String getPartition()
    {
        return partition;
    }

    public String getId()
    {
        return id;
    }

    public String getEtag()
    {
        return eTag;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    @Override
    public String toString() {
        return Utils.sGson.toJson(this);
    }

    // When caching is supported:
    // Flag indicating if data was retrieved from the local cache (for offline mode)
    // public boolean isFromCache();
}
