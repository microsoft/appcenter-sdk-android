/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.content.Context;

import java.security.KeyStore;

/**
 * Specification for implementations of cryptographic utilities.
 */
interface CryptoHandler {

    /**
     * Get algorithm to store along encrypted data.
     *
     * @return algorithm to store along encrypted data.
     */
    String getAlgorithm();

    /**
     * Generate a new key.
     *
     * @param cryptoFactory crypto factory.
     * @param alias         keystore alias.
     * @param context       application context.
     * @throws Exception if an error occurs.
     */
    void generateKey(CryptoUtils.ICryptoFactory cryptoFactory, String alias, Context context) throws Exception;

    /**
     * Encrypt data.
     *
     * @param cryptoFactory crypto factory.
     * @param apiLevel      Android API level.
     * @param keyStoreEntry key store.
     * @param data          data to encrypt.
     * @return encrypted bytes.
     * @throws Exception if an error occurs.
     */
    byte[] encrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] data) throws Exception;

    /**
     * Decrypt data.
     *
     * @param cryptoFactory crypto factory.
     * @param apiLevel      Android API level.
     * @param keyStoreEntry key store.
     * @param data          data to decrypt.
     * @return decrypted bytes.
     * @throws Exception if an error occurs.
     */
    byte[] decrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] data) throws Exception;
}
