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
import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.*;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

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
	static final String PREFERENCE_KEY_TOKEN_HISTORY = "AppCenter.auth_token_history";

	static final int TOKEN_HISTORY_LIMIT = 5;

	/**
	 * @param token         auth token.
	 * @param homeAccountId unique identifier of user.
	 *                      Saving all tokens into history with time when it was valid
	 */
	@Override
	public void saveToken(String token, String homeAccountId) {
		String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);

		// TODO: lazy cache?
		List<TokenStoreEntity> history = getTokenHistoryFromStorage();
		if (history == null) {
			history = new ArrayList<TokenStoreEntity>() {{
				add(new TokenStoreEntity(null, null));
			}};
		}
		history.add(new TokenStoreEntity(encryptedToken, new Date()));

		/* Limit history size. */
		if (history.size() > TOKEN_HISTORY_LIMIT) {
			history.remove(0);
		}

		/* Update history and current token. */
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
		List<TokenStoreEntity> history = getTokenHistoryFromStorage();
		if (history == null || history.size() == 0) {
			return new AuthTokenInfo(getToken(), null, null);
		}
		TokenStoreEntity storeEntity = history.get(0);
		String token = storeEntity.getToken();
		if (token != null && token.length() > 0) {
			CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(token, false);
			token = decryptedData.getDecryptedData();
		}
		Date endTime = history.size() > 1 ? history.get(1).getTime() : null;
		return new AuthTokenInfo(token, storeEntity.getTime(), endTime);
	}

	@Override
	public void removeToken(String token) {
		List<TokenStoreEntity> history = getTokenHistoryFromStorage();
		String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);

		if (history != null) {
			TokenStoreEntity tokenToRemove = null;

			/* Find token in token history */
			Iterator<TokenStoreEntity> iterator = history.listIterator();
			while (iterator.hasNext()) {
				TokenStoreEntity tokenStoreEntity = iterator.next();
				if (tokenStoreEntity.getToken().equals(encryptedToken)) {
					tokenToRemove = tokenStoreEntity;
					iterator.remove();
				}
			}
			/* if token was exist in history    */
			if (tokenToRemove != null) {
				SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, new Gson().toJson(history));
			}
		}
	}

	private static class TokenStoreEntity {

		@SerializedName("token")
		private String mToken;

		@SerializedName("time")
		private Date mTime;

		TokenStoreEntity(String token, Date time) {
			mToken = token;
			mTime = time;
		}

		String getToken() {
			return mToken;
		}

		Date getTime() {
			return mTime;
		}
	}

	private List<TokenStoreEntity> getTokenHistoryFromStorage() {
		String tokenHistoryJson = SharedPreferencesManager.getString(PREFERENCE_KEY_TOKEN_HISTORY, null);
		if (tokenHistoryJson == null) {
			return null;
		}
		List<TokenStoreEntity> history = null;
		try {
			history = Arrays.asList(new Gson().fromJson(tokenHistoryJson, TokenStoreEntity[].class));
		} catch (JsonParseException e) {
			AppCenterLog.warn(LOG_TAG, "Failed to deserialize auth token history.", e);
		}
		if (history == null) {
			history = new ArrayList<>();
		}
		return history;
	}
}
