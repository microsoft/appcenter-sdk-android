/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import android.app.DownloadManager;
import android.content.Context;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static android.content.Context.DOWNLOAD_SERVICE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerRemoveTaskTest {

    private static final long DOWNLOAD_ID = 42;

    @Mock
    private Context mContext;

    @Mock
    private DownloadManager mDownloadManager;

    @Test
    public void doInBackground() {
        when(mContext.getSystemService(DOWNLOAD_SERVICE)).thenReturn(mDownloadManager);
        DownloadManagerRemoveTask task = new DownloadManagerRemoveTask(mContext, DOWNLOAD_ID);
        task.doInBackground();

        /* Verify. */
        verify(mDownloadManager).remove(DOWNLOAD_ID);
    }
}