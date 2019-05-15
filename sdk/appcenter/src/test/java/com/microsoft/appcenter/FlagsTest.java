/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class FlagsTest {

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void persistenceNone() {
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(0, false));
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(0, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @Test
    public void persistenceNormal() {
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(Flags.NORMAL, false));
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(Flags.NORMAL, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @Test
    public void persistenceCritical() {
        assertEquals(Flags.CRITICAL, Flags.getPersistenceFlag(Flags.CRITICAL, false));
        assertEquals(Flags.CRITICAL, Flags.getPersistenceFlag(Flags.CRITICAL, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void persistenceBackwardCompatible() {
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(Flags.PERSISTENCE_NORMAL, true));
        assertEquals(Flags.CRITICAL, Flags.getPersistenceFlag(Flags.PERSISTENCE_CRITICAL, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @Test
    public void persistenceDefaults() {
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(Flags.DEFAULTS, false));
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(Flags.DEFAULTS, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @Test
    public void persistenceCriticalPlusOtherFlag() {
        assertEquals(Flags.CRITICAL, Flags.getPersistenceFlag(Flags.CRITICAL | 0x0100, false));
        assertEquals(Flags.CRITICAL, Flags.getPersistenceFlag(Flags.CRITICAL | 0x0200, true));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());
    }

    @Test
    public void persistenceInvalidFlag() {

        /* Fallback without warning. */
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(0x09, false));
        verifyStatic(never());
        AppCenterLog.warn(anyString(), anyString());

        /* Fallback with warning. */
        assertEquals(Flags.NORMAL, Flags.getPersistenceFlag(0x09, true));
        verifyStatic();
        AppCenterLog.warn(anyString(), anyString());
    }
}
