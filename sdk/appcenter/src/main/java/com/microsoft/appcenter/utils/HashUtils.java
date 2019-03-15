/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class relating to hashing.
 */
public class HashUtils {

    private static final char[] HEXADECIMAL_OUTPUT = "0123456789abcdef".toCharArray();

    @VisibleForTesting
    HashUtils() {

        /* Hide constructor in utils pattern. */
    }

    /**
     * Hash data with sha256 and encodeHex output in hexadecimal.
     *
     * @param data data to hash.
     * @return hashed data in hexadecimal output.
     */
    @NonNull
    public static String sha256(@NonNull String data) {
        return sha256(data, "UTF-8");
    }

    @NonNull
    @VisibleForTesting
    static String sha256(@NonNull String data, String charsetName) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(data.getBytes(charsetName));
            return encodeHex(digest.digest());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {

            /*
             * Never happens as every device has UTF-8 support and SHA-256,
             * but if it ever happens make sure we propagate exception as unchecked.
             */
            throw new RuntimeException(e);
        }
    }

    /**
     * Encode a byte array to a string (hexadecimal) representation.
     *
     * @param bytes the bytes to encodeHex.
     * @return the hexadecimal representation.
     */
    @NonNull
    private static String encodeHex(@NonNull byte[] bytes) {
        char[] output = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            output[j * 2] = HEXADECIMAL_OUTPUT[v >>> 4];
            output[j * 2 + 1] = HEXADECIMAL_OUTPUT[v & 0x0F];
        }
        return new String(output);
    }
}
