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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_TOKEN_HISTORY;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@PrepareForTest({SharedPreferencesManager.class, CryptoUtils.class})
@RunWith(PowerMockRunner.class)
public class PreferenceTokenStorageTest {

    private static final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

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
    public void getTokenHistory() {
        when(mCryptoUtils.decrypt(anyString(), eq(false))).thenAnswer(new Answer<CryptoUtils.DecryptedData>() {

            @Override
            public CryptoUtils.DecryptedData answer(InvocationOnMock invocation) {
                CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
                when(decryptedData.getDecryptedData()).thenReturn((String) invocation.getArguments()[0]);
                return decryptedData;
            }
        });

        /* History is null. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn(null);
        assertNull(mTokenStorage.getHistory());

        /* History is invalid. */
        mTokenStorage.setHistory(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("some bad json");
        assertEquals(0, mTokenStorage.getHistory().size());

        /* History has one token. */
        mTokenStorage.setHistory(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("[{\"token\":null,\"time\":null,\"expiresOn\":null}]");
        assertEquals(1, mTokenStorage.getHistory().size());
    }

    @Test
    public void getTokenHistoryEmptyJson() {
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn("");
        when(mCryptoUtils.decrypt(eq("empty"), eq(false))).thenReturn(decryptedData);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("empty");
        assertNull(mTokenStorage.getHistory());
    }

    @Test
    public void getOldestToken() {
        when(mCryptoUtils.decrypt(anyString(), eq(false))).thenAnswer(new Answer<CryptoUtils.DecryptedData>() {

            @Override
            public CryptoUtils.DecryptedData answer(InvocationOnMock invocation) {
                CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
                when(decryptedData.getDecryptedData()).thenReturn((String) invocation.getArguments()[0]);
                return decryptedData;
            }
        });

        /* History is null. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn(null);
        AuthTokenInfo authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History is invalid. */
        mTokenStorage.setHistory(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History has one null token. */
        mTokenStorage.setHistory(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class)))
                .thenReturn("[{\"token\":null,\"time\":null,\"expiresOn\":null}]");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History has one token. */
        mTokenStorage.setHistory(null);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class)))
                .thenReturn("[{\"token\":\"" + AUTH_TOKEN + "\",\"time\":null,\"expiresOn\":null}]");
        authTokenInfo = mTokenStorage.getOldestToken();
        assertEquals(AUTH_TOKEN, authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());
    }
}

