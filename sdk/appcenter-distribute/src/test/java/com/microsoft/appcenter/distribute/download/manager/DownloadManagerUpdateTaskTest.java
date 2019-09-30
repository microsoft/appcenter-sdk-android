/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.manager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DownloadManagerUpdateTaskTest {

    @Mock
    private DownloadManagerReleaseDownloader mDownloader;

    @Test
    public void doInBackground() {
        DownloadManagerUpdateTask task = new DownloadManagerUpdateTask(mDownloader);
        task.doInBackground(null);

        /* Verify. */
        verify(mDownloader).onUpdate();
    }
}