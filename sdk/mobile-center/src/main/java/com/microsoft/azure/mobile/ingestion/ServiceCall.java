/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.ingestion;

public interface ServiceCall {

    /**
     * Cancel the call if possible.
     */
    void cancel();
}
