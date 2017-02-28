package com.microsoft.azure.mobile.push;

import android.content.Context;

import com.google.firebase.iid.FirebaseInstanceId;
import com.microsoft.azure.mobile.MobileCenter;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Map;

import static com.microsoft.azure.mobile.push.Push.PREFERENCE_KEY_PUSH_TOKEN;
import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({
        Push.class,
        PushInstallationLog.class,
        MobileCenterLog.class,
        MobileCenter.class,
        StorageHelper.PreferencesStorage.class,
        FirebaseInstanceId.class
})
public class PushTest {

    private static final String PUSH_ENABLED_KEY = KEY_ENABLED + "_group_push";

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    FirebaseInstanceId mFirebaseInstanceId;

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

        /* Mock Firebase instance. */
        mockStatic(FirebaseInstanceId.class);
        when(FirebaseInstanceId.getInstance()).thenReturn(mFirebaseInstanceId);
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
    public void onTokenRefresh() {
        String testToken = "TEST";
        Push push = Mockito.spy(Push.getInstance());
        push.setInstanceEnabled(false);
        Channel channel = mock(Channel.class);
        push.onChannelReady(mock(Context.class), channel);
        verify(mFirebaseInstanceId, never()).getToken();

        /* When token unavailable */
        when(mFirebaseInstanceId.getToken()).thenReturn(null);
        push.setInstanceEnabled(true);
        verify(mFirebaseInstanceId).getToken();
        verify(push, never()).onTokenRefresh(anyString());

        /* When token available */
        when(mFirebaseInstanceId.getToken()).thenReturn(testToken);
        push.setInstanceEnabled(true);
        verify(push).onTokenRefresh(anyString());
        verifyStatic(times(1));
        StorageHelper.PreferencesStorage.putString(eq(PREFERENCE_KEY_PUSH_TOKEN), eq(testToken));

        /* For check enqueue only once */
        push.onTokenRefresh(testToken);
        verify(channel).enqueue(any(PushInstallationLog.class), anyString());
    }

    @Test
    public void setEnabled() {
        String testToken = "TEST";
        Push push = Push.getInstance();
        Channel channel = mock(Channel.class);
        assertTrue(Push.isEnabled());
        Push.setEnabled(true);
        assertTrue(Push.isEnabled());
        Push.setEnabled(false);
        assertFalse(Push.isEnabled());
        push.onChannelReady(mock(Context.class), channel);
        verify(channel).clear(push.getGroupName());
        verify(channel).removeGroup(eq(push.getGroupName()));
        verify(mFirebaseInstanceId, never()).getToken();

        /* If disabled when PushTokenTask executing */
        Push.setEnabled(false);
        push.onTokenRefresh(testToken);
        verify(channel, never()).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));

        /* For check enqueue only once */
        when(mFirebaseInstanceId.getToken()).thenReturn(testToken);
        Push.setEnabled(true);
        Push.setEnabled(true);
        verify(channel).enqueue(any(PushInstallationLog.class), eq(push.getGroupName()));
    }
}