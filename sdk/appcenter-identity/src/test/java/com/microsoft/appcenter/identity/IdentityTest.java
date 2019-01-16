package com.microsoft.appcenter.identity;

import android.content.Context;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.UserIdContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

public class IdentityTest extends AbstractIdentityTest {

    @After
    public void resetUserId() {
        UserIdContext.unsetInstance();
    }

    @Test
    public void singleton() {
        Assert.assertSame(Identity.getInstance(), Identity.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertFalse(Identity.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Identity.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void setEnabled() {

        /* Before start it does not work to change state, it's disabled. */
        Identity analytics = Identity.getInstance();
        Identity.setEnabled(true);
        assertFalse(Identity.isEnabled().get());
        Identity.setEnabled(false);
        assertFalse(Identity.isEnabled().get());

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel).removeGroup(eq(analytics.getGroupName()));
        verify(channel).addGroup(eq(analytics.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Identity.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Identity.setEnabled(false).get();
        assertFalse(Identity.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(IDENTITY_ENABLED_KEY, true)).thenReturn(false);
        Identity analytics = Identity.getInstance();

        /* Start. */
        Channel channel = mock(Channel.class);
        analytics.onStarting(mAppCenterHandler);
        analytics.onStarted(mock(Context.class), channel, "", null, true);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }
}
