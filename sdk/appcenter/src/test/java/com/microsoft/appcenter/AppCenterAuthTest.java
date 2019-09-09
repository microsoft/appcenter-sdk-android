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
    private AuthTokenContext.RefreshListener testSetAuthTokenListener() {
        final String jwt = "jwt";
        JwtClaims claims = mock(JwtClaims.class);
        when(claims.getSubject()).thenReturn("someId");
        when(claims.getExpirationDate()).thenReturn(new Date(123L));
        when(JwtClaims.parse(jwt)).thenReturn(claims);
        AppCenter.setAuthTokenListener(new AuthTokenListener() {

            @Override
            public void acquireAuthToken(AuthTokenCallback callback) {
                callback.onAuthTokenResult(jwt);
            }
        });
        ArgumentCaptor<AuthTokenContext.RefreshListener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.RefreshListener.class);
        verify(mAuthTokenContext).setRefreshListener(listenerArgumentCaptor.capture());
        AuthTokenContext.RefreshListener refreshListener = listenerArgumentCaptor.getValue();
        assertNotNull(refreshListener);
        refreshListener.onTokenRequiresRefresh(claims.getSubject());
        verify(mAuthTokenContext).doNotResetAuthAfterStart();
        verify(mAuthTokenContext).setAuthToken(jwt, claims.getSubject(), claims.getExpirationDate());
        return refreshListener;
    }

    @Test
    public void setAuthTokenListener() {
        testSetAuthTokenListener();
    }

    @Test
    public void setAuthTokenListenerWhenPreviouslySet() {
        AuthTokenContext.RefreshListener refreshListener = testSetAuthTokenListener();
        AppCenter.setAuthTokenListener(null);
        verify(mAuthTokenContext).unsetRefreshListener(refreshListener);
        verify(mAuthTokenContext).setAuthToken(null, null, null);
    }

    @Test
    public void setNullAuthTokenListenerWhenNoneExists() {
        AppCenter.setAuthTokenListener(null);
        verify(mAuthTokenContext, never()).unsetRefreshListener(any(AuthTokenContext.RefreshListener.class));
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void setAuthTokenListenerWithNullClaims() {
        final String invalidJwt = "invalid jwt";
        mockStatic(JwtClaims.class);
        when(JwtClaims.parse(invalidJwt)).thenReturn(null);
        AppCenter.setAuthTokenListener(new AuthTokenListener() {

            @Override
            public void acquireAuthToken(AuthTokenCallback callback) {
                callback.onAuthTokenResult(invalidJwt);
            }
        });
        ArgumentCaptor<AuthTokenContext.RefreshListener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.RefreshListener.class);
        verify(mAuthTokenContext).setRefreshListener(listenerArgumentCaptor.capture());
        AuthTokenContext.RefreshListener refreshListener = listenerArgumentCaptor.getValue();
        assertNotNull(refreshListener);
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("some account id");
        verify(mAuthTokenContext).doNotResetAuthAfterStart();
        verify(mAuthTokenContext).setAuthToken(null, null, null);
    }
}
