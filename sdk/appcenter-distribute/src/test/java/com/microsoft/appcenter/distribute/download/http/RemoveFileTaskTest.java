/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.download.http;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.utils.AsyncTaskUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.File;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        AsyncTaskUtils.class,
        RemoveFileTask.class,
        File.class
})
public class RemoveFileTaskTest {

    /**
     * Log tag for this service.
     */
    private static final String LOG_TAG = AppCenter.LOG_TAG + "Distribute";

    @Rule
    public PowerMockRule mRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(AsyncTaskUtils.class);
    }

    @Test
    public void doInBackground() {
        File mockFile = mock(File.class);

        /* Start. */
        startDoInBackground();
        RemoveFileTask task = AsyncTaskUtils.execute(LOG_TAG, new RemoveFileTask(mockFile));
        task.doInBackground(null);

        /* Verify. */
        verify(mockFile).delete();
    }

    private void startDoInBackground() {
        final RemoveFileTask[] task = {null};
        when(AsyncTaskUtils.execute(anyString(), isA(RemoveFileTask.class))).then(new Answer<RemoveFileTask>() {
            @Override
            public RemoveFileTask answer(InvocationOnMock invocation) {
                task[0] = (RemoveFileTask) invocation.getArguments()[1];
                return task[0];
            }
        });
    }
}