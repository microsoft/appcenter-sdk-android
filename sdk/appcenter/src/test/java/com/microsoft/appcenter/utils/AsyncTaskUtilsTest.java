/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.os.AsyncTask;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AppCenterLog.class)
public class AsyncTaskUtilsTest {

    @SuppressWarnings("InstantiationOfUtilityClass")
    @Test
    public void init() {
        new AsyncTaskUtils();
    }

    @Test
    public void execute() {

        @SuppressWarnings("unchecked")
        AsyncTask<Integer, Void, Void> task = mock(AsyncTask.class);
        when(task.executeOnExecutor(any(), anyInt(), anyInt())).thenReturn(task);
        assertSame(task, AsyncTaskUtils.execute("", task, 1, 2));
        verify(task).executeOnExecutor(any(), eq(1), eq(2));
    }

    @Test
    public void executeFallback() {

        @SuppressWarnings("unchecked")
        AsyncTask<Integer, Void, Void> task = mock(AsyncTask.class);
        mockStatic(AppCenterLog.class);
        RejectedExecutionException exception = new RejectedExecutionException();
        when(task.executeOnExecutor(any(), anyInt(), anyInt())).thenThrow(exception).thenReturn(task);
        assertSame(task, AsyncTaskUtils.execute("", task, 1, 2));
        verify(task, times(2)).executeOnExecutor(any(), eq(1), eq(2));
        verifyStatic(AppCenterLog.class);
        AppCenterLog.warn(eq(""), anyString(), eq(exception));
    }
}
