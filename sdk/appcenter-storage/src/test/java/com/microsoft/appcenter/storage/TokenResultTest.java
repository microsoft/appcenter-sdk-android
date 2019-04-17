/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import com.microsoft.appcenter.storage.models.TokenResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TokenResultTest {

    @Test
    public void setAccountId() {
        TokenResult result = new TokenResult();
        result.withAccountId("someId");
        assertEquals("someId", result.getAccountId());
    }
}
