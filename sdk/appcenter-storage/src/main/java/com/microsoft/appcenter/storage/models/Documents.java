package com.microsoft.appcenter.storage.models;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.util.List;

// A (paginated) list of documents from CosmosDB
public interface Documents<T> extends Iterable<Documents<T>> {

    // List of documents in the current page (or null)
    public List<Document<T>> getDocuments();

    // List of documents (deserialized) in the current page (or null)
    public List<T> asList();

    // Error (or null)
    public DocumentError getError();

    // Flag indicating if an extra page can be fetched
    public boolean hasNext();

    // Fetch more documents
    public AppCenterFuture<Documents<T>> next();

}
