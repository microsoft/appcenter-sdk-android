/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({
        AsyncTaskUtils.class,
        AppCenterLog.class,
        Distribute.class,
        ResumeFromBackgroundTask.class,
        SharedPreferencesManager.class
})
public class ResumeFromBackgroundTaskTest {

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Distribute mDistribute;

    @Before
    public void setUp() {
        mockStatic(AsyncTaskUtils.class);
        mockStatic(Distribute.class);
        mockStatic(SharedPreferencesManager.class);
        when(Distribute.getInstance()).thenReturn(mDistribute);
    }

    @Test
    public void doInBackgroundInvalidId() {
        when(SharedPreferencesManager.getLong(anyString(), anyLong())).thenReturn(-1L);

        /* Start. */
        ResumeFromBackgroundTask task = new ResumeFromBackgroundTask(mContext, 1L);
        task.doInBackground();

        /* Verify. */
        verify(mDistribute).startFromBackground(mContext);
        verify(mDistribute, never()).resumeDownload();
    }

    @Test
    public void doInBackgroundNotExpectedId() {
        when(SharedPreferencesManager.getLong(anyString(), anyLong())).thenReturn(2L);

        /* Start. */
        ResumeFromBackgroundTask task = new ResumeFromBackgroundTask(mContext, 4L);
        task.doInBackground();

        /* Verify. */
        verify(mDistribute).startFromBackground(mContext);
        verify(mDistribute, never()).resumeDownload();
    }

    @Test
    public void doInBackgroundResumeDownloaded() {
        long downloadedId = 2L;
        when(SharedPreferencesManager.getLong(anyString(), anyLong())).thenReturn(downloadedId);

        /* Start. */
        ResumeFromBackgroundTask task = new ResumeFromBackgroundTask(mContext, downloadedId);
        task.doInBackground();

        /* Verify. */
        verify(mDistribute).startFromBackground(mContext);
        verify(mDistribute).resumeDownload();
    }
}