/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ErrorDetailsTest {

    @Test
    public void parseErrorCode() throws JSONException {
        ErrorDetails errorDetails = ErrorDetails.parse("{code:'test'}");
        assertEquals("test", errorDetails.getCode());
    }

    @Test(expected = JSONException.class)
    public void missingErrorCode() throws JSONException {
        ErrorDetails.parse("{}");
    }
}
