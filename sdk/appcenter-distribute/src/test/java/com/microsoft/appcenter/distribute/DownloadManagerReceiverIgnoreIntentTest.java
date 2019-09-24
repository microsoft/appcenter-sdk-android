/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Distribute.class)
public class DownloadManagerReceiverIgnoreIntentTest {

    @Test
    public void invalidIntent() {
        mockStatic(Distribute.class);
        when(Distribute.getInstance()).thenReturn(mock(Distribute.class));
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(Intent.ACTION_ANSWER);
        new DownloadManagerReceiver().onReceive(mock(Context.class), clickIntent);
        when(clickIntent.getAction()).thenReturn(null);
        new DownloadManagerReceiver().onReceive(mock(Context.class), clickIntent);
        verifyStatic(never());
        Distribute.getInstance();
    }
}
