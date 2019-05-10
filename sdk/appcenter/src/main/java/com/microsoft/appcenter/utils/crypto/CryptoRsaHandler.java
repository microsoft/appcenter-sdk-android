/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CIPHER_RSA;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ENCRYPT_KEY_LIFETIME_IN_YEARS;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.PROVIDER_ANDROID_M;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.PROVIDER_ANDROID_OLD;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.RSA_KEY_SIZE;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

@RequiresApi(Build.VERSION_CODES.KITKAT)
class CryptoRsaHandler implements CryptoHandler {

    @Override
    public String getAlgorithm() {
        return CIPHER_RSA + "/" + RSA_KEY_SIZE;
    }

    /*
     * We don't run this code prior to Android 4.4 hence no 4.3 secure random problem.
     */
    @Override
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    @SuppressLint({"InlinedApi", "TrulyRandom"})
    public void generateKey(CryptoUtils.ICryptoFactory cryptoFactory, String alias, Context context) throws Exception {
        Calendar writeExpiry = Calendar.getInstance();
        writeExpiry.add(Calendar.YEAR, ENCRYPT_KEY_LIFETIME_IN_YEARS);
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
        generator.initialize(new android.security.KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=" + alias))
                .setStartDate(new Date())
                .setEndDate(writeExpiry.getTime())
                .setSerialNumber(BigInteger.TEN)
                .setKeySize(RSA_KEY_SIZE)
                .build());
        generator.generateKeyPair();
    }

    /**
     * Get new cipher.
     */
    private CryptoUtils.ICipher getCipher(CryptoUtils.ICryptoFactory cipherFactory, int apiLevel) throws Exception {
        String provider;
        if (apiLevel >= Build.VERSION_CODES.M) {
            provider = PROVIDER_ANDROID_M;
        } else {
            provider = PROVIDER_ANDROID_OLD;
        }
        return cipherFactory.getCipher(CIPHER_RSA, provider);
    }

    @Override
    public byte[] encrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] input) throws Exception {
        CryptoUtils.ICipher cipher = getCipher(cryptoFactory, apiLevel);
        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStoreEntry;
        X509Certificate certificate = (X509Certificate) privateKeyEntry.getCertificate();
        try {
            certificate.checkValidity();
        } catch (CertificateExpiredException e) {
            throw new InvalidKeyException(e);
        }
        cipher.init(ENCRYPT_MODE, certificate.getPublicKey());
        return cipher.doFinal(input);
    }

    @Override
    public byte[] decrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] data) throws Exception {
        CryptoUtils.ICipher cipher = getCipher(cryptoFactory, apiLevel);
        cipher.init(DECRYPT_MODE, ((KeyStore.PrivateKeyEntry) keyStoreEntry).getPrivateKey());
        return cipher.doFinal(data);
    }
}
