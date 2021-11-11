/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Intent;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import static android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class DistributePlusDownloadReceiverTest extends AbstractDistributeTest {

    @Test
    public void resumeAppBeforeStart() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(mContext, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadManagerReceiver().onReceive(mContext, clickIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        verify(mContext).startActivity(startIntent);
    }

    @Test
    public void resumeAfterBeforeStartButBackground() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        start();
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(mContext, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadManagerReceiver().onReceive(mContext, clickIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        verify(mContext).startActivity(startIntent);
    }

    @Test
    public void resumeForegroundThenPause() throws Exception {
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_UPDATE_TOKEN))).thenReturn("mock");
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        start();
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(mContext, DeepLinkActivity.class).thenReturn(startIntent);
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        new DownloadManagerReceiver().onReceive(mContext, clickIntent);
        verify(mContext, never()).startActivity(startIntent);

        /* Then pause and test again. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        new DownloadManagerReceiver().onReceive(mContext, clickIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        verify(mContext).startActivity(startIntent);
    }
}
