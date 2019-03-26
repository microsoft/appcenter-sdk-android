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
import java.util.List;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Storage for tokens that uses {@link SharedPreferencesManager}. Handles saving and encryption.
 */
public class PreferenceTokenStorage implements AuthTokenStorage {

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
     * {@link Context} instance.
     */
    private final Context mContext;

    /**
     * Token history.
     */
    private List<TokenStoreEntity> mHistory;

    /**
     * Default constructor.
     *
     * @param context {@link Context} instance.
     */
    PreferenceTokenStorage(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public synchronized void saveToken(String token, String homeAccountId, Date expiresOn) {
        List<TokenStoreEntity> history = getHistory();
        if (history == null) {
            history = new ArrayList<TokenStoreEntity>() {{

                /*
                 * Adding a null entry is required during the first initialization to differentiate
                 * anonymous usage before the moment and situation when we don't have a token
                 * in history because of the size limit for example.
                 */
                add(new TokenStoreEntity(null, null, null, null));
            }};
        }

        /* Do not add the same token twice in a row. */
        TokenStoreEntity lastEntry = history.size() > 0 ? history.get(history.size() - 1) : null;
        if (lastEntry == null || !TextUtils.equals(lastEntry.getToken(), token)) {
            Date date = new Date();

            /* If there is a gap between tokens. */
            if (lastEntry != null && lastEntry.getExpiresOn() != null && date.after(lastEntry.getExpiresOn())) {

                /* If the account the same or become anonymous. */
                if (token == null || TextUtils.equals(lastEntry.getHomeAccountId(), homeAccountId)) {

                    /* Apply the new token to this time. */
                    date = lastEntry.getExpiresOn();
                }
            }
            history.add(new TokenStoreEntity(token, homeAccountId, date, expiresOn));
        }

        /* Limit history size. */
        if (history.size() > TOKEN_HISTORY_LIMIT) {
            history.remove(0);
            AppCenterLog.debug(LOG_TAG, "Size of the token history is exceeded. The oldest token has been removed.");
        }

        /* Update history and current token. */
        setHistory(history);
    }

    @Override
    public synchronized String getToken() {
        List<TokenStoreEntity> history = getHistory();
        if (history != null && history.size() > 0) {
            return history.get(history.size() - 1).getToken();
        }
        return null;
    }

    @Override
    public synchronized String getHomeAccountId() {
        List<TokenStoreEntity> history = getHistory();
        if (history != null && history.size() > 0) {
            return history.get(history.size() - 1).getHomeAccountId();
        }
        return null;
    }

    @Override
    public synchronized List<AuthTokenInfo> getTokenHistory() {
        List<TokenStoreEntity> history = getHistory();
        if (history == null || history.size() == 0) {
            return null;
        }

        /* Return history with corrected end times. */
        List<AuthTokenInfo> result = new ArrayList<>();
        for (int i = 0; i < history.size(); i++) {
            TokenStoreEntity storeEntity = history.get(i);
            String token = storeEntity.getToken();
            Date endTime = storeEntity.getExpiresOn();
            Date nextChangeTime = history.size() > i + 1 ? history.get(i + 1).getTime() : null;
            if (nextChangeTime != null && endTime != null && nextChangeTime.before(endTime)) {
                endTime = nextChangeTime;
            } else if (endTime == null && nextChangeTime != null) {
                endTime = nextChangeTime;
            }
            result.add(new AuthTokenInfo(token, storeEntity.getTime(), endTime));
        }
        return result;
    }

    @Override
    public synchronized void removeToken(String token) {
        List<TokenStoreEntity> history = getHistory();
        if (history == null || history.size() == 0) {
            AppCenterLog.warn(LOG_TAG, "Couldn't remove token from history: token history is empty.");
            return;
        }
        if (history.size() == 1) {
            AppCenterLog.debug(LOG_TAG, "Couldn't remove token from history: token history contains only current one.");
            return;
        }
        TokenStoreEntity storeEntity = history.get(0);
        if (!TextUtils.equals(storeEntity.getToken(), token)) {
            AppCenterLog.debug(LOG_TAG, "Couldn't remove token from history: the token isn't oldest or is already removed.");
            return;
        }

        /* Remove the token from history. */
        history.remove(0);
        setHistory(history);
        AppCenterLog.debug(LOG_TAG, "The token has been removed from token history.");
    }

    @VisibleForTesting
    List<TokenStoreEntity> getHistory() {
        if (mHistory != null) {
            return mHistory;
        }
        String encryptedJson = SharedPreferencesManager.getString(PREFERENCE_KEY_TOKEN_HISTORY, null);
        String json = null;
        if (encryptedJson != null && !encryptedJson.isEmpty()) {
            CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(encryptedJson, false);
            json = decryptedData.getDecryptedData();
        }
        if (json == null || json.isEmpty()) {
            return null;
        }
        TokenStoreEntity[] entities = null;
        try {
            entities = new Gson().fromJson(json, TokenStoreEntity[].class);
        } catch (JsonParseException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to deserialize auth token history.", e);
        }
        mHistory = entities != null ? new ArrayList<>(Arrays.asList(entities)) : new ArrayList<TokenStoreEntity>();
        return mHistory;
    }

    @VisibleForTesting
    void setHistory(List<TokenStoreEntity> history) {
        mHistory = history;
        if (history != null) {
            String json = new Gson().toJson(history.toArray());
            String encryptedJson = CryptoUtils.getInstance(mContext).encrypt(json);
            SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, encryptedJson);
        } else {
            SharedPreferencesManager.remove(PREFERENCE_KEY_TOKEN_HISTORY);
        }
    }

    @VisibleForTesting
    static class TokenStoreEntity {

        @SerializedName("token")
        private String mToken;

        @SerializedName("homeAccountId")
        private String mHomeAccountId;

        @SerializedName("time")
        private Date mTime;

        @SerializedName("expiresOn")
        private Date mExpiresOn;

        TokenStoreEntity(String token, String homeAccountId, Date time, Date expiresOn) {
            mToken = token;
            mHomeAccountId = homeAccountId;
            mTime = time;
            mExpiresOn = expiresOn;
        }

        String getToken() {
            return mToken;
        }

        String getHomeAccountId() {
            return mHomeAccountId;
        }

        Date getTime() {
            return mTime;
        }

        Date getExpiresOn() {
            return mExpiresOn;
        }
    }
}
