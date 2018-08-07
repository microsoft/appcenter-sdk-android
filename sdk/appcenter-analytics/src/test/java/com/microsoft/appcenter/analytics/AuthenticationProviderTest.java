package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.utils.TicketCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TicketCache.class})
public class AuthenticationProviderTest {

    @Mock
    private TicketCache mTicketCache;

    @Before
    public void setUp() {
        mockStatic(TicketCache.class);
        when(TicketCache.getInstance()).thenReturn(mTicketCache);
    }

    @Test
    public void ticketKeyHash() {
        assertNull(new AuthenticationProvider(null, null, null).getTicketKeyHash());
        assertNotNull(new AuthenticationProvider(null, "test", null).getTicketKeyHash());
    }

    @Test
    public void acquireTokenAsync() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        doNothing().when(tokenProvider).getToken(anyString(), callback.capture());
        AuthenticationProvider authenticationProvider = new AuthenticationProvider(null, "key", tokenProvider);

        /* Verify acquireTokenAsync. */
        authenticationProvider.acquireTokenAsync();
        verify(tokenProvider).getToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* Verify onAuthenticationResult. */
        callback.getValue().onAuthenticationResult(null, new Date());
        verifyNoMoreInteractions(mTicketCache);
        callback.getValue().onAuthenticationResult("test", null);
        verifyNoMoreInteractions(mTicketCache);
        callback.getValue().onAuthenticationResult("test", new Date());
        verify(mTicketCache).putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("test"));
    }
}
