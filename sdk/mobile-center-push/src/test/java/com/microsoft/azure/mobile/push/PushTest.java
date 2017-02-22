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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.concurrent.Executor;

import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest({
        Push.class,
        Push.PushTokenTask.class,
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

        mockStatic(StorageHelper.PreferencesStorage.class);
        when(StorageHelper.PreferencesStorage.getBoolean(PUSH_ENABLED_KEY, true)).thenReturn(true);

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
}