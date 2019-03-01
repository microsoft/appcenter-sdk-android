package com.microsoft.appcenter.storage.models;

import java.util.List;

public class Page<T> {

    /**
     * Documents in the page.
     */
    private List<Document<T>> documents;

    private DocumentError documentError;

    public Page() {

    }

    public Page(Exception exception) {
        this.documentError = new DocumentError(exception);
    }

    /**
     * Return the documents in the page.
     * @return Documents in current page.
     */
    public List<Document<T>> getItems() {
        return documents;
    }

    public Page<T> withDocuments(List<Document<T>> documents) {
        this.documents =documents;
        return this;
    }

    /**
     * Get the error if failed to retrieve the page from document db.
     * @return DocumentError.
     */
    public DocumentError getError() {
        return documentError;
    }
}