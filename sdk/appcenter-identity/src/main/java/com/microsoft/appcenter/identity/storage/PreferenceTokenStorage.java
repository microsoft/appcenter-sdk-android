package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

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
    static final String PREFERENCE_KEY_HOME_ACCOUNT_ID = "AppCenter.account_id";

    @Override
    public void saveToken(String token, String homeAccountId) {
        AuthTokenContext.getInstance().setAuthToken(token, homeAccountId);
        String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
        SharedPreferencesManager.putString(PREFERENCE_KEY_AUTH_TOKEN, encryptedToken);
        SharedPreferencesManager.putString(PREFERENCE_KEY_HOME_ACCOUNT_ID, homeAccountId);
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
    private String getHomeAccountId() {
        return SharedPreferencesManager.getString(PREFERENCE_KEY_HOME_ACCOUNT_ID, null);
    }

    @Override
    public void cacheToken() {
        String tokenFromStorage = getToken();
        String accountId = getHomeAccountId();

        /* We need to update Token context here if the values are not null. */
        if (tokenFromStorage != null && accountId != null) {
            AuthTokenContext.getInstance().setAuthToken(tokenFromStorage, accountId);
        }
    }

    @Override
    public void removeToken() {
        SharedPreferencesManager.remove(PREFERENCE_KEY_AUTH_TOKEN);
        SharedPreferencesManager.remove(PREFERENCE_KEY_HOME_ACCOUNT_ID);
        AuthTokenContext.getInstance().clearToken();
    }
}
