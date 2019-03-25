/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage.models;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.Constants;
import com.microsoft.appcenter.storage.Utils;
import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import static com.microsoft.appcenter.storage.Constants.LOG_TAG;

public class PaginatedDocuments<T> implements Iterable<Document<T>> {

    private transient Page<T> currentPage;

    private transient TokenResult tokenResult;

    private transient HttpClient httpClient;

    private transient Class<T> documentType;

    /**
     * Continuation token for retrieving the next page.
     */
    private transient String continuationToken;

    /**
     * Set the token result.
     *
     * @param tokenResult The token result.
     * @return TokenResult.
     */
    public PaginatedDocuments<T> withTokenResult(TokenResult tokenResult) {
        this.tokenResult = tokenResult;
        return this;
    }

    /**
     * Return true if has next page.
     *
     * @return True if has next page.
     */
    public boolean hasNextPage() {
        return continuationToken != null;
    }

    /**
     * Set current page.
     *
     * @param currentPage The page to be set to current page.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> withCurrentPage(Page<T> currentPage) {
        this.currentPage = currentPage;
        return this;
    }

    /**
     * Get current page.
     *
     * @return Current page.
     */
    public Page<T> getCurrentPage() {
        return this.currentPage;
    }

    /**
     * Set the httpclient.
     *
     * @param httpClient The httpclient to be set.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> withHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
        return this;
    }

    /**
     * Set the continuation token.
     *
     * @param continuationToken The continuation token to retrieve the next page.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> withContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
        return this;
    }

    /**
     * Set the document type.
     *
     * @param documentType The document type.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> withDocumentType(Class<T> documentType) {
        this.documentType = documentType;
        return this;
    }

    /**
     * Asynchronously fetch the next page.
     *
     * @return Next page.
     */
    public AppCenterFuture<Page<T>> getNextPage() {
        final DefaultAppCenterFuture<Page<T>> result = new DefaultAppCenterFuture<>();
        if (hasNextPage()) {
            CosmosDb.callCosmosDbListApi(
                    tokenResult,
                    continuationToken,
                    httpClient,
                    new ServiceCallback() {

                        @Override
                        public void onCallSucceeded(String payload, Map<String, String> headers) {
                            Page<T> page = Utils.parseDocuments(payload, documentType);
                            currentPage = page;
                            continuationToken = headers.get(Constants.CONTINUATION_TOKEN_HEADER);
                            result.complete(page);
                        }

                        @Override
                        public void onCallFailed(Exception e) {
                            result.complete(new Page<T>(e));
                        }
                    });
        } else {
            result.complete(new Page<T>(new NoSuchElementException()));
        }
        return result;
    }

    @NonNull
    @Override
    public Iterator<Document<T>> iterator() {
        return new Iterator<Document<T>>() {

            private int currentIndex = 0;

            @Override
            public boolean hasNext() {
                return currentIndex < getCurrentPage().getItems().size() || hasNextPage();
            }

            @Override
            public Document<T> next() {
                if (!hasNext()) {
                    return new Document<>(new NoSuchElementException());
                } else if (currentIndex >= getCurrentPage().getItems().size()) {
                    currentPage = getNextPage().get();
                    currentIndex = 0;
                }
                return getCurrentPage().getItems().get(currentIndex++);
            }

            @Override
            public void remove() {
                AppCenterLog.error(LOG_TAG, "Remove operation is not supported in the iterator.", new UnsupportedOperationException());
            }
        };
    }
}