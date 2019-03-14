/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unused")
public class ConstantsAndroidTest {

    @Before
    public void setUp() {
        Constants.FILES_PATH = null;
    }

    @Test
    public void loadFromContext() {
        Constants.loadFromContext(InstrumentationRegistry.getContext());
        Assert.assertNotNull(Constants.FILES_PATH);
    }

    @Test
    public void loadFromContextNullContext() {
        boolean debuggableFlag = Constants.APPLICATION_DEBUGGABLE;
        Constants.loadFromContext(null);
        Assert.assertNull(Constants.FILES_PATH);
        Assert.assertEquals(debuggableFlag, Constants.APPLICATION_DEBUGGABLE);

        debuggableFlag = Constants.APPLICATION_DEBUGGABLE = !debuggableFlag;
        Constants.loadFromContext(null);
        Assert.assertEquals(debuggableFlag, Constants.APPLICATION_DEBUGGABLE);
    }

    @Test
    public void loadFilesPathError() {
        Context mockContext = mock(Context.class);
        when(mockContext.getFilesDir()).thenReturn(null);

        Constants.loadFromContext(mockContext);

        /* Should return null, not throw an exception. */
        Assert.assertNull(Constants.FILES_PATH);
    }

    @Test
    public void loadDebuggableFlag() {
        Context mockContext = mock(Context.class);
        ApplicationInfo mockApplicationInfo = mock(ApplicationInfo.class);
        when(mockContext.getApplicationInfo()).thenReturn(mockApplicationInfo);

        mockApplicationInfo.flags = ApplicationInfo.FLAG_DEBUGGABLE;
        Constants.loadFromContext(mockContext);
        Assert.assertTrue(Constants.APPLICATION_DEBUGGABLE);

        mockApplicationInfo.flags = 0;
        Constants.loadFromContext(mockContext);
        Assert.assertFalse(Constants.APPLICATION_DEBUGGABLE);
    }
}
