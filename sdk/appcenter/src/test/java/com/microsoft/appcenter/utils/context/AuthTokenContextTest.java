package com.microsoft.appcenter.utils.context;

import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AuthTokenContextTest {

    private AuthTokenContext mAuthTokenContext;
    private final String MOCK_TOKEN = UUIDUtils.randomUUID().toString();

    @Before
    public void setUp() {
        TokenStorage mockTokenStorage = mock(TokenStorage.class);
        when(mockTokenStorage.getToken()).thenReturn(MOCK_TOKEN);
        mAuthTokenContext = AuthTokenContext.getInstance();
        mAuthTokenContext.setTokenStorage(mockTokenStorage);
    }


    @Test
    public void setAuthTokenTest() {

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = mock(AuthTokenContext.Listener.class);

        /* Set new auth token. */
        mAuthTokenContext.addListener(mockListener);
        mAuthTokenContext.setAuthToken(MOCK_TOKEN);

        /* Verify that the returned token is the same. */
        assertEquals(mAuthTokenContext.getAuthToken(), MOCK_TOKEN);

        /* Verify that listener is called on a new token. */
        verify(mockListener).onNewToken(MOCK_TOKEN);
    }

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }
}
