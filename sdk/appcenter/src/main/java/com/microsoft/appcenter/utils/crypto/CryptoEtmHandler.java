package com.microsoft.appcenter.utils.crypto;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.AES_KEY_SIZE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ENCRYPT_KEY_LIFETIME_IN_YEARS;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.PROVIDER_ANDROID_M;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class CryptoEtmHandler implements CryptoHandler {

    private static final String APPCENTER_SECURE_KEY = "appCenterSecureKey";
    private static final String APPCENTER_SECURE_KEY_IV = APPCENTER_SECURE_KEY + "-iv";
    private static final int SECURE_KEY_LENGTH = 32;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public String getAlgorithm() {
        return CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void generateKey(CryptoUtils.ICryptoFactory cryptoFactory, String alias, Context context) throws Exception {

        // Generate secret key for encoding/decoding other secret key.
        Calendar writeExpiry = Calendar.getInstance();
        writeExpiry.add(Calendar.YEAR, ENCRYPT_KEY_LIFETIME_IN_YEARS);
        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE
        );
        keyGenerator.init(
            new KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeyValidityForOriginationEnd(writeExpiry.getTime())
            .build()
        );
        SecretKey secretKeyForEncryptionKey = keyGenerator.generateKey();

        // Generate secret key.
        byte[] secureKey = new byte[SECURE_KEY_LENGTH];
        (new SecureRandom()).nextBytes(secureKey);

        // Init cipher with secret key for encrypting other secret key.
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES_GCM_NOPADDING, PROVIDER_ANDROID_M);
        cipher.init(ENCRYPT_MODE, secretKeyForEncryptionKey);

        // Encrypt secret key and save to SharedPreferences.
        byte[] cipherOutput = cipher.doFinal(secureKey);
        SharedPreferencesManager.putString(APPCENTER_SECURE_KEY, Base64.encodeToString(cipherOutput, Base64.DEFAULT));
        SharedPreferencesManager.putString(APPCENTER_SECURE_KEY_IV, Base64.encodeToString(cipher.getIV(), Base64.DEFAULT));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private byte[] getSecretKey(CryptoUtils.ICryptoFactory cryptoFactory, KeyStore.Entry keyStoreEntry) throws Exception {

        // Get secret key from keystore.
        SecretKey secretKey = ((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey();

        // Load secret key and iv from SharedPreferences.
        String secretKeyBase64 = SharedPreferencesManager.getString(APPCENTER_SECURE_KEY, null);
        String ivBase64 = SharedPreferencesManager.getString(APPCENTER_SECURE_KEY_IV, null);
        if (TextUtils.isEmpty(secretKeyBase64)) {
            throw new IllegalArgumentException("Secret key shouldn't be null.");
        }
        if (TextUtils.isEmpty(ivBase64)) {
            throw new IllegalArgumentException("IV shouldn't be null.");
        }

        // Init cipher with secret key for encrypting other secret key and decrypt key from SharedPreferences.
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES_GCM_NOPADDING, PROVIDER_ANDROID_M);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, Base64.decode(ivBase64, Base64.DEFAULT)));
        return cipher.doFinal(Base64.decode(secretKeyBase64, Base64.DEFAULT));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public byte[] encrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] input) throws Exception {

        // Get secure key and subkeys.
        byte[] secretKey = getSecretKey(cryptoFactory, keyStoreEntry);
        byte[] encryptionSubkey = getEncryptionSubkey(secretKey);
        byte[] authenticationSubkey = getAuthenticationSubkey(secretKey);

        // Prepared cipher.
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES, null);
        cipher.init(ENCRYPT_MODE, new SecretKeySpec(encryptionSubkey, KeyProperties.KEY_ALGORITHM_AES));
        byte[] cipherIv = cipher.getIV();
        byte[] cipherOutput = cipher.doFinal(input);

        // Calculate mac.
        byte[] hMac = getMacBytes(authenticationSubkey, cipherIv, cipherOutput);

        // Convert data to common message.
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + cipherIv.length + 1 + hMac.length + cipherOutput.length);
        byteBuffer.put((byte) cipherIv.length);
        byteBuffer.put(cipherIv);
        byteBuffer.put((byte) hMac.length);
        byteBuffer.put(hMac);
        byteBuffer.put(cipherOutput);
        return byteBuffer.array();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public byte[] decrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] data) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);

        // Check iv data.
        int ivLength = byteBuffer.get();
        if (ivLength != 16) {
            throw new IllegalArgumentException("Invalid IV length.");
        }
        byte[] iv = new byte[ivLength];
        byteBuffer.get(iv);

        // Check mac data.
        int macLength = (byteBuffer.get());
        if (macLength != 32) {
            throw new IllegalArgumentException("Invalid MAC length.");
        }
        byte[] actualHMac = new byte[macLength];
        byteBuffer.get(actualHMac);

        // Get cipher data.
        byte[] cipherText = new byte[byteBuffer.remaining()];
        byteBuffer.get(cipherText);

        // Get secure key and subkeys.
        byte[] secretKey = getSecretKey(cryptoFactory, keyStoreEntry);
        byte[] encryptionSubkey = getEncryptionSubkey(secretKey);
        byte[] authenticationSubkey = getAuthenticationSubkey(secretKey);

        // Calculate mac.
        byte[] expectedHMac = getMacBytes(authenticationSubkey, iv, cipherText);

        // Verity mac value in the encrypted message.
        if (!MessageDigest.isEqual(expectedHMac, actualHMac)) {
            throw new SecurityException("Could not authenticate MAC value.");
        }

        // Decrypt message.
        CryptoUtils.ICipher cipher = cryptoFactory.getCipher(CryptoConstants.CIPHER_AES, null);
        cipher.init(DECRYPT_MODE, new SecretKeySpec(encryptionSubkey, KeyProperties.KEY_ALGORITHM_AES), new IvParameterSpec(iv));
        return cipher.doFinal(cipherText);
    }

    private byte[] getEncryptionSubkey(byte[] secretKey) {
        return Arrays.copyOfRange(secretKey, 0,secretKey.length / 2);
    }

    private byte[] getAuthenticationSubkey(byte[] secretKey) {
        return Arrays.copyOfRange(secretKey, secretKey.length / 2, secretKey.length);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private byte[] getMacBytes(byte[] authKey, byte[] iv, byte[] cipherText) throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKey macSecureKey = new SecretKeySpec(authKey, KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        Mac hMac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        hMac.init(macSecureKey);
        hMac.update(iv);
        hMac.update(cipherText);
        return hMac.doFinal();
    }
}
