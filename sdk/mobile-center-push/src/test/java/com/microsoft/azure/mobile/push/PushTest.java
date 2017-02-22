package com.microsoft.azure.mobile.push;

import android.content.Context;

import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.concurrent.Executor;

import static com.microsoft.azure.mobile.push.Push.PREFERENCE_KEY_PUSH_TOKEN;
import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Push.class,
        Push.PushTokenTask.class,
        PushInstallationLog.class,
        MobileCenterLog.class,
        MobileCenter.class,
        StorageHelper.PreferencesStorage.class
})
public class PushTest {

    private static final String PUSH_ENABLED_KEY = KEY_ENABLED + "_group_push";

    @Mock
    Push.PushTokenTask mPushTokenTask;

    @Before
    public void setUp() throws Exception {
        Push.unsetInstance();
        mockStatic(MobileCenterLog.class);
        mockStatic(MobileCenter.class);
        when(MobileCenter.isEnabled()).thenReturn(true);

        /* First call to com.microsoft.azure.mobile.MobileCenter.isEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(PUSH_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                Mockito.when(StorageHelper.PreferencesStorage.getBoolean(PUSH_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(PUSH_ENABLED_KEY), anyBoolean());

        whenNew(Push.PushTokenTask.class).withNoArguments().thenReturn(mPushTokenTask);
    }

    @Test
    public void singleton() {
        Assert.assertSame(Push.getInstance(), Push.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Push.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(PushInstallationLog.TYPE) instanceof PushInstallationLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void getPushToken() {
        Push push = Push.getInstance();
        Push.setSenderId("TEST");
        Channel channel = mock(Channel.class);
        push.onChannelReady(mock(Context.class), channel);
        verify(mPushTokenTask).executeOnExecutor(any(Executor.class));
    }

    @Test
    public void noSenderId() {
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        push.onChannelReady(mock(Context.class), channel);
        verify(mPushTokenTask, never()).executeOnExecutor(any(Executor.class));
    }

    @Test
    public void handlePushToken() {
        String testToken = "TEST";
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        push.onChannelReady(mock(Context.class), channel);
        push.handlePushToken(testToken);
        verifyStatic(times(1));
        StorageHelper.PreferencesStorage.putString(eq(PREFERENCE_KEY_PUSH_TOKEN), eq(testToken));

        // For check enqueue only once
        push.handlePushToken(testToken);
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));
    }

    @Test
    public void setEnabled() {
        Push push = Push.getInstance();
        Push.setSenderId("TEST");
        Channel channel = mock(Channel.class);
        assertTrue(Push.isEnabled());
        Push.setEnabled(true);
        assertTrue(Push.isEnabled());
        Push.setEnabled(false);
        assertFalse(Push.isEnabled());
        push.onChannelReady(mock(Context.class), channel);
        verify(channel).clear(push.getGroupName());
        verify(channel).removeGroup(eq(push.getGroupName()));
        verify(mPushTokenTask, never()).executeOnExecutor(any(Executor.class));

        Push.setEnabled(true);
        verify(mPushTokenTask).executeOnExecutor(any(Executor.class));

        // If disabled when PushTokenTask executing
        Push.setEnabled(false);
        push.handlePushToken("TEST");
        verify(channel, never()).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        Push.setEnabled(true);
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));
    }
}