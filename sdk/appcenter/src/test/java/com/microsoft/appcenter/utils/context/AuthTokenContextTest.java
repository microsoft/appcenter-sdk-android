/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import com.microsoft.appcenter.utils.UUIDUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
public class AuthTokenContextTest {

    private AuthTokenContext mAuthTokenContext;

    private final String MOCK_TOKEN = UUIDUtils.randomUUID().toString();

    @Before
    public void setUp() {
        mAuthTokenContext = AuthTokenContext.getInstance();
    }

    @Test
    public void saveAuthTokenAndTimeToStorage(){
    	for (int i = 0; i<10;i++){
			mAuthTokenContext.addTokenToList(MOCK_TOKEN,UUIDUtils.randomUUID().toString());
		}
        assertEquals(mAuthTokenContext.getTokenAndTimePairList().size(),5);
    }

    @Test
    public void setAuthTokenTest() {

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = mock(AuthTokenContext.Listener.class);

        /* Set new auth token. */
        mAuthTokenContext.addListener(mockListener);
        mAuthTokenContext.setAuthToken(MOCK_TOKEN, "mock-user");

        /* Verify that the returned token is the same. */
        assertEquals(mAuthTokenContext.getAuthToken(), MOCK_TOKEN);

        /* Clear token data. */
        mAuthTokenContext.clearToken();

        /* Verify that listener is called on empty token. */
        verify(mockListener, times(1)).onNewAuthToken(isNull(String.class));
        verify(mockListener, times(1)).onNewUser(isNull(String.class));
        assertNull(mAuthTokenContext.getAuthToken());

        /* Remove listener. */
        mAuthTokenContext.removeListener(mockListener);

        /* Update token without listener attached. */
        mAuthTokenContext.setAuthToken(MOCK_TOKEN, "mock-user");

        /* Verify that listener is called only once on a new token (i.e. before we removed listener). */
        verify(mockListener, times(1)).onNewAuthToken(MOCK_TOKEN);
    }

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }
}
