package com.microsoft.appcenter.utils.crypto;

import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.AES_KEY_SIZE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ENCRYPT_KEY_LIFETIME_IN_YEARS;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static javax.crypto.Cipher.ENCRYPT_MODE;

public class CryptoAesAndEtmHandler implements CryptoHandler {

    private static final int ENCRYPTION_KEY_LENGTH = 16;
    private static final int AUTHENTICATION_KEY_LENGTH = 32;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public String getAlgorithm() {
        return CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void generateKey(CryptoUtils.ICryptoFactory cryptoFactory, String alias, Context context) throws Exception {
        Calendar writeExpiry = Calendar.getInstance();
        writeExpiry.add(Calendar.YEAR, ENCRYPT_KEY_LIFETIME_IN_YEARS);
        CryptoUtils.IKeyGenerator keyGenerator = cryptoFactory.getKeyGenerator(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, ANDROID_KEY_STORE);
        keyGenerator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
                        .setKeyValidityForOriginationEnd(writeExpiry.getTime())
                        .build());
        keyGenerator.generateKey();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public byte[] encrypt(CryptoUtils.ICryptoFactory cryptoFactory, int apiLevel, KeyStore.Entry keyStoreEntry, byte[] input) throws Exception {

        // Get secure key and subkeys.
        byte[] encryptionSubkey = getSubkey(((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey(), ENCRYPTION_KEY_LENGTH);
        byte[] authenticationSubkey = getSubkey(((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey(), AUTHENTICATION_KEY_LENGTH);

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
        byte[] encryptionSubkey = getSubkey(((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey(), ENCRYPTION_KEY_LENGTH);
        byte[] authenticationSubkey = getSubkey(((KeyStore.SecretKeyEntry) keyStoreEntry).getSecretKey(), AUTHENTICATION_KEY_LENGTH);

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

    @RequiresApi(api = Build.VERSION_CODES.M)
    private byte[] getMacBytes(byte[] authKey, byte[] iv, byte[] cipherText) throws InvalidKeyException, NoSuchAlgorithmException {
        SecretKey macSecureKey = new SecretKeySpec(authKey, KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        Mac hMac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        hMac.init(macSecureKey);
        hMac.update(iv);
        hMac.update(cipherText);
        return hMac.doFinal();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @VisibleForTesting
    byte[] getSubkey(@NonNull SecretKey secretKey, int outputDataLength) throws NoSuchAlgorithmException, InvalidKeyException {
        if (outputDataLength < 1) {
            throw new IllegalArgumentException("Output data length must be at more than zero.");
        }

        // Init mac.
        Mac hMac = Mac.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256);
        hMac.init(secretKey);

        // Prepared array and calculate count of iterations.
        int iterations = (int) Math.ceil(((double) outputDataLength) / ((double) hMac.getMacLength()));
        if (iterations > 255) {
            throw new IllegalArgumentException("Output data length must be maximal 255 * hash-length.");
        }

        // Calculate subkey.
        byte[] tempBlock = new byte[0];
        ByteBuffer buffer = ByteBuffer.allocate(outputDataLength);
        int restBytes = outputDataLength;
        int stepSize;
        for (int i = 0; i < iterations; i++) {
            hMac.update(tempBlock);
            hMac.update((byte) (i + 1));
            tempBlock = hMac.doFinal();
            stepSize = Math.min(restBytes, tempBlock.length);
            buffer.put(tempBlock, 0, stepSize);
            restBytes -= stepSize;
        }
        return buffer.array();
    }
}
