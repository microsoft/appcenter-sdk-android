package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Test;

import static android.app.DownloadManager.ACTION_NOTIFICATION_CLICKED;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class UpdatesPlusDownloadReceiverTest extends AbstractUpdatesTest {

    @Test
    public void resumeAppBeforeStart() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadManagerReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void resumeAfterBeforeStartButBackground() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Updates.getInstance().onStarted(context, "", mock(Channel.class));
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadManagerReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void resumeForegroundThenPause() throws Exception {
        when(StorageHelper.PreferencesStorage.getString(eq(PREFERENCE_KEY_UPDATE_TOKEN))).thenReturn("mock");
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Updates.getInstance().onStarted(context, "", mock(Channel.class));
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        new DownloadManagerReceiver().onReceive(context, clickIntent);
        verify(context, never()).startActivity(startIntent);

        /* Then pause and test again. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        new DownloadManagerReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
