/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.utils.JwtClaims;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Date;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.Listener;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({JwtClaims.class, AuthTokenContext.class})
public class AppCenterAuthTest extends AbstractAppCenterTest {

    @Mock
    AuthTokenContext mAuthTokenContext;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockStatic(AuthTokenContext.class);
        mockStatic(JwtClaims.class);
        when(AuthTokenContext.getInstance()).thenReturn(mAuthTokenContext);
    }

    @NonNull
    private Listener testSetAuthProvider() {
        final String jwt = "jwt";
        JwtClaims claims = mock(JwtClaims.class);
        when(claims.getSubject()).thenReturn("someId");
        when(claims.getExpirationDate()).thenReturn(new Date(123L));
        when(JwtClaims.parse(jwt)).thenReturn(claims);
        AppCenter.setAuthProvider(new AuthProvider() {

            @Override
            public void acquireToken(Callback callback) {
                callback.onAuthResult(jwt);
            }
        });
        ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());
        Listener listener = listenerArgumentCaptor.getValue();
        assertNotNull(listener);
        listener.onTokenRequiresRefresh(claims.getSubject());
        verify(mAuthTokenContext).doNotResetAuthAfterStart();
        verify(mAuthTokenContext).setAuthToken(jwt, claims.getSubject(), claims.getExpirationDate());
        return listener;
    }

    @Test
    public void setAuthProvider() {
        testSetAuthProvider();
    }

    @Test
    public void setAuthProviderWhenPreviouslySet() {
        Listener listener = testSetAuthProvider();
        AppCenter.setAuthProvider(null);
        verify(mAuthTokenContext).removeListener(listener);
        verify(mAuthTokenContext).setAuthToken(null, null, null);
    }

    @Test
    public void setNullAuthProviderWhenNoneExists() {
        AppCenter.setAuthProvider(null);
        verify(mAuthTokenContext, never()).removeListener(any(Listener.class));
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void setAuthProviderWithNullClaims() {
        final String invalidJwt = "invalid jwt";
        mockStatic(JwtClaims.class);
        when(JwtClaims.parse(invalidJwt)).thenReturn(null);
        AppCenter.setAuthProvider(new AuthProvider() {

            @Override
            public void acquireToken(Callback callback) {
                callback.onAuthResult(invalidJwt);
            }
        });
        ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());
        assertNotNull(listenerArgumentCaptor.getValue());
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("some account id");
        verify(mAuthTokenContext).doNotResetAuthAfterStart();
        verify(mAuthTokenContext).setAuthToken(null, null, null);
    }
}
