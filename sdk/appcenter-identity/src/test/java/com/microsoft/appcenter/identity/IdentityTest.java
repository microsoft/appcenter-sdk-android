package com.microsoft.appcenter.identity;

import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class IdentityTest extends AbstractIdentityTest {

    @NonNull
    private Channel start(Identity identity) {
        Channel channel = mock(Channel.class);
        identity.onStarting(mAppCenterHandler);
        identity.onStarted(mock(Context.class), channel, "", null, true);
        return channel;
    }

    @Test
    public void singleton() {
        Assert.assertSame(Identity.getInstance(), Identity.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Identity.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Identity.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {

        /* Before start it does not work to change state, it's disabled. */
        Identity identity = Identity.getInstance();
        Identity.setEnabled(true);
        assertFalse(Identity.isEnabled().get());
        Identity.setEnabled(false);
        assertFalse(Identity.isEnabled().get());

        /* Start. */
        Channel channel = start(identity);
        verify(channel).removeGroup(eq(identity.getGroupName()));
        verify(channel).addGroup(eq(identity.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Identity.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Identity.setEnabled(false).get();
        assertFalse(Identity.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(IDENTITY_ENABLED_KEY, true)).thenReturn(false);
        Identity identity = Identity.getInstance();

        /* Start. */
        Channel channel = start(identity);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void downloadFullInvalidConfiguration() throws Exception {

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        start(Identity.getInstance());

        /* When we get an invalid payload. */
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallSucceeded("invalid", new HashMap<String, String>());

        /* We didn't attempt to even save. */
        verifyStatic();
        FileManager.write(any(File.class), anyString());
    }
}
