/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Utility to store and retrieve the latest authorization token.
 */
public class AuthTokenContext {

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
     * The Maximum length of account Id.
     */
    @VisibleForTesting
    static final int ACCOUNT_ID_LENGTH = 36;

    /**
     * Unique instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static AuthTokenContext sInstance;

    /**
     * Global listeners collection.
     */
    private final Set<Listener> mListeners = Collections.newSetFromMap(new ConcurrentHashMap<Listener, Boolean>());

    /**
     * {@link Context} instance.
     */
    private Context mContext;

    /**
     * Token history.
     */
    private List<AuthTokenHistoryEntry> mHistory;

    /**
     * {@code true} if the current token should be reset.
     */
    private boolean mResetAuthTokenRequired = true;

    /**
     * Initializes AuthTokenContext class.
     *
     * @param context {@link Context} instance.
     */
    public static synchronized void initialize(@NonNull Context context) {
        AuthTokenContext authTokenContext = getInstance();
        authTokenContext.mContext = context.getApplicationContext();
        authTokenContext.getHistory();
    }

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized AuthTokenContext getInstance() {
        if (sInstance == null) {
            sInstance = new AuthTokenContext();
        }
        return sInstance;
    }

    /**
     * Unset singleton instance.
     */
    @VisibleForTesting
    public static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Adds listener to token context.
     *
     * @param listener listener to be notified of changes.
     */
    public void addListener(@NonNull Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a specific listener.
     *
     * @param listener listener to be removed.
     */
    public void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Prevents resetting the current auth token if it exists. Should be called during
     * initialization process if the current auth token should be kept.
     */
    public synchronized void doNotResetAuthAfterStart() {
        mResetAuthTokenRequired = false;
    }

    /**
     * Finishes initialization process. Resets current token if nothing prevents it.
     */
    public synchronized void finishInitialization() {
        if (!mResetAuthTokenRequired) {
            return;
        }
        mResetAuthTokenRequired = false;
        setAuthToken(null, null, null);
    }

    /**
     * Sets new authorization token.
     *
     * @param authToken     authorization token.
     * @param homeAccountId unique user id.
     * @param expiresOn     time when token expires.
     */
    public void setAuthToken(String authToken, String homeAccountId, Date expiresOn) {

        /* Do not store any data for anonymous token. */
        if (authToken == null) {
            homeAccountId = null;
            expiresOn = null;
        }
        Boolean isNewUser = addTokenHistory(authToken, homeAccountId, expiresOn);
        if (isNewUser == null) {
            return;
        }

        /* Call listeners so that they can react on new token. */
        for (Listener listener : mListeners) {
            listener.onNewAuthToken(authToken);
            if (isNewUser) {
                String accountId = homeAccountId == null ? null : homeAccountId.substring(0, Math.min(ACCOUNT_ID_LENGTH, homeAccountId.length()));
                listener.onNewUser(accountId);
            }
        }
    }

    /**
     * Add token history.
     *
     * @param authToken     authorization token.
     * @param homeAccountId unique user id.
     * @param expiresOn     time when token expires.
     * @return true if it is a new user.
     */
    private synchronized Boolean addTokenHistory(String authToken, String homeAccountId, Date expiresOn) {
        List<AuthTokenHistoryEntry> history = getHistory();
        if (history == null) {
            history = new ArrayList<>();
        }

        /* Do not add the same token twice in a row. */
        AuthTokenHistoryEntry lastEntry = history.size() > 0 ? history.get(history.size() - 1) : null;
        if (lastEntry != null && TextUtils.equals(lastEntry.getAuthToken(), authToken)) {
            return null;
        }

        /* Check if it's a new user before changing current home account id. */
        boolean isNewUser = lastEntry == null || !TextUtils.equals(lastEntry.getHomeAccountId(), homeAccountId);
        Date date = new Date();

        /* If there is a gap between tokens. */
        if (lastEntry != null && lastEntry.getExpiresOn() != null && date.after(lastEntry.getExpiresOn())) {

            /* If the account is the same or becomes anonymous. */
            if (!isNewUser || authToken == null) {

                /* Apply the new token to this time. */
                date = lastEntry.getExpiresOn();
            } else {

                /* If it's not the same account treat the gap as anonymous. */
                history.add(new AuthTokenHistoryEntry(null, null, lastEntry.getExpiresOn(), date));
            }
        }
        history.add(new AuthTokenHistoryEntry(authToken, homeAccountId, date, expiresOn));

        /* Limit history size. */
        if (history.size() > TOKEN_HISTORY_LIMIT) {
            history.subList(0, history.size() - TOKEN_HISTORY_LIMIT).clear();
            AppCenterLog.debug(LOG_TAG, "Size of the token history is exceeded. The oldest token has been removed.");
        }

        /* Update history and current token. */
        setHistory(history);
        return isNewUser;
    }

    /**
     * Gets current authorization token.
     *
     * @return authorization token.
     */
    public String getAuthToken() {
        AuthTokenHistoryEntry lastEntry = getLastHistoryEntry();
        return lastEntry != null ? lastEntry.getAuthToken() : null;
    }

    /**
     * Gets current homeAccountId value.
     *
     * @return unique identifier of user.
     */
    public String getHomeAccountId() {
        AuthTokenHistoryEntry lastEntry = getLastHistoryEntry();
        return lastEntry != null ? lastEntry.getHomeAccountId() : null;
    }

    /**
     * Gets current account ID value.
     *
     * @return unique identifier of the account.
     */
    public String getAccountId() {
        String homeAccountId = getHomeAccountId();
        return homeAccountId == null ? null : homeAccountId.substring(0, Math.min(ACCOUNT_ID_LENGTH, homeAccountId.length()));
    }

    /**
     * Gets list of auth tokens validity info. It contains tokens and time when it was valid.
     *
     * @return list of auth tokens validity info.
     * @see AuthTokenInfo
     */
    @NonNull
    public synchronized List<AuthTokenInfo> getAuthTokenValidityList() {
        List<AuthTokenHistoryEntry> history = getHistory();
        if (history == null || history.size() == 0) {
            return Collections.singletonList(new AuthTokenInfo());
        }

        /* Return history with corrected end times. */
        List<AuthTokenInfo> result = new ArrayList<>();
        if (history.get(0).getAuthToken() != null) {
            result.add(new AuthTokenInfo(null, null, history.get(0).getTime()));
        }
        for (int i = 0; i < history.size(); i++) {
            AuthTokenHistoryEntry storeEntity = history.get(i);
            String token = storeEntity.getAuthToken();
            Date startTime = storeEntity.getTime();
            if (token == null && i == 0) {
                startTime = null;
            }
            Date endTime = storeEntity.getExpiresOn();
            Date nextChangeTime = history.size() > i + 1 ? history.get(i + 1).getTime() : null;
            if (nextChangeTime != null && endTime != null && nextChangeTime.before(endTime)) {
                endTime = nextChangeTime;
            } else if (endTime == null && nextChangeTime != null) {
                endTime = nextChangeTime;
            }
            result.add(new AuthTokenInfo(token, startTime, endTime));
        }
        return result;
    }

    /**
     * Removes the token from history. Please note that only oldest token is
     * allowed to remove. To reset current to anonymous, use
     * {@link #setAuthToken(String, String, Date)} with <code>null</code> value instead.
     *
     * @param token auth token to remove. Despite the fact that only the oldest
     *              token can be removed, it's required to avoid removing
     *              the wrong one on duplicated calls etc.
     */
    public synchronized void removeOldestTokenIfMatching(String token) {
        List<AuthTokenHistoryEntry> history = getHistory();
        if (history == null || history.size() == 0) {
            AppCenterLog.warn(LOG_TAG, "Couldn't remove token from history: token history is empty.");
            return;
        }
        if (history.size() == 1) {
            AppCenterLog.debug(LOG_TAG, "Couldn't remove token from history: token history contains only current one.");
            return;
        }
        AuthTokenHistoryEntry storeEntity = history.get(0);
        if (!TextUtils.equals(storeEntity.getAuthToken(), token)) {
            AppCenterLog.debug(LOG_TAG, "Couldn't remove token from history: the token isn't oldest or is already removed.");
            return;
        }

        /* Remove the token from history. */
        history.remove(0);
        setHistory(history);
        AppCenterLog.debug(LOG_TAG, "The token has been removed from token history.");
    }

    /**
     * Performs check on auth token and calls relevant delegate to refresh it.
     *
     * @param authTokenInfo auth token to check for expiration.
     */
    public void checkIfTokenNeedsToBeRefreshed(@NonNull AuthTokenInfo authTokenInfo) {
        AuthTokenHistoryEntry lastEntry = getLastHistoryEntry();
        if (lastEntry == null || authTokenInfo.getAuthToken() == null ||
                !authTokenInfo.getAuthToken().equals(lastEntry.getAuthToken()) ||
                !authTokenInfo.isAboutToExpire()) {
            return;
        }
        for (Listener listener : mListeners) {
            listener.onTokenRequiresRefresh(lastEntry.getHomeAccountId());
        }
    }

    /**
     * Gets the last entry from history.
     *
     * @return the last auth token entry.
     */
    private synchronized AuthTokenHistoryEntry getLastHistoryEntry() {
        List<AuthTokenHistoryEntry> history = getHistory();
        if (history != null && history.size() > 0) {
            return history.get(history.size() - 1);
        }
        return null;
    }

    @VisibleForTesting
    List<AuthTokenHistoryEntry> getHistory() {
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
        try {
            mHistory = deserializeHistory(json);
        } catch (JSONException e) {
            AppCenterLog.warn(LOG_TAG, "Failed to deserialize auth token history.", e);
        }
        return mHistory;
    }

    @VisibleForTesting
    void setHistory(List<AuthTokenHistoryEntry> history) {
        mHistory = history;
        if (history != null) {
            try {
                String json = serializeHistory(history);
                String encryptedJson = CryptoUtils.getInstance(mContext).encrypt(json);
                SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, encryptedJson);
            } catch (JSONException e) {
                AppCenterLog.warn(LOG_TAG, "Failed to serialize auth token history.", e);
            }
        } else {
            SharedPreferencesManager.remove(PREFERENCE_KEY_TOKEN_HISTORY);
        }
    }

