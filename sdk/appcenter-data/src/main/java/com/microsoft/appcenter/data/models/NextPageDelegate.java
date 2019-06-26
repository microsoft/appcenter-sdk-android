/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

/**
 * A next page load delegate used to pull the next available page of the List documents method.
 */
public interface NextPageDelegate {

    /**
     * Called when the user wants to load next page of documents from the server.
     *
     * @param tokenResult The token result.
     * @param result PaginatedDocuments wrapper that will be updated with the results of the call.
     * @param readOptions The read options for the next page.
     * @param documentType The document type.
     * @param continuationToken The continuation token to retrieve the next page.
     * @param <T> A type for the deserialized instance.
     */
    <T> void loadNextPage(
            TokenResult tokenResult,
            DefaultAppCenterFuture<PaginatedDocuments<T>> result,
            ReadOptions readOptions,
            Class<T> documentType,
            String continuationToken);
}

