/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.client.CosmosDb;
import com.microsoft.appcenter.storage.client.TokenExchange;

import org.junit.Test;

/**
 * Just to fix coverage.
 */
public class ConstructorTests {

    @Test
    public void constructors() {
        new Constants();
        new TokenExchange();
        new CosmosDb();
        new Utils();
    }
}
