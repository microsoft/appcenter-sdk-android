package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.JwtClaims;
import com.microsoft.appcenter.utils.context.AuthTokenContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Date;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
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
        mockStatic(JwtClaims.class);
        mockStatic(AuthTokenContext.class);
        when(getInstance()).thenReturn(mAuthTokenContext);
    }

    @Test
    public void setAuthProvider() {
        final String jwt = "jwt";
        JwtClaims claims = new JwtClaims("subject", new Date(123));
        when(JwtClaims.parse(jwt)).thenReturn(claims);
        AppCenter.setAuthProvider(new AuthProvider() {

            @Override
            public void acquireToken(Callback callback) {
                callback.onAuthResult(jwt);
            }
        });
        ArgumentCaptor<Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(Listener.class);
        verify(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());
        assertNotNull(listenerArgumentCaptor.getValue());
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh(claims.getSubject());
        verify(mAuthTokenContext).doNotResetAuthAfterStart();
        verify(mAuthTokenContext).setAuthToken(jwt, claims.getSubject(), claims.getExpirationDate());
    }

    @Test
    public void setNullAuthProviderWhenNoneExists() {
        AppCenter.setAuthProvider(null);
        verify(mAuthTokenContext, never()).removeListener(any(Listener.class));
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));
    }

    @Test
    public void setAuthProviderWhenPreviouslySet() {
        setAuthProvider();
        AppCenter.setAuthProvider(null);
        verify(mAuthTokenContext).removeListener(notNull(Listener.class));
        verify(mAuthTokenContext).setAuthToken(null, null, null);
    }

    @Test
    public void setAuthProviderWithNullClaims() {
        final String invalidJwt = "invalid jwt";
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
