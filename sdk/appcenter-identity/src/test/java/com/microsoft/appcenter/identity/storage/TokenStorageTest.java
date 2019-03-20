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

	@Mock
	private CryptoUtils mCryptoUtils;

	private AuthTokenStorage mTokenStorage;

	private String mMockToken = UUIDUtils.randomUUID().toString();

	private String mMockEncryptedToken = UUIDUtils.randomUUID().toString();

	private String mMockAccountId = UUIDUtils.randomUUID().toString();

	private CryptoUtils.DecryptedData mDecryptedToken;

	@Before
	public void setUp() {
		mockStatic(SharedPreferencesManager.class);
		mockStatic(CryptoUtils.class);
		when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);

		/* Mock token. */
		mTokenStorage = TokenStorageFactory.getTokenStorage(mock(Context.class));
		mDecryptedToken = mock(CryptoUtils.DecryptedData.class);
		when(mDecryptedToken.getDecryptedData()).thenReturn(mMockToken);
	}

	@Test
	public void testSave() {

		/* Save the token into storage. */
		when(mCryptoUtils.encrypt(eq(mMockToken))).thenReturn(mMockEncryptedToken);
		mTokenStorage.saveToken(mMockToken, mMockAccountId);
		verify(mCryptoUtils).encrypt(mMockToken);
		mCryptoUtils.encrypt(mMockToken);

		/* Verify save called on context and preferences. */
		verifyStatic();
		SharedPreferencesManager.putString(eq(PREFERENCE_KEY_AUTH_TOKEN), eq(mMockEncryptedToken));
		verifyStatic();
		SharedPreferencesManager.putString(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID), eq(mMockAccountId));
		verifyStatic();
		SharedPreferencesManager.putString(eq(PREFERENCE_KEY_TOKEN_HISTORY), anyString());


	}

	@Test
	public void testRemove() {

		/* Remove the token from storage. */
		//mTokenStorage.removeToken(TODO);

		/* Verify remove called on context and preferences. */
		when(mCryptoUtils.encrypt(eq(mMockToken))).thenReturn(mMockEncryptedToken);
		mTokenStorage.removeToken(mMockToken);
		verify(mCryptoUtils).encrypt(mMockToken);

		verifyStatic();
		SharedPreferencesManager.remove(eq(PREFERENCE_KEY_AUTH_TOKEN));
		verifyStatic();
		SharedPreferencesManager.remove(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID));

	}

	@Test
	public void testGet() {

		/* Mock preferences and crypto calls. */
		when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(mMockToken);
		when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_HOME_ACCOUNT_ID), isNull(String.class))).thenReturn(mMockAccountId);
		when(mCryptoUtils.decrypt(eq(mMockToken), eq(false))).thenReturn(mDecryptedToken);

		/* Verify the right token is returned. */
		assertEquals(mMockToken, mTokenStorage.getToken());
	}

	@Test
	public void testGetFailsWithNull() {
		when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn(null);
		assertNull(mTokenStorage.getToken());
		when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_AUTH_TOKEN), isNull(String.class))).thenReturn("");
		assertNull(mTokenStorage.getToken());
	}
}

