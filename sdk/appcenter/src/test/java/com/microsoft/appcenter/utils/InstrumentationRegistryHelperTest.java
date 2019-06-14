/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import com.microsoft.appcenter.AppCenter;

import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.support.membermodification.MemberMatcher.method;

@PrepareForTest({
        InstrumentationRegistryHelper.class
})
public class InstrumentationRegistryHelperTest {

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Test
    public void instrumentationRegistryHelperCoverage() {
        new InstrumentationRegistryHelper();
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