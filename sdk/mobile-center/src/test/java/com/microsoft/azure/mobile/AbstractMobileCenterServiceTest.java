package com.microsoft.azure.mobile;

import android.content.Context;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageHelper.PreferencesStorage.class, MobileCenter.class})
public class AbstractMobileCenterServiceTest {

    private static final String SERVICE_ENABLED_KEY = KEY_ENABLED + "_Test";

    private AbstractMobileCenterService mService;

    @Before
    public void setUp() throws Exception {
        mService = new AbstractMobileCenterService() {

            @Override
            protected String getGroupName() {
                return "group_test";
            }

            @Override
            public String getServiceName() {
                return "Test";
            }

            @Override
            protected String getLoggerTag() {
                return "TestLog";
            }
        };
        mockStatic(MobileCenter.class);

        /* First call to com.microsoft.azure.mobile.MobileCenter.isEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(SERVICE_ENABLED_KEY, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(SERVICE_ENABLED_KEY, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(SERVICE_ENABLED_KEY), anyBoolean());
    }

    @Test
    public void onActivityCreated() {
        mService.onActivityCreated(null, null);
    }

    @Test
    public void onActivityStarted() {
        mService.onActivityStarted(null);
    }

    @Test
    public void onActivityResumed() {
        mService.onActivityResumed(null);
    }

    @Test
    public void onActivityPaused() {
        mService.onActivityPaused(null);
    }

    @Test
    public void onActivityStopped() {
        mService.onActivityStopped(null);
    }

    @Test
    public void onActivitySaveInstanceState() {
        mService.onActivitySaveInstanceState(null, null);
    }

    @Test
    public void onActivityDestroyed() {
        mService.onActivityDestroyed(null);
    }

    @Test
    public void setEnabledIfCoreEnabled() {
        MobileCenterHandler mobileCenterHandler = mock(MobileCenterHandler.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ((Runnable) invocation.getArguments()[0]).run();
                return null;
            }
        }).when(mobileCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mService.onStarting(mobileCenterHandler);
        mService.onStarted(mock(Context.class), "", mock(Channel.class));
        assertTrue(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(true);
        mService.setInstanceEnabledAsync(false);
        assertFalse(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(false);
        mService.setInstanceEnabledAsync(true);
        assertTrue(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(true);
        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(mService.getEnabledPreferenceKey(), false);
        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(mService.getEnabledPreferenceKey(), true);
    }

    @Test
    public void setEnabledIfCoreDisabled() {
        MobileCenterHandler mobileCenterHandler = mock(MobileCenterHandler.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object disabledRunnable = invocation.getArguments()[1];
                if (disabledRunnable instanceof Runnable) {
                    ((Runnable) disabledRunnable).run();
                }
                return null;
            }
        }).when(mobileCenterHandler).post(any(Runnable.class), any(Runnable.class));
        mService.onStarting(mobileCenterHandler);
        mService.onStarted(mock(Context.class), "", mock(Channel.class));
        assertFalse(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(true);
        assertFalse(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(false);
        assertFalse(mService.isInstanceEnabledAsync().get());
        mService.setInstanceEnabledAsync(true);
        assertFalse(mService.isInstanceEnabledAsync().get());
        verifyStatic(never());
        StorageHelper.PreferencesStorage.putBoolean(eq(mService.getEnabledPreferenceKey()), anyBoolean());
    }

    @Test
    public void getLogFactories() {
        Assert.assertNull(null, mService.getLogFactories());
    }

    @Test
    public void onChannelReadyEnabledThenDisable() {
        Channel channel = mock(Channel.class);
        mService.onStarted(mock(Context.class), "", channel);
        verify(channel).removeGroup(mService.getGroupName());
        verify(channel).addGroup(mService.getGroupName(), mService.getTriggerCount(), mService.getTriggerInterval(), mService.getTriggerMaxParallelRequests(), mService.getChannelListener());
        verifyNoMoreInteractions(channel);
        Assert.assertSame(channel, mService.mChannel);

        mService.setInstanceEnabled(false);
        verify(channel, times(2)).removeGroup(mService.getGroupName());
        verify(channel).clear(mService.getGroupName());
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void onChannelReadyDisabledThenEnable() {
        Channel channel = mock(Channel.class);
        mService.onStarted(mock(Context.class), "", channel);
        verify(channel).removeGroup(mService.getGroupName());
        verify(channel).addGroup(eq(mService.getGroupName()), anyInt(), anyLong(), anyInt(), any(Channel.GroupListener.class));
        mService.setInstanceEnabled(false);
        verify(channel, times(2)).removeGroup(mService.getGroupName());
        verify(channel).clear(mService.getGroupName());
        verifyNoMoreInteractions(channel);
        Assert.assertSame(channel, mService.mChannel);
        mService.setInstanceEnabled(true);
        verify(channel, times(2)).addGroup(mService.getGroupName(), mService.getTriggerCount(), mService.getTriggerInterval(), mService.getTriggerMaxParallelRequests(), mService.getChannelListener());
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void getGroupName() {
        Assert.assertEquals("group_test", mService.getGroupName());
    }

    @Test
    public void optionalGroup() {
        mService = new AbstractMobileCenterService() {

            @Override
            protected String getGroupName() {
                return null;
            }

            @Override
            public String getServiceName() {
                return "Test";
            }

            @Override
            protected String getLoggerTag() {
                return "TestLog";
            }
        };
        Channel channel = mock(Channel.class);
        mService.onStarted(mock(Context.class), "", channel);
        mService.setInstanceEnabled(false);
        mService.setInstanceEnabled(true);
        verifyZeroInteractions(channel);
    }
}
