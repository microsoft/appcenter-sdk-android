/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;
import static com.microsoft.appcenter.test.TestUtils.compareSelfNullClass;

@SuppressWarnings("unused")
public class HttpExceptionTest {

    @Test
    public void equalsAndHashCode() {
        compareSelfNullClass(new HttpException(new HttpResponse(501)));
        checkEquals(new HttpException(new HttpResponse(401)), new HttpException(new HttpResponse(401)));
        checkNotEquals(new HttpException(new HttpResponse(201)), new HttpException(new HttpResponse(302)));
    }
}
