/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;

import com.microsoft.appcenter.utils.AsyncTaskUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({AsyncTaskUtils.class, Distribute.class, DownloadManagerReceiver.class})
public class DownloadManagerReceiverTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    Distribute mDistribute;

    @Before
    public void setUp() {
        mockStatic(Distribute.class);
        mockStatic(AsyncTaskUtils.class);
        when(Distribute.getInstance()).thenReturn(mDistribute);
    }

    @Test
    public void onReceiveResumeApp() {
        Context mockContext = mock(Context.class);
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getAction()).thenReturn(DownloadManager.ACTION_NOTIFICATION_CLICKED);

        /* Start. */
        DownloadManagerReceiver downloadManagerReceiver = new DownloadManagerReceiver();
        downloadManagerReceiver.onReceive(mockContext, mockIntent);

        /* Verify. */
        verify(mDistribute).resumeApp(mockContext);
    }

    @Test
    public void onReceiveInvalidDownloadIdentifier() {
        Context mockContext = mock(Context.class);
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(mockIntent.getLongExtra(anyString(), anyLong())).thenReturn(-1L);

        /* Start. */
        DownloadManagerReceiver downloadManagerReceiver = new DownloadManagerReceiver();
        downloadManagerReceiver.onReceive(mockContext, mockIntent);

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(ResumeFromBackgroundTask.class));
    }

    @Test
    public void onReceiveProperDownloadIdentifier() {
        Context mockContext = mock(Context.class);
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.getAction()).thenReturn(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        when(mockIntent.getLongExtra(anyString(), anyLong())).thenReturn(1L);

        /* Start. */
        DownloadManagerReceiver downloadManagerReceiver = new DownloadManagerReceiver();
        downloadManagerReceiver.onReceive(mockContext, mockIntent);

        /* Verify. */
        verifyStatic(AsyncTaskUtils.class);
        AsyncTaskUtils.execute(anyString(), isA(ResumeFromBackgroundTask.class));
    }

    @Test
    public void invalidIntent() {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(Intent.ACTION_ANSWER);
        new DownloadManagerReceiver().onReceive(mock(Context.class), clickIntent);
        when(clickIntent.getAction()).thenReturn(null);
        new DownloadManagerReceiver().onReceive(mock(Context.class), clickIntent);

        /* Verify. */
        verifyStatic(Distribute.class, never());
        Distribute.getInstance();
        verifyStatic(AsyncTaskUtils.class, never());
        AsyncTaskUtils.execute(anyString(), isA(ResumeFromBackgroundTask.class));
    }
}