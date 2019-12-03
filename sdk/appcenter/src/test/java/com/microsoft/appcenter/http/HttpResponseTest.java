/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static com.microsoft.appcenter.test.TestUtils.compareSelfNullClass;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class HttpResponseTest {

    @Test
    public void equalsAndHashCode() {
        compareSelfNullClass(new HttpResponse(401));
        checkEquals(new HttpResponse(401), new HttpResponse(401));
        checkNotEquals(new HttpResponse(401), new HttpResponse(501));
        checkNotEquals(new HttpResponse(401, "Unauthorized"), new HttpResponse(401, "Authentication failure"));

        Map<String, String> responseHeaders1 = new HashMap<String, String>() {{
            put("Content-Type", "application/json");
            put("x-ms-retry-after-ms", "1234");
        }};

        checkEquals(new HttpResponse(401, "Unauthorized", responseHeaders1), new HttpResponse(401, "Unauthorized", responseHeaders1));
        checkNotEquals(new HttpResponse(401, "Unauthorized"), new HttpResponse(401, "Unauthorized", responseHeaders1));
        Map<String, String> responseHeaders2 = new HashMap<String, String>() {{
            put("Content-Type", "application/json");
            put("x-ms-retry-after-ms", "9090");
        }};

        checkNotEquals(new HttpResponse(401, "Unauthorized", responseHeaders1), new HttpResponse(401, "Unauthorized", responseHeaders2));

        assertEquals(403, new HttpResponse(403).getStatusCode());
        assertEquals("", new HttpResponse(403).getPayload());
        assertEquals("Busy", new HttpResponse(503, "Busy").getPayload());
        assertEquals(2, new HttpResponse(401, "Unauthorized", responseHeaders1).getHeaders().size());
        assertEquals("1234", new HttpResponse(401, "Unauthorized", responseHeaders1).getHeaders().get("x-ms-retry-after-ms"));
    }
}
