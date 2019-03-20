/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_AUTH_TOKEN;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_HOME_ACCOUNT_ID;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_TOKEN_HISTORY;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({SharedPreferencesManager.class, CryptoUtils.class})
@RunWith(PowerMockRunner.class)
public class PreferenceTokenStorageTest {

    private static final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ENCRYPTED_AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ACCOUNT_ID = UUIDUtils.randomUUID().toString();

    @Mock
    private CryptoUtils mCryptoUtils;

    private PreferenceTokenStorage mTokenStorage;

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(CryptoUtils.class);
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);

        /* Mock token. */
        mTokenStorage = new PreferenceTokenStorage(mock(Context.class));
    }

    @Test
    public void factoryCreatesPreferenceTokenStorage() {
        AuthTokenStorage tokenStorage = TokenStorageFactory.getTokenStorage(mock(Context.class));
        assertThat(tokenStorage, instanceOf(PreferenceTokenStorage.class));
        assertEquals(tokenStorage, TokenStorageFactory.getTokenStorage(mock(Context.class)));
    }

    @Test
    public void getCurrentToken() {

        /* Mock preferences and crypto calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(AUTH_TOKEN);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID), isNull(String.class))).thenReturn(ACCOUNT_ID);
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn(AUTH_TOKEN);
        when(mCryptoUtils.decrypt(eq(AUTH_TOKEN), eq(false))).thenReturn(decryptedData);

        /* Verify the right token is returned. */
        assertEquals(AUTH_TOKEN, mTokenStorage.getToken());

        /* Empty values. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(null);
        assertNull(mTokenStorage.getToken());
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn("");
        assertNull(mTokenStorage.getToken());
    }

    @Test
    public void loadTokenHistory() {

        /* History is null. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn(null);
        assertNull(mTokenStorage.loadTokenHistory());

        /* History is invalid. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("some bad json");
        assertEquals(0, mTokenStorage.loadTokenHistory().size());

        /* History has one token. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("[{\"token\":null,\"time\":null,\"expiresOn\":null}]");
        assertEquals(1, mTokenStorage.loadTokenHistory().size());
    }

    @Test
    public void getOldestToken() {

        /* History is null. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn(null);
        AuthTokenInfo authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History is invalid. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History has one null token. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class)))
                .thenReturn("[{\"token\":null,\"time\":null,\"expiresOn\":null}]");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History has one empty token. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class)))
                .thenReturn("[{\"token\":\"\",\"time\":null,\"expiresOn\":null}]");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertEquals("", authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History has one token. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class)))
                .thenReturn("[{\"token\":\"" + ENCRYPTED_AUTH_TOKEN + "\",\"time\":null,\"expiresOn\":null}]");
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn(AUTH_TOKEN);
        when(mCryptoUtils.decrypt(eq(ENCRYPTED_AUTH_TOKEN), eq(false))).thenReturn(decryptedData);
        authTokenInfo = mTokenStorage.getOldestToken();
        assertEquals(AUTH_TOKEN, authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());
    }
}

