/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Date;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_AUTH_TOKEN;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_HOME_ACCOUNT_ID;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_TOKEN_HISTORY;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest({SharedPreferencesManager.class, CryptoUtils.class})
@RunWith(PowerMockRunner.class)
public class TokenStorageTest {

    private static final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ENCRYPTED_AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ACCOUNT_ID = UUIDUtils.randomUUID().toString();

    @Mock
    private CryptoUtils mCryptoUtils;

    private AuthTokenStorage mTokenStorage;

    @Before
    public void setUp() {
        mockStatic(SharedPreferencesManager.class);
        mockStatic(CryptoUtils.class);
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);

        /* Mock token. */
        mTokenStorage = TokenStorageFactory.getTokenStorage(mock(Context.class));
    }

    @Test
    public void testSave() {

        /* Save the token into storage. */
        when(mCryptoUtils.encrypt(eq(AUTH_TOKEN))).thenReturn(ENCRYPTED_AUTH_TOKEN);
        mTokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, mock(Date.class));
        verify(mCryptoUtils).encrypt(AUTH_TOKEN);
        mCryptoUtils.encrypt(AUTH_TOKEN);

        /* Verify save called on context and preferences. */
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_AUTH_TOKEN), eq(ENCRYPTED_AUTH_TOKEN));
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID), eq(ACCOUNT_ID));
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_TOKEN_HISTORY), anyString());
    }

    @Test
    public void testRemove() {
        when(mCryptoUtils.encrypt(eq(AUTH_TOKEN))).thenReturn(ENCRYPTED_AUTH_TOKEN);

        /* Remove the token from storage. */
        mTokenStorage.removeToken(AUTH_TOKEN);

        /* Verify remove called on context and preferences. */
        verify(mCryptoUtils).encrypt(AUTH_TOKEN);
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_AUTH_TOKEN));
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID));

    }

    @Test
    public void testGet() {

        /* Mock preferences and crypto calls. */
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(AUTH_TOKEN);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID), isNull(String.class))).thenReturn(ACCOUNT_ID);
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn(AUTH_TOKEN);
        when(mCryptoUtils.decrypt(eq(AUTH_TOKEN), eq(false))).thenReturn(decryptedData);

        /* Verify the right token is returned. */
        assertEquals(AUTH_TOKEN, mTokenStorage.getToken());
    }

    @Test
    public void testGetFailsWithNull() {
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(null);
        assertNull(mTokenStorage.getToken());
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn("");
        assertNull(mTokenStorage.getToken());
    }
}

