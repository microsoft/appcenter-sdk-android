package com.microsoft.azure.mobile.http;

import org.junit.Test;

import static com.microsoft.azure.mobile.test.TestUtils.checkEquals;
import static com.microsoft.azure.mobile.test.TestUtils.checkNotEquals;
import static com.microsoft.azure.mobile.test.TestUtils.compareSelfNullClass;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unused")
public class HttpExceptionTest {

    @Test
    public void equalsAndHashCode() {
        compareSelfNullClass(new HttpException(401));
        checkEquals(new HttpException(401), new HttpException(401));
        checkNotEquals(new HttpException(401), new HttpException(501));
        checkNotEquals(new HttpException(401, "Unauthorized"), new HttpException(401, "Authentication failure"));
        assertEquals(403, new HttpException(403).getStatusCode());
        assertEquals("", new HttpException(403).getPayload());
        assertEquals("Busy", new HttpException(503, "Busy").getPayload());
    }
}
