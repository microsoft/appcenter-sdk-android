/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.PREFERENCE_KEY_TOKEN_HISTORY;
import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.TOKEN_HISTORY_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PreferenceTokenStorageTest {


	private Context mContext;

	@Before
	public void setUp() {
		mContext = InstrumentationRegistry.getTargetContext();
		SharedPreferencesManager.initialize(mContext);
	}

	@After
	public void tearDown() {
		SharedPreferencesManager.clear();
	}

	@Test
	public void testPreferenceTokenStorage() {

		/* Mock token. */
		AuthTokenStorage tokenStorage = TokenStorageFactory.getTokenStorage(mContext);
		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();

		/* Save the token into storage. */
		tokenStorage.saveToken(mockToken, mockAccountId);

		/* Assert that storage returns the same token.*/
		assertEquals(mockToken, tokenStorage.getToken());
		assertEquals(mockAccountId, tokenStorage.getHomeAccountId());

		/* Remove the token from storage. */
		tokenStorage.saveToken(null, null);

		/* Assert that there's no token in storage. */
		assertNull(tokenStorage.getToken());
		assertNull(tokenStorage.getHomeAccountId());
	}

	@Test
	public void saveToken() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

		tokenStorage.saveToken(null, null);

		assertEquals(2, tokenStorage.loadTokenHistory().size());


	}

	@Test
	public void tokenHistoryLimit() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

		for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
			String mockToken = UUIDUtils.randomUUID().toString();
			String mockAccountId = UUIDUtils.randomUUID().toString();
			tokenStorage.saveToken(mockToken, mockAccountId);
		}

		assertEquals(TOKEN_HISTORY_LIMIT, tokenStorage.loadTokenHistory().size());
	}

	@Test
	public void removeTokenFromHistory() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);


		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();
		tokenStorage.saveToken(mockToken, mockAccountId);
		assertEquals(2, tokenStorage.loadTokenHistory().size());

		tokenStorage.removeToken(null);
		assertEquals(1, tokenStorage.loadTokenHistory().size());

		SharedPreferencesManager.clear();

		tokenStorage.removeToken(null);
		assertNull(tokenStorage.loadTokenHistory());

		tokenStorage.saveToken(mockToken, mockAccountId);
	}

	@Test
	public void loadTokenHistory() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
		assertNull(tokenStorage.loadTokenHistory());
		String mockToken = UUIDUtils.randomUUID().toString();
		String mockAccountId = UUIDUtils.randomUUID().toString();
		tokenStorage.saveToken(mockToken, mockAccountId);
		assertEquals(2, tokenStorage.loadTokenHistory().size());

		SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, "some bad json");
		assertEquals(0, tokenStorage.loadTokenHistory().size());


	}

	@Test
	public void getOldestToken() {
		PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
		assertNull(tokenStorage.getOldestToken().getEndTime());
		for (int i = 0; i < 10; i++) {
			String mockToken = UUIDUtils.randomUUID().toString();
			String mockAccountId = UUIDUtils.randomUUID().toString();
			tokenStorage.saveToken(mockToken, mockAccountId);
		}
		assertNotNull(tokenStorage.getOldestToken());
		assertNotNull(tokenStorage.getOldestToken().getEndTime());
		tokenStorage.saveTokenHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
		assertNotNull(tokenStorage.getOldestToken());
		assertNull(tokenStorage.getOldestToken().getEndTime());

	}
}
