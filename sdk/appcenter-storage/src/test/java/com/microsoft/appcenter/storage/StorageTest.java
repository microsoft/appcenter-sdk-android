package com.microsoft.appcenter.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.Map;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class StorageTest extends AbstractStorageTest {

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @NonNull
    private Channel start(Storage storage) {
        Channel channel = mock(Channel.class);
        storage.onStarting(mAppCenterHandler);
        storage.onStarted(mock(Context.class), channel, "", null, true);
        return channel;
    }

    @Test
    public void singleton() {
        Assert.assertSame(Storage.getInstance(), Storage.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Storage.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Storage.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {

        /* Before start it does not work to change state, it's disabled. */
        Storage storage = Storage.getInstance();
        Storage.setEnabled(true);
        assertFalse(Storage.isEnabled().get());
        Storage.setEnabled(false);
        assertFalse(Storage.isEnabled().get());

        /* Start. */
        Channel channel = start(storage);
        verify(channel).removeGroup(eq(storage.getGroupName()));
        verify(channel).addGroup(eq(storage.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Storage.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Storage.setEnabled(false).get();
        assertFalse(Storage.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(STORAGE_ENABLED_KEY, true)).thenReturn(false);
        Storage storage = Storage.getInstance();

        /* Start. */
        Channel channel = start(storage);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }
}
