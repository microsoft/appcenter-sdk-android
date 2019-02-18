package com.microsoft.appcenter.identity.storage;

import android.content.Context;

import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

/**
 * Storage for tokens that uses {@link SharedPreferencesManager}. Handles saving and encryption.
 */
class PreferenceTokenStorage implements AuthTokenStorage {

    /**
     * {@link Context} instance.
     */
    private final Context mContext;

    /**
     * Default constructor.
     *
     * @param context {@link Context} instance.
     */
    PreferenceTokenStorage(Context context) {
        mContext = context;
    }

    /**
     * Used for authentication requests, string field for auth token.
     */
    @SuppressWarnings("WeakerAccess")
    static final String PREFERENCE_KEY_AUTH_TOKEN = "AppCenter.auth_token";

    @Override
    public void saveToken(String token) {
        String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
        SharedPreferencesManager.putString(PREFERENCE_KEY_AUTH_TOKEN, encryptedToken);
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

    @Override
    public void removeToken() {
        SharedPreferencesManager.remove(PREFERENCE_KEY_AUTH_TOKEN);
    }
}
