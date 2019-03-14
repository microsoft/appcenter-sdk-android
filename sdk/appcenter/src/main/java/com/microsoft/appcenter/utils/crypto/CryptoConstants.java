/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.annotation.SuppressLint;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

/**
 * Various constants used in the cryptography package.
 */
class CryptoConstants {

    /**
     * Android key store name.
     */
    static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    /**
     * Keystore alias prefix to use for App Center SDK.
     */
    static final String KEYSTORE_ALIAS_PREFIX = "appcenter";

    /**
     * Keystore alias prefix fallback for old Mobile Center store.
     */
    static final String KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER = "mobile.center";

    /**
     * Keystore alias separator.
     */
    static final String ALIAS_SEPARATOR = ".";

    /**
     * Separator between algorithm and encrypted data.
     */
    static final String ALGORITHM_DATA_SEPARATOR = ":";

    /**
     * Encoding charset used for bytes/string conversion.
     */
    static final String CHARSET = "UTF-8";

    /**
     * How long an encryption key is valid for producing new  encrypted data.
     * Decrypting is always allowed and needed for rollover and migration.
     */
    static final int ENCRYPT_KEY_LIFETIME_IN_YEARS = 1;

    /**
     * Key size for AES.
     */
    static final int AES_KEY_SIZE = 256;

    /**
     * Key size for RSA.
     */
    static final int RSA_KEY_SIZE = 2048;

    /**
     * Cipher used for AES.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    static final String CIPHER_AES = KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7;

    /**
     * Cipher used for RSA.
     */
    @SuppressLint("InlinedApi")
    static final String CIPHER_RSA = KeyProperties.KEY_ALGORITHM_RSA + "/" + KeyProperties.BLOCK_MODE_ECB + "/" + KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1;

    /**
     * Cipher provider when running on old devices.
     */
    static final String PROVIDER_ANDROID_OLD = "AndroidOpenSSL";

    /**
     * Cipher provider when running on M devices.
     */
    static final String PROVIDER_ANDROID_M = "AndroidKeyStoreBCWorkaround";
}
