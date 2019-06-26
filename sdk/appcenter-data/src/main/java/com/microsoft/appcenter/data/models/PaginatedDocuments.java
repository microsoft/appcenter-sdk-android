/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.microsoft.appcenter.data.Constants.LOG_TAG;

public class PaginatedDocuments<T> implements Iterable<DocumentWrapper<T>> {

    private transient Page<T> mCurrentPage;

    private transient TokenResult mTokenResult;

    private transient Class<T> mDocumentType;

    private transient ReadOptions mReadOptions;

    private transient NextPageDelegate mNextPageDelegate;

    /**
     * Continuation token for retrieving the next page.
     */
    private transient String mContinuationToken;

    /**
     * Set the token result.
     *
     * @param tokenResult The token result.
     * @return TokenResult.
     */
    public PaginatedDocuments<T> setTokenResult(TokenResult tokenResult) {
        mTokenResult = tokenResult;
        return this;
    }

    /**
     * Return true if has next page.
     *
     * @return True if has next page.
     */
    public boolean hasNextPage() {
        return mContinuationToken != null;
    }

    /**
     * Set current page.
     *
     * @param currentPage The page to be set to current page.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> setCurrentPage(Page<T> currentPage) {
        mCurrentPage = currentPage;
        return this;
    }

    /**
     * Get current page.
     *
     * @return Current page.
     */
    public Page<T> getCurrentPage() {
        return mCurrentPage;
    }

    /**
     * Set the continuation token.
     *
     * @param continuationToken The continuation token to retrieve the next page.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> setContinuationToken(String continuationToken) {
        mContinuationToken = continuationToken;
        return this;
    }

    /**
     * Set ReadOptions.
     *
     * @param readOptions The read options for the next page.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> setReadOptions(ReadOptions readOptions) {
        mReadOptions = readOptions;
        return this;
    }

    /**
     * Set next page load delegate.
     *
     * @param nextPageDelegate The next page load delegate.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> setNextPageDelegate(NextPageDelegate nextPageDelegate) {
        mNextPageDelegate = nextPageDelegate;
        return this;
    }

    /**
     * Set the document type.
     *
     * @param documentType The document type.
     * @return PaginatedDocuments.
     */
    public PaginatedDocuments<T> setDocumentType(Class<T> documentType) {
        mDocumentType = documentType;
        return this;
    }

    /**
     * Asynchronously fetch the next page.
     *
     * @return Next page.
     */
    public synchronized AppCenterFuture<Page<T>> getNextPage() {
        final DefaultAppCenterFuture<Page<T>> result = new DefaultAppCenterFuture<>();
        if (hasNextPage() && mNextPageDelegate != null) {
            DefaultAppCenterFuture<PaginatedDocuments<T>> paginatedResult = new DefaultAppCenterFuture<>();
            mNextPageDelegate.loadNextPage(mTokenResult, paginatedResult, mReadOptions, mDocumentType, mContinuationToken);
            paginatedResult.thenAccept(new AppCenterConsumer<PaginatedDocuments<T>>() {

                @Override
                public void accept(PaginatedDocuments<T> docs) {
                    setCurrentPage(docs.mCurrentPage);
                    setContinuationToken(docs.mContinuationToken);
                    setNextPageDelegate(docs.mNextPageDelegate);
                    result.complete(getCurrentPage());
                }
            });
        } else {
            result.complete(new Page<T>(new NoSuchElementException()));
        }
        return result;
    }

    @NonNull
    @Override
    public Iterator<DocumentWrapper<T>> iterator() {
        return new Iterator<DocumentWrapper<T>>() {

            private int mCurrentIndex = 0;

            @Override
            public boolean hasNext() {
               List<DocumentWrapper<T>> items = getCurrentPage().getItems();
               return (items != null && mCurrentIndex < items.size()) || hasNextPage();
            }

            @Override
            public DocumentWrapper<T> next() {
                if (!hasNext()) {
                    return new DocumentWrapper<>(new NoSuchElementException());
                } else if (mCurrentIndex >= getCurrentPage().getItems().size()) {
                    mCurrentPage = getNextPage().get();
                    mCurrentIndex = 0;
                }
                return getCurrentPage().getItems().get(mCurrentIndex++);
            }

            @Override
            public void remove() {
                AppCenterLog.error(LOG_TAG, "Remove operation is not supported in the iterator.", new UnsupportedOperationException());
            }
        };
    }
}
