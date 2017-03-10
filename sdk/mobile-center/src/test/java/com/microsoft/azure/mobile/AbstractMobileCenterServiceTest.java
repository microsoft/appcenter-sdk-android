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
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({StorageHelper.PreferencesStorage.class, MobileCenter.class})
public class AbstractMobileCenterServiceTest {

    private static final String SERVICE_ENABLED_KEY = KEY_ENABLED + "_Test";

    private AbstractMobileCenterService service;

    @Before
    public void setUp() {
        service = new AbstractMobileCenterService() {
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
        when(MobileCenter.isEnabled()).thenReturn(true);

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
        service.onActivityCreated(null, null);
    }

    @Test
    public void onActivityStarted() {
        service.onActivityStarted(null);
    }

    @Test
    public void onActivityResumed() {
        service.onActivityResumed(null);
    }

    @Test
    public void onActivityPaused() {
        service.onActivityPaused(null);
    }

    @Test
    public void onActivityStopped() {
        service.onActivityStopped(null);
    }

    @Test
    public void onActivitySaveInstanceState() {
        service.onActivitySaveInstanceState(null, null);
    }

    @Test
    public void onActivityDestroyed() {
        service.onActivityDestroyed(null);
    }

    @Test
    public void setEnabled() {
        assertTrue(service.isInstanceEnabled());
        service.setInstanceEnabled(true);
        service.setInstanceEnabled(false);
        assertFalse(service.isInstanceEnabled());
        service.setInstanceEnabled(false);
        service.setInstanceEnabled(true);
        assertTrue(service.isInstanceEnabled());
        service.setInstanceEnabled(true);
        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(service.getEnabledPreferenceKey(), false);
        verifyStatic();
        StorageHelper.PreferencesStorage.putBoolean(service.getEnabledPreferenceKey(), true);
    }

    @Test
    public void getLogFactories() {
        Assert.assertNull(null, service.getLogFactories());
    }

    @Test
    public void onChannelReadyEnabledThenDisable() {
        Channel channel = mock(Channel.class);
        service.onStarted(mock(Context.class), "", channel);
        verify(channel).removeGroup(service.getGroupName());
        verify(channel).addGroup(service.getGroupName(), service.getTriggerCount(), service.getTriggerInterval(), service.getTriggerMaxParallelRequests(), service.getChannelListener());
        verifyNoMoreInteractions(channel);
        Assert.assertSame(channel, service.mChannel);

        service.setInstanceEnabled(false);
        verify(channel, times(2)).removeGroup(service.getGroupName());
        verify(channel).clear(service.getGroupName());
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void onChannelReadyDisabledThenEnable() {
        Channel channel = mock(Channel.class);
        service.setInstanceEnabled(false);
        service.onStarted(mock(Context.class), "", channel);
        verify(channel).removeGroup(service.getGroupName());
        verify(channel).clear(service.getGroupName());
        verifyNoMoreInteractions(channel);
        Assert.assertSame(channel, service.mChannel);

        service.setInstanceEnabled(true);
        verify(channel).addGroup(service.getGroupName(), service.getTriggerCount(), service.getTriggerInterval(), service.getTriggerMaxParallelRequests(), service.getChannelListener());
        verifyNoMoreInteractions(channel);
    }

    @Test
    public void getGroupName() {
        Assert.assertEquals("group_test", service.getGroupName());
    }

    @Test
    public void optionalGroup() {
        service = new AbstractMobileCenterService() {

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
        service.onStarted(mock(Context.class), "", channel);
        service.setInstanceEnabled(false);
        service.setInstanceEnabled(true);
        verifyZeroInteractions(channel);
    }
}
