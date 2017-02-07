package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({Updates.class, StorageHelper.PreferencesStorage.class, MobileCenterLog.class, MobileCenter.class})
public class UpdatesTest {

    private static final String UPDATES_ENABLED_KEY = KEY_ENABLED + "_Updates";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        Updates.unsetInstance();
        mockStatic(MobileCenterLog.class);
        mockStatic(MobileCenter.class);
        when(MobileCenter.isEnabled()).thenReturn(true);

        /* First call to com.microsoft.azure.mobile.MobileCenter.isEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(UPDATES_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(UPDATES_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(UPDATES_ENABLED_KEY), anyBoolean());
    }

    @Test
    public void singleton() {
        Assert.assertSame(Updates.getInstance(), Updates.getInstance());
    }

    @Test
    public void resumeAppBeforeStart() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadCompletionReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void resumeAfterBeforeStartButBackground() throws Exception {
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Updates.getInstance().onStarted(context, "", mock(Channel.class));
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        new DownloadCompletionReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Test
    public void resumeForegroundThenPause() throws Exception {
        when(StorageHelper.PreferencesStorage.getString(eq(Updates.PREFERENCE_KEY_UPDATE_TOKEN))).thenReturn("mock");
        Intent clickIntent = mock(Intent.class);
        when(clickIntent.getAction()).thenReturn(DownloadManager.ACTION_NOTIFICATION_CLICKED);
        Context context = mock(Context.class);
        Updates.getInstance().onStarted(context, "", mock(Channel.class));
        Intent startIntent = mock(Intent.class);
        whenNew(Intent.class).withArguments(context, DeepLinkActivity.class).thenReturn(startIntent);
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        new DownloadCompletionReceiver().onReceive(context, clickIntent);
        verify(context, never()).startActivity(startIntent);

        /* Then pause and test again. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        new DownloadCompletionReceiver().onReceive(context, clickIntent);
        verify(context).startActivity(startIntent);
        verify(startIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
