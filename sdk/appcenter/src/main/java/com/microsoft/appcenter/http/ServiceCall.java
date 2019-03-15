/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

public interface ServiceCall {

    /**
     * Cancel the call if possible.
     */
    void cancel();
}
