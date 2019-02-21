package com.microsoft.appcenter.storage.models;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Date;

// A document coming back from CosmosDB
public class Document<T> {

    private String documentPayload;
    private String partition;
    private String id;

    @Expose
    @SerializedName(value = "_etag")
    private String eTag;

    @Expose
    @SerializedName(value = "_ts")
    private long   timestamp;

    public Document(String documentPayload, String partition, String id) {
        this.documentPayload = documentPayload;
        this.partition = partition;
        this.id = id;
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
        Gson gson = new Gson();
        return gson.fromJson(documentPayload, documentType);
    }

    // Error (or null)
    public DataStoreError getError()
    {
        return null;
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


    // When caching is supported:
    // Flag indicating if data was retrieved from the local cache (for offline mode)
    // public boolean isFromCache();

}
