/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
public class UUIDUtilsTest {

    @Test
    public void utilsCoverage() {
        new UUIDUtils();
    }

    @Test
    public void secureRandom() {
        UUID uuid = UUIDUtils.randomUUID();
        assertEquals(4, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    public void securityException() {
        UUIDUtils.sImplementation = mock(UUIDUtils.Implementation.class);
        when(UUIDUtils.sImplementation.randomUUID()).thenThrow(new SecurityException("mock"));
        for (int i = 0; i < 2; i++) {
            UUID uuid = UUIDUtils.randomUUID();
            assertEquals(4, uuid.version());
            assertEquals(2, uuid.variant());
        }
    }
}
