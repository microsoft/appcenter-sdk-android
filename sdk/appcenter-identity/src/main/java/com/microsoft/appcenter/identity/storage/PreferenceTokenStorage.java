/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.AuthTokenStorage;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
     * Used for saving tokens history.
     */
    @VisibleForTesting
    static final String PREFERENCE_KEY_TOKEN_HISTORY = "AppCenter.auth_token_history";

    /**
     * The maximum number of tokens stored in the history.
     */
    @VisibleForTesting
    static final int TOKEN_HISTORY_LIMIT = 5;

    /**
     * Saving all tokens into history with time when it was valid.
     *
     * @param token         auth token.
     * @param homeAccountId unique identifier of user.
     * @param expiresTimestamp time when token create.
     */
    @Override
    public void saveToken(String token, String homeAccountId, Date expiresTimestamp) {
        String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
        List<TokenStoreEntity> history = loadTokenHistory();
        if (history == null) {
            history = new ArrayList<TokenStoreEntity>() {{
                add(new TokenStoreEntity(null, null, null));
            }};
        }
        history.add(new TokenStoreEntity(encryptedToken, new Date(), expiresTimestamp));

        /* Limit history size. */
        if (history.size() > TOKEN_HISTORY_LIMIT) {
            AppCenterLog.debug(LOG_TAG, "Size of the token history is exceeded. The oldest token has been removed.");
            history.remove(0);
        }

        /* Update history and current token. */
        saveTokenHistory(history);
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
        List<TokenStoreEntity> history = loadTokenHistory();
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
        List<TokenStoreEntity> history = loadTokenHistory();
        if (history == null) {
            AppCenterLog.warn(LOG_TAG, "Couldn't remove token from history: the token history is empty.");
            return;
        }
        String encryptedToken = null;
        if (token != null && token.length() > 0) {
            encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
        }

        /* Find token in token history. */
        Iterator<TokenStoreEntity> iterator = history.listIterator();
        while (iterator.hasNext()) {
            TokenStoreEntity entity = iterator.next();
            if (TextUtils.equals(entity.getToken(), encryptedToken)) {
                iterator.remove();
                saveTokenHistory(history);
                AppCenterLog.debug(LOG_TAG, "The token has been removed from the token history.");
                return;
            }
        }
        AppCenterLog.warn(LOG_TAG, "Couldn't find token in the token history.");
    }

    @VisibleForTesting
    List<TokenStoreEntity> loadTokenHistory() {
        String historyJson = SharedPreferencesManager.getString(PREFERENCE_KEY_TOKEN_HISTORY, null);
        if (historyJson == null) {
            return null;
        }
        TokenStoreEntity[] entities = null;
        try {
            entities = new Gson().fromJson(historyJson, TokenStoreEntity[].class);
        } catch (JsonParseException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to deserialize auth token history.", e);
        }
        if (entities != null) {
            return new ArrayList<>(Arrays.asList(entities));
        }
        return new ArrayList<>();
    }

    void saveTokenHistory(List<TokenStoreEntity> history) {
        String json = new Gson().toJson(history.toArray());
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, json);
    }

    private static class TokenStoreEntity {

        @SerializedName("token")
        private String mToken;

        @SerializedName("time")
        private Date mTime;

        @SerializedName("expiresTimestamp")
        private Date mExpiresTimestamp;

        TokenStoreEntity(String token, Date time, Date expiresTimestamp) {
            mToken = token;
            mTime = time;
            mExpiresTimestamp = expiresTimestamp;
        }

        String getToken() {
            return mToken;
        }

        Date getTime() {
            return mTime;
        }
    }
}
