/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.exception.StorageException;

/**
 * A listener that is going to be notified about remote operations completion status.
 */
public interface DataStoreEventListener {

    /**
     * Will be called on device network status changing from offline to online as storage operations are attempted to be sent to Cosmos DB
     *
     * @param operation name
     * @param document  metadata
     * @param error     details. If null, then the operation was successful.
     */
    void onDataStoreOperationResult(String operation, DocumentMetadata document, StorageException error);

}
