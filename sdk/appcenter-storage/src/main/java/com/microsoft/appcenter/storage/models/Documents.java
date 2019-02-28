package com.microsoft.appcenter.storage.models;

import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.util.List;

/**
 * A (paginated) list of documents from CosmosDB.
 *
 * @param <T> The class type of the document.
 */
public interface Documents<T> extends Iterable<Documents<T>> {

    /**
     * List of documents in the current page.
     * @return List of documents.
     */
    List<Document<T>> getDocuments();

    /**
     * List of documents (deserialized) in the current page.
     * @return List of documents.
     */
    List<T> asList();

    /**
     * Get document error.
     * @return Document error.
     */
    DocumentError getError();

    /**
     * Flag indicating if an extra page can be fetched.
     * @return True if an extra page can be fetched.
     */
    boolean hasNext();

    /**
     * Fetch more documents.
     * @return Next document.
     */
    AppCenterFuture<Documents<T>> next();

}
