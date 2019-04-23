/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.data.Constants;
import com.microsoft.appcenter.data.exception.StorageException;

import java.util.List;

public class Page<T> {

    /**
     * Documents in the page.
     */
    @SerializedName(value = Constants.DOCUMENTS_FIELD_NAME)
    private List<DocumentWrapper<T>> mItems;

    /**
     * Document error.
     */
    private transient StorageException mError;

    public Page() {
    }

    public Page(Exception exception) {
        mError = new StorageException(exception);
    }

    public Page(StorageException exception) {
        mError = exception;
    }

    /**
     * Return the documents in the page.
     *
     * @return Documents in current page.
     */
    public List<DocumentWrapper<T>> getItems() {
        return mItems;
    }

    public Page<T> setItems(List<DocumentWrapper<T>> items) {
        mItems = items;
        return this;
    }

    /**
     * Get the error if failed to retrieve the page from document db.
     *
     * @return DocumentError.
     */
    public StorageException getError() {
        return mError;
    }
}
