/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IsRunningInAppCenterTest extends AbstractAppCenterTest {

    @Test
    public void notRunningInAppCenterTestNoEnvironment() {
        assertFalse(AppCenter.isRunningInAppCenterTestCloud());
    }

    @Test
    public void notRunningInAppCenterTest() {
        addRunningInAppCenterToRegistry("0");
        assertFalse(AppCenter.isRunningInAppCenterTestCloud());
    }

    @Test
    public void runningInAppCenterTest() {
        addRunningInAppCenterToRegistry("1");
        assertTrue(AppCenter.isRunningInAppCenterTestCloud());
    }
}