    private String serializeHistory(List<AuthTokenHistoryEntry> history) throws JSONException {
        JSONStringer writer = new JSONStringer();
        writer.array();
        for (AuthTokenHistoryEntry entry : history) {
            writer.object();
            entry.write(writer);
            writer.endObject();
        }
        writer.endArray();
        return writer.toString();
    }

    private List<AuthTokenHistoryEntry> deserializeHistory(String json) throws JSONException {
        JSONArray jArray = new JSONArray(json);
        List<AuthTokenHistoryEntry> array = new ArrayList<>(jArray.length());
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject jModel = jArray.getJSONObject(i);
            AuthTokenHistoryEntry entry = new AuthTokenHistoryEntry();
            entry.read(jModel);
            array.add(entry);
        }
        return array;
    }

    /**
     * Token context global listener specification.
     */
    public interface Listener {

        /**
         * Called whenever a new token is set.
         *
         * @param authToken authorization token.
         */
        void onNewAuthToken(@SuppressWarnings("unused") String authToken);

        /**
         * Called whenever a new user signs in.
         *
         * @param accountId account id.
         */
        void onNewUser(String accountId);

        /**
         * Called whenever token needs to be refreshed.
         *
         * @param homeAccountId account id to call refresh with.
         */
        void onTokenRequiresRefresh(String homeAccountId);
    }
}
