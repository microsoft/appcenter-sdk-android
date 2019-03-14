/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA_COMPACT;
import static com.microsoft.appcenter.analytics.AuthenticationProvider.Type.MSA_DELEGATE;
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
        assertEquals(MSA_COMPACT, AuthenticationProvider.Type.valueOf(MSA_COMPACT.name()));
    }

    @Test
    public void ticketKeyHash() {
        assertNull(new AuthenticationProvider(null, null, null).getTicketKeyHash());
        assertNotNull(new AuthenticationProvider(null, "test", null).getTicketKeyHash());
    }

    @Test
    public void acquireTokenAsyncInvalidToken() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        doNothing().when(tokenProvider).acquireToken(anyString(), callback.capture());
        AuthenticationProvider authenticationProvider = new AuthenticationProvider(MSA_COMPACT, "key", tokenProvider);

        /* Verify acquireTokenAsync. */
        authenticationProvider.acquireTokenAsync();
        verify(tokenProvider).acquireToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* When callback parameters are invalid, don't update cache. */
        callback.getValue().onAuthenticationResult(null, new Date());
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());

        /* Ignore calling callback more than once, even if parameters are valid the second time. */
        long freshDate = System.currentTimeMillis() + 15 * 60 * 1000;
        callback.getValue().onAuthenticationResult("test", new Date(freshDate));
        verifyStatic(never());
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("p:test"));
    }

    @Test
    public void acquireTokenAsyncInvalidDate() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        doNothing().when(tokenProvider).acquireToken(anyString(), callback.capture());
        AuthenticationProvider authenticationProvider = new AuthenticationProvider(MSA_COMPACT, "key", tokenProvider);

        /* Verify acquireTokenAsync. */
        authenticationProvider.acquireTokenAsync();
        verify(tokenProvider).acquireToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* When callback parameters are invalid, don't update cache. */
        callback.getValue().onAuthenticationResult("test", null);
        verifyStatic(never());
        TicketCache.putTicket(anyString(), anyString());

        /* Ignore calling callback more than once, even if parameters are valid the second time. */
        long freshDate = System.currentTimeMillis() + 15 * 60 * 1000;
        callback.getValue().onAuthenticationResult("test", new Date(freshDate));
        verifyStatic(never());
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("p:test"));
    }

    @Test
    public void acquireTokenAsyncValid() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        doNothing().when(tokenProvider).acquireToken(anyString(), callback.capture());
        AuthenticationProvider authenticationProvider = new AuthenticationProvider(MSA_COMPACT, "key", tokenProvider);

        /* Verify acquireTokenAsync. */
        authenticationProvider.acquireTokenAsync();
        verify(tokenProvider).acquireToken(eq("key"), any(AuthenticationProvider.AuthenticationCallback.class));

        /* When callback parameters are valid update cache. */
        long freshDate = System.currentTimeMillis() + 15 * 60 * 1000;
        callback.getValue().onAuthenticationResult("test", new Date(freshDate));
        verifyStatic();
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("p:test"));

        /* Duplicate calls are ignored. */
        callback.getValue().onAuthenticationResult("test2", new Date(freshDate));
        verifyStatic(never());
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("p:test2"));
    }

    @Test
    public void refreshToken() {
        AuthenticationProvider.TokenProvider tokenProvider = mock(AuthenticationProvider.TokenProvider.class);
        ArgumentCaptor<AuthenticationProvider.AuthenticationCallback> callback = ArgumentCaptor.forClass(AuthenticationProvider.AuthenticationCallback.class);
        AuthenticationProvider authenticationProvider = spy(new AuthenticationProvider(MSA_DELEGATE, "key", tokenProvider));

        /* When no token, then refresh does nothing. */
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* When acquired a fresh token. */
        authenticationProvider.acquireTokenAsync();
        Date expiryDate = mock(Date.class);
        when(expiryDate.getTime()).thenReturn(System.currentTimeMillis() + 15 * 60 * 1000);
        verify(tokenProvider).acquireToken(anyString(), callback.capture());
        callback.getValue().onAuthenticationResult("test", expiryDate);
        verifyStatic();
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("d:test"));

        /* Then refresh does nothing. */
        reset(authenticationProvider);
        reset(tokenProvider);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider, never()).acquireTokenAsync();

        /* When token expiring soon, then it tries to acquire a new token. */
        when(expiryDate.getTime()).thenReturn(System.currentTimeMillis() + 9 * 60 * 1000);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider).acquireTokenAsync();
        verify(tokenProvider).acquireToken(anyString(), callback.capture());

        /* When checking a second time if expired before token provider called back. */
        reset(authenticationProvider);
        reset(tokenProvider);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider).acquireTokenAsync();

        /* Then we don't call back again. */
        verify(tokenProvider, never()).acquireToken(anyString(), callback.capture());

        /* Call back after refresh. */
        callback.getValue().onAuthenticationResult("test", expiryDate);

        /* Verify cache updated. */
        verifyStatic(times(2));
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("d:test"));

        /* Now that called back, we can refresh again. */
        reset(authenticationProvider);
        reset(tokenProvider);
        authenticationProvider.checkTokenExpiry();
        verify(authenticationProvider).acquireTokenAsync();
        verify(tokenProvider).acquireToken(anyString(), callback.capture());
        callback.getValue().onAuthenticationResult("test", expiryDate);
        verifyStatic(times(3));
        TicketCache.putTicket(eq(authenticationProvider.getTicketKeyHash()), eq("d:test"));
    }
}
