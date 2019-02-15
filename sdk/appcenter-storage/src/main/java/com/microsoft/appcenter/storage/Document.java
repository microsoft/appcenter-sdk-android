package com.microsoft.appcenter.storage;

import java.util.Date;

// A document coming back from CosmosDB
public interface Document<T> {

    // Non-serialized document (or null)
    public String getJsonDocument();

    // Deserialized document (or null)
    public T getDocument();

    // Error (or null)
    public DataStoreError getError();

    // ID + document metadata
    public String getPartition();
    public String getId();
    public String getEtag();
    public Date getDate();

    // When caching is supported:
    // Flag indicating if data was retrieved from the local cache (for offline mode)
    // public boolean isFromCache();

}
