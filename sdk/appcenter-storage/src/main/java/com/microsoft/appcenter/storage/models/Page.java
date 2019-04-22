/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.exception.DocumentError;

import java.util.List;

public class Page<T> {

    /**
     * Documents in the page.
     */
    @SerializedName(value = Constants.DOCUMENTS_FIELD_NAME)
    private List<Document<T>> mItems;

    /**
     * Document error.
     */
    private transient DocumentError mError;

    public Page() {
    }

    public Page(Exception exception) {
        mError = new DocumentError(exception);
    }

    /**
     * Return the documents in the page.
     *
     * @return Documents in current page.
     */
    public List<Document<T>> getItems() {
        return mItems;
    }

    public Page<T> setItems(List<Document<T>> items) {
        mItems = items;
        return this;
    }

    /**
     * Get the error if failed to retrieve the page from document db.
     *
     * @return DocumentError.
     */
    public DocumentError getError() {
        return mError;
    }
}
