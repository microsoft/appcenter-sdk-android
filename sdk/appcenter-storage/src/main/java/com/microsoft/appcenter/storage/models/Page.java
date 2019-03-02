package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Constants;

import java.util.List;

public class Page<T> {

    /**
     * Documents in the page.
     */
    @SerializedName(value = Constants.DOCUMENTS_FILED_NAME)
    private List<Document<T>> documents;

    /**
     * Document error.
     */
    private DocumentError error;

    public Page() {
    }

    public Page(Exception exception) {
        this.error = new DocumentError(exception);
    }

    /**
     * Return the documents in the page.
     *
     * @return Documents in current page.
     */
    public List<Document<T>> getItems() {
        return documents;
    }

    public Page<T> withDocuments(List<Document<T>> documents) {
        this.documents = documents;
        return this;
    }

    /**
     * Get the error if failed to retrieve the page from document db.
     *
     * @return DocumentError.
     */
    public DocumentError getError() {
        return error;
    }
}
