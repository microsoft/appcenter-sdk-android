package com.microsoft.appcenter.utils.context;

import android.content.Context;

import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({
        TokenStorageFactory.class
})
@RunWith(PowerMockRunner.class)
public class AuthTokenContextTest {

    private AuthTokenContext mAuthTokenContext;
    
    private final String MOCK_TOKEN = UUIDUtils.randomUUID().toString();

    @Before
    public void setUp() {
        mockStatic(TokenStorageFactory.class);
        AuthTokenStorage mockTokenStorage = mock(AuthTokenStorage.class);
        when(mockTokenStorage.getToken()).thenReturn(MOCK_TOKEN);
        when(TokenStorageFactory.getTokenStorage(any(Context.class))).thenReturn(mockTokenStorage);
        mAuthTokenContext = AuthTokenContext.getInstance(mock(Context.class));
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
        verify(mockListener).onNewAuthToken(MOCK_TOKEN);
    }

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }
}
