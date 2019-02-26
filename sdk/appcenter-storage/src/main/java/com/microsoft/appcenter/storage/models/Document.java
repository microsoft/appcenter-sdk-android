package com.microsoft.appcenter.storage.models;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.microsoft.appcenter.storage.Utils;

import java.lang.reflect.Type;

// A document coming back from CosmosDB
public class Document<T> {

    private transient String documentPayload;

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

    private Document(String partition, String id) {
        this.partition = partition;
        this.id = id;
    }

    public Document(String documentPayload, String partition, String id) {
        this(partition, id);
        this.documentPayload = documentPayload;
    }

    public Document(T document, String partition, String id)
    {
        this(partition, id);
        this.document = document;
    }

    public Document(Exception dError)
    {
        this.documentError = new DocumentError(dError);
    }

    // Non-serialized document (or null)
    public String getJsonDocument()
    {
        return documentPayload;
    }

    // Deserialized document (or null)
    public T getDocument()
    {
        Type documentType = new TypeToken<Document<T>>(){}.getType();
        return Utils.sGson.fromJson(documentPayload, documentType);
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
