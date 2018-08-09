package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.TicketCache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TicketCache.class, HandlerUtils.class})
public class AuthenticationProviderTest {

    @Before
    public void setUp() {
        mockStatic(TicketCache.class);
    }

    @Test
    public void typeEnumTest() {

        /* Work around for code coverage. */
        assertEquals(MSA, AuthenticationProvider.Type.valueOf(MSA.name()));
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

        /* When callback parameters are invalid, don't update cache. */
        callback.getValue().onAuthenticationResult(null, new Date());
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());
        callback.getValue().onAuthenticationResult("test", null);
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());

        /* When callback parameters are valid update cache. */
        long freshDate = System.currentTimeMillis() + 15 * 60 * 1000;
        callback.getValue().onAuthenticationResult("test", new Date(freshDate));
        verifyStatic();
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("test"));
    }

    @Test
    public void refreshToken() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        AuthenticationProvider authenticationProvider = spy(new AuthenticationProvider(null, "key", tokenProvider));

        /* When no token, then refresh does nothing. */
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* When acquired a fresh token. */
        authenticationProvider.acquireTokenAsync();
        Date expiresAt = mock(Date.class);
        when(expiresAt.getTime()).thenReturn(System.currentTimeMillis() + 15 * 60 * 1000);
        verify(tokenProvider).getToken(anyString(), callback.capture());
        callback.getValue().onAuthenticationResult("test", expiresAt);
        verifyStatic();
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("test"));

        /* Then refresh does nothing. */
        reset(authenticationProvider);
        reset(tokenProvider);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* When token expiring soon, then it tries to acquire a new token. */
        when(expiresAt.getTime()).thenReturn(System.currentTimeMillis() + 9 * 60 * 1000);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider).acquireTokenAsync();
        verify(tokenProvider).getToken(anyString(), callback.capture());

        /* When checking a second time if expired before token provider called back. */
        reset(authenticationProvider);
        reset(tokenProvider);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider).acquireTokenAsync();

        /* Then we don't call back again. */
        verify(tokenProvider, never()).getToken(anyString(), callback.capture());

        /* Call back after refresh. */
        callback.getValue().onAuthenticationResult("test", expiresAt);

        /* Verify cache updated. */
        verifyStatic(times(2));
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("test"));
    }
}
