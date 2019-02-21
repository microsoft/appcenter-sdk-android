package com.microsoft.appcenter.storage.cosmosdb;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Formatter;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    public static final Base64.Encoder Base64Encoder = java.util.Base64.getEncoder();
    public static final Base64.Decoder Base64Decoder = java.util.Base64.getDecoder();

    // NOTE DateTimeFormatter.RFC_1123_DATE_TIME cannot be used.
    // because cosmos db rfc1123 validation requires two digits for day.
    // so Thu, 04 Jan 2018 00:30:37 GMT is accepted by the cosmos db service,
    // but Thu, 4 Jan 2018 00:30:37 GMT is not.
    // Therefore, we need a custom date time formatter.
    private static final DateTimeFormatter RFC_1123_DATE_TIME = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    private static final ZoneId GMT_ZONE_ID = ZoneId.of("GMT");

    public static String encodeBase64String(byte[] binaryData) {
        String encodedString = Base64Encoder.encodeToString(binaryData);

        if (encodedString.endsWith("\r\n")) {
            encodedString = encodedString.substring(0, encodedString.length() - 2);
        }
        return encodedString;
    }

    /**
     * Returns Current Time in RFC 1123 format, e.g,
     * Fri, 01 Dec 2017 19:22:30 GMT.
     *
     * @return an instance of String
     */
    public static String nowAsRFC1123() {
        ZonedDateTime now = ZonedDateTime.now(GMT_ZONE_ID);
        return Utils.RFC_1123_DATE_TIME.format(now);
    }

    public static String urlEncode(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("failed to encode url " + url, e);
        }
    }

    /**
     * Encryption of a given text using the provided secretKey
     *
     * @param text
     * @param secretKey
     * @return the encoded string
     * @throws SignatureException
     */
    public static byte[] hashMacToByte(String text, byte[] secretKey)
            throws SignatureException {

        try {
            Key sk = new SecretKeySpec(secretKey, HASH_ALGORITHM);
            Mac mac = Mac.getInstance(sk.getAlgorithm());
            mac.init(sk);
            final byte[] hmac = mac.doFinal(text.getBytes());
            return hmac;
        } catch (NoSuchAlgorithmException e1) {
            // throw an exception or pick a different encryption method
            throw new SignatureException(
                    "error building signature, no such algorithm in device "
                            + HASH_ALGORITHM);
        } catch (InvalidKeyException e) {
            throw new SignatureException(
                    "error building signature, invalid key " + HASH_ALGORITHM);
        }
    }

    private static final String HASH_ALGORITHM = "HmacSHA256";
}
