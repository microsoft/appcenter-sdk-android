/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;

public interface NextPageDelegate {

    <T> void loadNextPage(
            TokenResult tokenResult,
            DefaultAppCenterFuture<PaginatedDocuments<T>> result,
            ReadOptions readOptions,
            Class<T> documentType,
            String continuationToken);
}

