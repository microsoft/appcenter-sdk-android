/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.security.KeyStore;
import java.util.Calendar;

import javax.crypto.spec.IvParameterSpec;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.AES_KEY_SIZE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ENCRYPT_KEY_LIFETIME_IN_YEARS;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.PROVIDER_ANDROID_M;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

@RequiresApi(Build.VERSION_CODES.M)
class CryptoAesHandler implements CryptoHandler {

    @Override
    public String getAlgorithm() {
        return CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE;
    }

    @Override
    public void generateKey(CryptoUtils.ICryptoFactory cryptoFactory, String alias, Context context) throws Exception {
        Calendar writeExpiry = Calendar.getInstance();
        writeExpiry.add(Calendar.YEAR, ENCRYPT_KEY_LIFETIME_IN_YEARS);
        CryptoUtils.IKeyGenerator keyGenerator = cryptoFactory.getKeyGenerator(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                .setKeySize(AES_KEY_SIZE)
                .setKeyValidityForOriginationEnd(writeExpiry.getTime())
                .build());
        keyGenerator.generateKey();
    }

    @Override
    public byte[] encrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] input) throws Exception {
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES, PROVIDER_ANDROID_M);
        cipher.init(ENCRYPT_MODE, ((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey());
        byte[] cipherIV = cipher.getIV();
        byte[] output = cipher.doFinal(input);
        byte[] encryptedBytes = new byte[cipherIV.length + output.length];
        System.arraycopy(cipherIV, 0, encryptedBytes, 0, cipherIV.length);
        System.arraycopy(output, 0, encryptedBytes, cipherIV.length, output.length);
        return encryptedBytes;
    }

    @Override
    public byte[] decrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] data) throws Exception {
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES, PROVIDER_ANDROID_M);
        int blockSize = cipher.getBlockSize();
        IvParameterSpec ivParameterSpec = new IvParameterSpec(data, 0, blockSize);
        cipher.init(DECRYPT_MODE, ((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey(), ivParameterSpec);
        return cipher.doFinal(data, blockSize, data.length - blockSize);
    }
}
