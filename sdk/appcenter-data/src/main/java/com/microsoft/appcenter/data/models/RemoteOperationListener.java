/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data.models;

import com.microsoft.appcenter.data.exception.DataException;

/**
 * A listener used to be notified about remote operations completion status.
 */
public interface RemoteOperationListener {

    /**
     * Called when device network status changes from offline to online as data operations are performed against CosmosDB.
     *
     * @param operation        operation that ran.
     * @param documentMetadata metadata about the document that was synced.
     * @param error            error details or null if the sync was successful.
     */
    void onRemoteOperationCompleted(String operation, DocumentMetadata documentMetadata, DataException error);

}
