/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;

import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

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

    @Test
    public void notRunningInAppCenterTestWhenIllegalStateException() throws Exception {
        mockStatic(InstrumentationRegistryHelper.class);
        doThrow(new IllegalStateException()).when(InstrumentationRegistryHelper.class, "getArguments");
        assertFalse(AppCenter.isRunningInAppCenterTestCloud());
    }

    @Test
    public void notRunningInAppCenterTestWhenExceptionInClass() throws Exception {
        spy(InstrumentationRegistryHelper.class);
        Method getClass = method(InstrumentationRegistryHelper.class, "getClass", String.class);
        doThrow(new ClassNotFoundException()).when(InstrumentationRegistryHelper.class, getClass)
                .withArguments(any(String.class));
        assertFalse(AppCenter.isRunningInAppCenterTestCloud());
    }
}
