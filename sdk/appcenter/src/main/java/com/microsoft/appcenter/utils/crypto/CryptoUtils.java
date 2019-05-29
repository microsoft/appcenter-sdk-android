/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.util.Base64;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ALGORITHM_DATA_SEPARATOR;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ALIAS_SEPARATOR;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CHARSET;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.KEYSTORE_ALIAS_PREFIX;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER;

/**
 * Tool to encrypt/decrypt strings seamlessly.
 */
public class CryptoUtils {

    @VisibleForTesting
    static final ICryptoFactory DEFAULT_CRYPTO_FACTORY = new ICryptoFactory() {

        @Override
        public IKeyGenerator getKeyGenerator(String algorithm, String provider) throws Exception {
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm, provider);
            return new IKeyGenerator() {

                @Override
                public void init(AlgorithmParameterSpec parameters) throws Exception {
                    keyGenerator.init(parameters);
                }

                @Override
                public void generateKey() {
                    keyGenerator.generateKey();
                }
            };
        }

        @Override
        public ICipher getCipher(String transformation, String provider) throws Exception {
            final Cipher cipher = Cipher.getInstance(transformation, provider);
            return new ICipher() {

                @Override
                public void init(int opMode, Key key) throws Exception {
                    cipher.init(opMode, key);
                }

                @Override
                public void init(int opMode, Key key, AlgorithmParameterSpec params) throws Exception {
                    cipher.init(opMode, key, params);
                }

                @Override
                public byte[] doFinal(byte[] input) throws Exception {
                    return cipher.doFinal(input);
                }

                @Override
                public byte[] doFinal(byte[] input, int inputOffset, int inputLength) throws Exception {
                    return cipher.doFinal(input, inputOffset, inputLength);
                }

                @Override
                public byte[] getIV() {
                    return cipher.getIV();
                }

                @Override
                public int getBlockSize() {
                    return cipher.getBlockSize();
                }

                @Override
                public String getAlgorithm() {
                    return cipher.getAlgorithm();
                }

                @Override
                public String getProvider() {
                    return cipher.getProvider().getName();
                }
            };
        }
    };

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static CryptoUtils sInstance;

    /**
     * Supported crypto handlers. Ordered, first one is the preferred one.
     */
    @VisibleForTesting
    final Map<String, CryptoHandlerEntry> mCryptoHandlers = new LinkedHashMap<>();

    /**
     * Application context.
     */
    private final Context mContext;

    /**
     * Crypto factory.
     */
    private final ICryptoFactory mCryptoFactory;

    /**
     * Android API level.
     */
    private final int mApiLevel;

    /**
     * Android key store or null if could not use it.
     */
    private final KeyStore mKeyStore;

    /**
     * Init.
     *
     * @param context any context.
     */
    private CryptoUtils(@NonNull Context context) {
        this(context, DEFAULT_CRYPTO_FACTORY, Build.VERSION.SDK_INT);
    }

    @VisibleForTesting
    @TargetApi(Build.VERSION_CODES.M)
    CryptoUtils(@NonNull Context context, @NonNull ICryptoFactory cryptoFactory, int apiLevel) {

        /* Store application context. */
        mContext = context.getApplicationContext();
        mCryptoFactory = cryptoFactory;
        mApiLevel = apiLevel;

        /* Load Android secure key store if available. */
        KeyStore keyStore = null;
        if (apiLevel >= Build.VERSION_CODES.KITKAT) {
            try {
                keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
                keyStore.load(null);
            } catch (Exception e) {
                AppCenterLog.error(LOG_TAG, "Cannot use secure keystore on this device.");
            }
        }
        mKeyStore = keyStore;

        /* We have to use AES to be compliant but it's available only after Android M. */
        if (keyStore != null && apiLevel >= Build.VERSION_CODES.M) {
            try {
                registerHandler(new CryptoAesHandler());
            } catch (Exception e) {
                AppCenterLog.error(LOG_TAG, "Cannot use modern encryption on this device.");
            }
        }

        /*
         * Even if we're not going to use it on modern devices to decrypt,
         * we may have to decrypt stored data that was encrypted before the firmware was upgraded.
         * So we load this handler in every case.
         */
        if (keyStore != null) {
            try {
                registerHandler(new CryptoRsaHandler());
            } catch (Exception e) {
                AppCenterLog.error(LOG_TAG, "Cannot use old encryption on this device.");
            }
        }

        /* Add the fake handler at the end of the list no matter what. */
        CryptoNoOpHandler cryptoNoOpHandler = new CryptoNoOpHandler();
        mCryptoHandlers.put(cryptoNoOpHandler.getAlgorithm(), new CryptoHandlerEntry(0, 0, cryptoNoOpHandler));
    }

    /**
     * Get unique instance.
     *
     * @param context any context.
     * @return unique instance.
     */
    public static CryptoUtils getInstance(@NonNull Context context) {
        if (sInstance == null) {
            sInstance = new CryptoUtils(context);
        }
        return sInstance;
    }

    @VisibleForTesting
    ICryptoFactory getCryptoFactory() {
        return mCryptoFactory;
    }

    /**
     * Register handler and create alias the first time.
     */
    private void registerHandler(@NonNull CryptoHandler handler) throws Exception {

        /* Check which of the potential aliases is the more recent one, the one to use. */
        String alias0 = getAlias(handler, 0, false);
        String alias1 = getAlias(handler, 1, false);
        String alias0MC = getAlias(handler, 0, true);
        String alias1MC = getAlias(handler, 1, true);
        Date aliasDate0 = mKeyStore.getCreationDate(alias0);
        Date aliasDate1 = mKeyStore.getCreationDate(alias1);
        Date aliasDate0MC = mKeyStore.getCreationDate(alias0MC);
        Date aliasDate1MC = mKeyStore.getCreationDate(alias1MC);
        int index = 0, indexMC = 0;
        String alias = alias0;
        if (aliasDate1 != null && aliasDate1.after(aliasDate0)) {
            index = 1;
            alias = alias1;
        }
        if (aliasDate1MC != null && aliasDate1MC.after(aliasDate0MC)) {
            indexMC = 1;
        }

        /* If it's the first time we use the preferred handler, create the alias. */
        if (mCryptoHandlers.isEmpty() && !mKeyStore.containsAlias(alias)) {
            AppCenterLog.debug(LOG_TAG, "Creating alias: " + alias);
            handler.generateKey(mCryptoFactory, alias, mContext);
        }

        /* Register the handler. */
        AppCenterLog.debug(LOG_TAG, "Using " + alias);
        mCryptoHandlers.put(handler.getAlgorithm(), new CryptoHandlerEntry(index, indexMC, handler));
    }

    @NonNull
    private String getAlias(@NonNull CryptoHandler handler, int index, boolean mobileCenterFallback) {
        return (mobileCenterFallback ? KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER : KEYSTORE_ALIAS_PREFIX) + ALIAS_SEPARATOR + index + ALIAS_SEPARATOR + handler.getAlgorithm();
    }

    /**
     * Get key store entry for the corresponding handler.
     */
    @Nullable
    private KeyStore.Entry getKeyStoreEntry(@NonNull CryptoHandlerEntry handlerEntry) throws Exception {
        return getKeyStoreEntry(handlerEntry.mCryptoHandler, handlerEntry.mAliasIndex, false);
    }

    @Nullable
    private KeyStore.Entry getKeyStoreEntry(CryptoHandler cryptoHandler, int aliasIndex, boolean mobileCenterFailOver) throws Exception {
        if (mKeyStore == null) {
            return null;
        }
        String alias = getAlias(cryptoHandler, aliasIndex, mobileCenterFailOver);
        return mKeyStore.getEntry(alias, null);
    }

    /**
     * Encrypt data.
     *
     * @param data data to encrypt.
     * @return encrypted data, or original data on internal failure or if null.
     */
    @Nullable
    public String encrypt(@Nullable String data) {
        if (data == null) {
            return null;
        }
        try {

            /* Get preferred crypto handler. */
            CryptoHandlerEntry handlerEntry = mCryptoHandlers.values().iterator().next();
            CryptoHandler handler = handlerEntry.mCryptoHandler;
            try {

                /* Attempt encryption. */
                KeyStore.Entry keyStoreEntry = getKeyStoreEntry(handlerEntry);
                byte[] encryptedBytes = handler.encrypt(mCryptoFactory, mApiLevel, keyStoreEntry, data.getBytes(CHARSET));
                String encryptedString = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);

                /*
                 * Store algorithm for crypto agility alongside the data.
                 * We also use that information in decrypt in case of firmware/sdk upgrade.
                 */
                return handler.getAlgorithm() + ALGORITHM_DATA_SEPARATOR + encryptedString;
            } catch (InvalidKeyException e) {

                /* When key expires, switch to another alias. */
                AppCenterLog.debug(LOG_TAG, "Alias expired: " + handlerEntry.mAliasIndex);
                handlerEntry.mAliasIndex ^= 1;
                String newAlias = getAlias(handler, handlerEntry.mAliasIndex, false);

                /* If this is the second time we switch, we delete the previous key. */
                if (mKeyStore.containsAlias(newAlias)) {
                    AppCenterLog.debug(LOG_TAG, "Deleting alias: " + newAlias);
                    mKeyStore.deleteEntry(newAlias);
                }

                /* Generate new key. */
                AppCenterLog.debug(LOG_TAG, "Creating alias: " + newAlias);
                handler.generateKey(mCryptoFactory, newAlias, mContext);

                /* And encrypt using that new key. */
                return encrypt(data);
            }
        } catch (Exception e) {

            /* Return data as is. */
            AppCenterLog.error(LOG_TAG, "Failed to encrypt data.");
            return data;
        }
    }

    /**
     * Decrypt data.
     *
     * @param data                 data to decrypt.
     * @param mobileCenterFailOver if true, uses Mobile Center keystore instead of App Center keystore when false.
     * @return decrypted data.
     */
    @NonNull
    public DecryptedData decrypt(@Nullable String data, boolean mobileCenterFailOver) {

        /* Handle null for convenience. */
        if (data == null) {
            return new DecryptedData(null, null);
        }

        /* Guess what algorithm was used in case the data was encrypted using an old SDK or old firmware. */
        String[] dataSplit = data.split(ALGORITHM_DATA_SEPARATOR);
        CryptoHandlerEntry handlerEntry = dataSplit.length == 2 ? mCryptoHandlers.get(dataSplit[0]) : null;
        CryptoHandler cryptoHandler = handlerEntry == null ? null : handlerEntry.mCryptoHandler;
        if (cryptoHandler == null) {

            /* Return data as is. */
            AppCenterLog.error(LOG_TAG, "Failed to decrypt data.");
            return new DecryptedData(data, null);
        }

        /* Try the current alias. */
        try {
            return getDecryptedData(cryptoHandler, handlerEntry.mAliasIndex, dataSplit[1], mobileCenterFailOver);
        } catch (Exception e) {

            /* Try the expired alias. */
            try {
                return getDecryptedData(cryptoHandler, handlerEntry.mAliasIndex ^ 1, dataSplit[1], mobileCenterFailOver);
            } catch (Exception e2) {

                /* Return data as is on failure. We cannot log details for security. */
                AppCenterLog.error(LOG_TAG, "Failed to decrypt data.");
                return new DecryptedData(data, null);
            }
        }
    }

    @NonNull
    private DecryptedData getDecryptedData(CryptoHandler cryptoHandler, int aliasIndex, String data, boolean mobileCenterFailOver) throws Exception {
        KeyStore.Entry keyStoreEntry = getKeyStoreEntry(cryptoHandler, aliasIndex, mobileCenterFailOver);
        byte[] decryptedBytes = cryptoHandler.decrypt(mCryptoFactory, mApiLevel, keyStoreEntry, Base64.decode(data, Base64.DEFAULT));
        String decryptedString = new String(decryptedBytes, CHARSET);
        String newEncryptedData = null;
        if (cryptoHandler != mCryptoHandlers.values().iterator().next().mCryptoHandler) {
            newEncryptedData = encrypt(decryptedString);
        }
        return new DecryptedData(decryptedString, newEncryptedData);
    }

    /**
     * Crypto factory.
     */
    interface ICryptoFactory {

        /**
         * Adapt {@link KeyGenerator#getInstance(String, Provider)}.
         */
        IKeyGenerator getKeyGenerator(String algorithm, String provider) throws Exception;

        /**
         * Adapt {@link Cipher#getInstance(String, Provider)}.
         */
        ICipher getCipher(String algorithm, String provider) throws Exception;
    }

    /**
     * Adapter for KeyGenerator.
     */
    interface IKeyGenerator {

        /**
         * Adapt {@link KeyGenerator#init(AlgorithmParameterSpec)}.
         */
        void init(AlgorithmParameterSpec parameters) throws Exception;

        /**
         * Adapt {@link KeyGenerator#generateKey()}.
         */
        void generateKey();
    }

    /**
     * Adapter for cipher.
     */
    interface ICipher {

        /**
         * Adapt {@link Cipher#init(int, Key)}.
         */
        void init(int opMode, Key key) throws Exception;

        /**
         * Adapt {@link Cipher#init(int, Key, AlgorithmParameterSpec)}.
         */
        @SuppressWarnings("SameParameterValue")
        void init(int opMode, Key key, AlgorithmParameterSpec params) throws Exception;

        /**
         * Adapt {@link Cipher#doFinal(byte[])}.
         */
        byte[] doFinal(byte[] input) throws Exception;

        /**
         * Adapt {@link Cipher#doFinal(byte[], int, int)}.
         */
        byte[] doFinal(byte[] input, int inputOffset, int inputLength) throws Exception;

        /**
         * Adapt {@link Cipher#getIV()}}.
         */
        byte[] getIV();

        /**
         * Adapt {@link Cipher#getBlockSize()}.
         */
        int getBlockSize();

        /**
         * Adapt {@link Cipher#getAlgorithm()}.
         */
        @VisibleForTesting
        String getAlgorithm();

        /**
         * Adapt {@link Cipher#getProvider()}.
         */
        @VisibleForTesting
        String getProvider();
    }

    /**
     * Structure for the register handler entries.
     */
    @VisibleForTesting
    static class CryptoHandlerEntry {

        /**
         * Crypto handler.
         */
        final CryptoHandler mCryptoHandler;

        /**
         * Current keystore alias index, 0 or 1.
         */
        int mAliasIndex;

        /**
         * Fallback Mobile Center Key Store alias index, 0 or 1.
         */
        final int mAliasIndexMC;

        /**
         * Init.
         */
        CryptoHandlerEntry(int aliasIndex, int aliasIndexMC, CryptoHandler cryptoHandler) {
            mAliasIndex = aliasIndex;
            mAliasIndexMC = aliasIndexMC;
            mCryptoHandler = cryptoHandler;
        }
    }

    /**
     * Decrypted data returned by {@link #decrypt(String, boolean)}.
     */
    public static class DecryptedData {

        /**
         * Decrypted data.
         */
        final String mDecryptedData;

        /**
         * Better encrypted data.
         */
        final String mNewEncryptedData;

        /**
         * Init.
         *
         * @param decryptedData    decrypted data.
         * @param newEncryptedData new encrypted data that should be replace previously encrypted data.
         */
        @VisibleForTesting
        public DecryptedData(String decryptedData, String newEncryptedData) {
            mDecryptedData = decryptedData;
            mNewEncryptedData = newEncryptedData;
        }

        /**
         * Get decrypted data.
         *
         * @return decrypted data, or original data if null or failed to decrypt.
         */
        public String getDecryptedData() {
            return mDecryptedData;
        }

        /**
         * Get new encrypted data.
         *
         * @return new encrypted data to use if input was encrypted using an old cipher, or null.
         */
        public String getNewEncryptedData() {
            return mNewEncryptedData;
        }
    }
}
