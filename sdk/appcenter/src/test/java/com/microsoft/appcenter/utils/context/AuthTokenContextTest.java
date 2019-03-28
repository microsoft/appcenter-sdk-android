/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.content.Context;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.PREFERENCE_KEY_TOKEN_HISTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({CryptoUtils.class, JSONUtils.class, SharedPreferencesManager.class})
@RunWith(PowerMockRunner.class)
public class AuthTokenContextTest {

    private final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private AuthTokenContext mAuthTokenContext;

    @Mock
    private CryptoUtils mCryptoUtils;

    @Before
    public void setUp() {
        mockStatic(CryptoUtils.class);
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);
        mockStatic(SharedPreferencesManager.class);
        mAuthTokenContext = AuthTokenContext.getInstance();

        /* JSON library is part of Android SDK and doesn't available here. */
        mockStatic(JSONUtils.class);
    }

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }

    @Test
    public void setAuthTokenTest() {

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = spy(AbstractTokenContextListener.class);

        /* Set new auth token. */
        mAuthTokenContext.addListener(mockListener);
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, "mock-user", mock(Date.class));

        /* Verify that the returned token is the same. */
        assertEquals(mAuthTokenContext.getAuthToken(), AUTH_TOKEN);

        /* Clear token data. */
        mAuthTokenContext.setAuthToken(null, null, null);

        /* Verify that listener is called on empty token. */
        verify(mockListener, times(1)).onNewAuthToken(isNull(String.class));
        verify(mockListener, times(1)).onNewUser(isNull(String.class));
        assertNull(mAuthTokenContext.getAuthToken());

        /* Remove listener. */
        mAuthTokenContext.removeListener(mockListener);

        /* Update token without listener attached. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, "mock-user", mock(Date.class));

        /* Verify that listener is called only once on a new token (i.e. before we removed listener). */
        verify(mockListener, times(1)).onNewAuthToken(AUTH_TOKEN);
    }

    @Test
    public void getTokenHistoryEmptyJson() {
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn("");
        when(mCryptoUtils.decrypt(eq("empty"), eq(false))).thenReturn(decryptedData);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("empty");
        assertNull(mAuthTokenContext.getHistory());
    }
}
