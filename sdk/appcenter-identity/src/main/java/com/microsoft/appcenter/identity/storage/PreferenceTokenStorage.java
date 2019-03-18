/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenInfo;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Storage for tokens that uses {@link SharedPreferencesManager}. Handles saving and encryption.
 */
public class PreferenceTokenStorage implements AuthTokenStorage {

	/**
	 * {@link Context} instance.
	 */
	private final Context mContext;

	/**
	 * Default constructor.
	 *
	 * @param context {@link Context} instance.
	 */
	PreferenceTokenStorage(@NonNull Context context) {
		mContext = context.getApplicationContext();
	}

	/**
	 * Used for authentication requests, string field for auth token.
	 */
	@VisibleForTesting
	static final String PREFERENCE_KEY_AUTH_TOKEN = "AppCenter.auth_token";

	/**
	 * Used for distinguishing users, string field for home account id.
	 */
	@VisibleForTesting
	static final String PREFERENCE_KEY_HOME_ACCOUNT_ID = "AppCenter.home_account_id";

	/**
	 * Used for saving tokens history
	 */
	@VisibleForTesting
	static final String PREFERENCE_KEY_TOKEN_HISTORY = "TOKEN_HISTORY";

	static final int TOKEN_HISTORY_LIMIT = 5;

	/**
	 * @param token         auth token.
	 * @param homeAccountId unique identifier of user.
	 *                      Saving all tokens into history with time when it was valid
	 */
	@Override
	public void saveToken(String token, String homeAccountId) {
		String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
		String tokenHistoryJson = SharedPreferencesManager.getString(PREFERENCE_KEY_TOKEN_HISTORY, null);
		List<TokenStoreEntity> history;
		if (tokenHistoryJson == null) {
			history = new ArrayList<>();
			TokenStoreEntity initialTokenStore = new TokenStoreEntity(null, null);
			history.add(initialTokenStore);
		} else {
			try {
				history = new Gson().fromJson(tokenHistoryJson, new TypeToken<TokenStoreEntity>() {
				}.getType());
			} catch (JsonParseException e) {
				history = new ArrayList<>();
				e.printStackTrace();
			}
		}
		Date currentTime = new Date();
		history.add(new TokenStoreEntity(encryptedToken, currentTime)); // token = encryptedToken, starttime = now
		if (history.size() > TOKEN_HISTORY_LIMIT) {
			history.remove(0);
		}
		SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, new Gson().toJson(history));
		if (token != null) {
			SharedPreferencesManager.putString(PREFERENCE_KEY_AUTH_TOKEN, encryptedToken);
			SharedPreferencesManager.putString(PREFERENCE_KEY_HOME_ACCOUNT_ID, homeAccountId);
		} else {
			SharedPreferencesManager.remove(PREFERENCE_KEY_AUTH_TOKEN);
			SharedPreferencesManager.remove(PREFERENCE_KEY_HOME_ACCOUNT_ID);
		}
	}

	@Override
	public String getToken() {
		String encryptedToken = SharedPreferencesManager.getString(PREFERENCE_KEY_AUTH_TOKEN, null);
		if (encryptedToken == null || encryptedToken.length() == 0) {
			return null;
		}
		CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(encryptedToken, false);
		return decryptedData.getDecryptedData();
	}

	/**
	 * Retrieves unique user id.
	 *
	 * @return unique user id.
	 */
	@Override
	public String getHomeAccountId() {
		return SharedPreferencesManager.getString(PREFERENCE_KEY_HOME_ACCOUNT_ID, null);
	}

	@Override
	public AuthTokenInfo getOldestToken() {
		String tokenHistoryJson = SharedPreferencesManager.getString(PREFERENCE_KEY_TOKEN_HISTORY, null);
		if (tokenHistoryJson == null) {
			return null;
		}
		ArrayList<TokenStoreEntity> history;
		try {
			history = new Gson().fromJson(tokenHistoryJson, new TypeToken<AuthTokenInfo>() {
			}.getType());
		} catch (JsonParseException e) {
			history = new ArrayList<>();
		}
		if (history.size() < 2) {
			return null;
		}
		TokenStoreEntity firstToken = history.get(0);
		TokenStoreEntity secondToken = history.get(1);

		if (firstToken.getTime() == null && firstToken.getToken() == null)
			return new AuthTokenInfo(secondToken.getToken(), null, secondToken.getTime());
		return new AuthTokenInfo(firstToken.mToken, firstToken.getTime(), secondToken.getTime());
	}

	@Override
	public void removeToken(String token) {

	}

	private class TokenStoreEntity {
		private String mToken;

		private Date mTime;

		TokenStoreEntity(String token, Date time) {
			this.mToken = token;
			this.mTime = time;
		}

		String getToken() {
			return mToken;
		}

		Date getTime() {
			return mTime;
		}
	}
}
