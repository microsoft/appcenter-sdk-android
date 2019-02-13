package com.microsoft.appcenter.utils.context;

import android.content.Context;

import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

/**
 * Storage for tokens that uses Shared Preferences. Handles saving and encryption.
 */
public class PreferenceTokenStorage implements TokenStorage {

    /**
     * Context instance.
     */
    private Context mContext;

    /**
     * Default constructor.
     * @param context context instance.
     */
    public PreferenceTokenStorage(Context context) {
        mContext = context;
    }

    /**
     * Base key for stored preferences.
     */
    static final String PREFERENCE_PREFIX = "AppCenter.";

    /**
     * Used for authentication requests, string field for auth token.
     */
    static final String PREFERENCE_KEY_AUTH_TOKEN = PREFERENCE_PREFIX + "auth_token";

    @Override
    public void saveToken(String token) {
        String encryptedToken = CryptoUtils.getInstance(mContext).encrypt(token);
        SharedPreferencesManager.putString(PREFERENCE_KEY_AUTH_TOKEN, encryptedToken);
    }

    @Override
    public String getToken() {
        String encryptedToken = SharedPreferencesManager.getString(PREFERENCE_KEY_AUTH_TOKEN, "");
        if (encryptedToken.length() == 0) {
            return "";
        }
        CryptoUtils.DecryptedData decryptedData = CryptoUtils.getInstance(mContext).decrypt(encryptedToken, false);
        return decryptedData.getDecryptedData();
    }

    @Override
    public void removeToken() {
        SharedPreferencesManager.remove(PREFERENCE_KEY_AUTH_TOKEN);
    }
}
