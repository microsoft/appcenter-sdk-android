/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import com.microsoft.appcenter.data.models.TokenResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TokenResultTest {

    @Test
    public void setAccountId() {
        TokenResult result = new TokenResult();
        result.setAccountId("someId");
        assertEquals("someId", result.getAccountId());
    }
}
