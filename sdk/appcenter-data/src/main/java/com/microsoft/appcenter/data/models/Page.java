/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.data.Constants;
import com.microsoft.appcenter.data.exception.DataException;

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
    private transient DataException mError;

    public Page() {
    }

    public Page(Exception exception) {
        mError = new DataException(exception);
    }

    public Page(DataException exception) {
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
     * @return Error.
     */
    public DataException getError() {
        return mError;
    }
}
