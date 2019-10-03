/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HttpConnectionRemoveFileTaskTest {

    @Mock
    private File mFile;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void doInBackground() {
        HttpConnectionRemoveFileTask task = new HttpConnectionRemoveFileTask(mFile);
        task.doInBackground();

        /* Verify. */
        verify(mFile).delete();
    }
}