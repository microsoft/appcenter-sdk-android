/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

public interface NextPageDelegate {
    <T> void LoadNextPage(
            final TokenResult tokenResult,
            final DefaultAppCenterFuture<PaginatedDocuments<T>> result,
            final ReadOptions readOptions,
            final Class<T> documentType,
            final String continuationToken);
}

