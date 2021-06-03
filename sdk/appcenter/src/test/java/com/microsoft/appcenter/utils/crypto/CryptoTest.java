/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.crypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyExpiredException;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.AES_KEY_SIZE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ALGORITHM_DATA_SEPARATOR;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CIPHER_AES;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CIPHER_RSA;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.RSA_KEY_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressLint("NewApi")
@PowerMockIgnore({"javax.security.auth.x500.*"})
@PrepareForTest({KeyStore.class, KeyPairGenerator.class, Base64.class, CryptoUtils.class, CryptoRsaHandler.class, CryptoAesHandler.class, CryptoAesAndEtmHandler.class, MessageDigest.class})
public class CryptoTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private KeyStore mKeyStore;

    @Mock
    private X509Certificate mRsaCert;

    @Mock
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    private android.security.KeyPairGeneratorSpec.Builder mRsaBuilder;

    @Mock
    private KeyGenParameterSpec.Builder mAesAndEtmBuilder;

    @Mock
    private Context mContext;

    @Mock
    private CryptoUtils.ICryptoFactory mCryptoFactory;

    @Mock
    private CryptoUtils.ICipher mCipher;

    @Before
    @SuppressWarnings({"deprecation", "WrongConstant", "RedundantSuppression"})
    public void setUp() throws Exception {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        mockStatic(KeyStore.class);
        mockStatic(KeyPairGenerator.class);
        mockStatic(Base64.class);
        when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws CharacterCodingException {
                CharsetDecoder decoder = StandardCharsets.ISO_8859_1.newDecoder();
                return decoder.decode(ByteBuffer.wrap((byte[])invocation.getArguments()[0]))
                        .toString();
            }
        });
        when(Base64.decode(anyString(), anyInt())).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) throws UnsupportedEncodingException, CharacterCodingException {
                CharsetEncoder encoder = StandardCharsets.ISO_8859_1.newEncoder();
                return encoder.encode(CharBuffer.wrap(invocation.getArguments()[0].toString()))
                        .array();
            }
        });
        when(KeyStore.getInstance(ANDROID_KEY_STORE)).thenReturn(mKeyStore);

        /* Mock some RSA specifics. */
        whenNew(android.security.KeyPairGeneratorSpec.Builder.class).withAnyArguments().thenReturn(mRsaBuilder);
        when(mRsaBuilder.setAlias(anyString())).thenReturn(mRsaBuilder);
        when(mRsaBuilder.setSubject(any(X500Principal.class))).thenReturn(mRsaBuilder);
        when(mRsaBuilder.setStartDate(any(Date.class))).thenReturn(mRsaBuilder);
        when(mRsaBuilder.setEndDate(any(Date.class))).thenReturn(mRsaBuilder);
        when(mRsaBuilder.setSerialNumber(any(BigInteger.class))).thenReturn(mRsaBuilder);
        when(mRsaBuilder.setKeySize(anyInt())).thenReturn(mRsaBuilder);
        when(KeyPairGenerator.getInstance(anyString(), anyString())).thenReturn(mock(KeyPairGenerator.class));
        KeyStore.PrivateKeyEntry rsaKey = mock(KeyStore.PrivateKeyEntry.class);
        when(mKeyStore.getEntry(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return String.valueOf(argument).contains(CIPHER_RSA);
            }
        }), any(KeyStore.ProtectionParameter.class))).thenReturn(rsaKey);
        when(rsaKey.getCertificate()).thenReturn(mRsaCert);
        when(mCipher.doFinal(any(byte[].class))).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                return (byte[]) invocation.getArguments()[0];
            }
        });

        /* Mock some AES specifics. */
        KeyStore.SecretKeyEntry aesAndEtmKey = mock(KeyStore.SecretKeyEntry.class);
        byte[] array = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        SecretKey secretKeyMock = new SecretKeySpec(array, "AES");
        when(aesAndEtmKey.getSecretKey()).thenReturn(secretKeyMock);
        whenNew(KeyGenParameterSpec.Builder.class).withAnyArguments().thenReturn(mAesAndEtmBuilder);
        when(mAesAndEtmBuilder.setBlockModes(anyString())).thenReturn(mAesAndEtmBuilder);
        when(mAesAndEtmBuilder.setEncryptionPaddings(anyString())).thenReturn(mAesAndEtmBuilder);
        when(mAesAndEtmBuilder.setKeySize(anyInt())).thenReturn(mAesAndEtmBuilder);
        when(mAesAndEtmBuilder.setKeyValidityForOriginationEnd(any(Date.class))).thenReturn(mAesAndEtmBuilder);
        when(mAesAndEtmBuilder.build()).thenReturn(mock(KeyGenParameterSpec.class));
        when(mKeyStore.getEntry(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return String.valueOf(argument).contains(CIPHER_AES);
            }
        }), any(KeyStore.ProtectionParameter.class))).thenReturn(aesAndEtmKey);
        when(mCryptoFactory.getKeyGenerator(anyString(), anyString())).thenReturn(mock(CryptoUtils.IKeyGenerator.class));
        final byte[] mockInitVector = new byte[16];
        when(mCipher.getBlockSize()).thenReturn(mockInitVector.length);
        when(mCipher.getIV()).thenReturn(mockInitVector);
        when(mCipher.doFinal(any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                byte[] input = (byte[]) invocation.getArguments()[0];
                int offset = (int) invocation.getArguments()[1];
                int length = (int) invocation.getArguments()[2];
                byte[] data = new byte[length];

                /*
                 * This answer is called when trying to change it again using when().
                 * Need to check for null (any() returns null).
                 */
                if (input != null) {
                    System.arraycopy(input, offset, data, 0, length);
                }
                return data;
            }
        });

        /* Mock ciphers. */
        when(mCryptoFactory.getCipher(anyString(), anyString())).thenReturn(mCipher);
    }

    @Test
    public void initCryptoConstants() {
        new CryptoConstants();
    }

    @Test
    public void nullData() {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        assertNull(cryptoUtils.encrypt(null));
        CryptoUtils.DecryptedData nullDecryptedData = cryptoUtils.decrypt(null);
        assertNull(nullDecryptedData.getDecryptedData());
        assertNull(nullDecryptedData.getNewEncryptedData());
        nullDecryptedData = cryptoUtils.decrypt(null);
        assertNull(nullDecryptedData.getDecryptedData());
        assertNull(nullDecryptedData.getNewEncryptedData());
    }

    private void verifyNoCrypto(int apiLevel) {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        String encrypted = cryptoUtils.encrypt("anything");
        assertEquals("None" + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    @Test
    public void keyStoreNotFound() throws Exception {
        when(KeyStore.getInstance(ANDROID_KEY_STORE)).thenThrow(new KeyStoreException());
        verifyNoCrypto(Build.VERSION_CODES.LOLLIPOP);
        verifyStatic();
        KeyStore.getInstance(anyString());
    }

    @Test
    public void rsaFailsToLoadWhenPreferred() throws Exception {
        when(KeyPairGenerator.getInstance(anyString(), anyString())).thenThrow(new NoSuchAlgorithmException());
        verifyNoCrypto(Build.VERSION_CODES.LOLLIPOP);
        verifyStatic();
        KeyStore.getInstance(anyString());
    }

    @Test
    public void aesAndEtmFailsToLoadWhenPreferred() throws Exception {
        when(mCryptoFactory.getKeyGenerator(anyString(), anyString())).thenThrow(new NoSuchAlgorithmException());
        verifyRsaPreferred(Build.VERSION_CODES.M);
    }

    @Test
    public void decryptUnknownAlgorithm() {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        CryptoUtils.DecryptedData data = cryptoUtils.decrypt("rot13:caesar");
        assertEquals("rot13:caesar", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt("rot13:caesar");
        assertEquals("rot13:caesar", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt(":");
        assertEquals(":", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt(":");
        assertEquals(":", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
    }

    @Test
    public void failsToEncryptWithBadPadding() throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.LOLLIPOP);
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new BadPaddingException());
        String data = "anythingThatWouldMakeTheCipherFailForSomeReason";
        String encryptedData = cryptoUtils.encrypt(data);
        assertEquals(data, encryptedData);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    @Test
    public void failsToEncryptWithInvalidKey() throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new InvalidKeyException());
        String data = "anythingThatWouldMakeTheCipherFailForSomeReason";
        String encryptedData = cryptoUtils.encrypt(data);
        assertEquals(data, encryptedData);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    @Test
    public void failsToDecrypt() throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        String data = "anythingThatWouldMakeTheCipherFailForSomeReason";
        String encryptedData = cryptoUtils.encrypt(data);
        assertNotEquals(data, encryptedData);
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new BadPaddingException());
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData);

        /* Check decryption failed (data returned as is). */
        assertEquals(encryptedData, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    @Test
    public void readExpiredDataOnAfterAndroidM() throws Exception {

        /* Encrypt test data. */
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        String data = "oldData";
        String encryptedData = cryptoUtils.encrypt(data);

        /* Make key rotate on next encryption. */
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new KeyExpiredException()).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                return (byte[]) invocation.getArguments()[0];
            }
        });
        cryptoUtils.encrypt("otherData");

        /*
         * Make decrypt fail with current key and work with expired key (i.e. the second call).
         */
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new BadPaddingException()).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                byte[] input = (byte[]) invocation.getArguments()[0];
                return input;
            }
        });
        int expectedKeyStoreCalls = 3;
        verify(mKeyStore, times(expectedKeyStoreCalls)).getEntry(notNull(String.class), isNull(KeyStore.ProtectionParameter.class));

        /* Verify we can decrypt with retry on expired key. */
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Verify the second alias was picked for decryption. */
        expectedKeyStoreCalls += 2;
        ArgumentCaptor<String> aliasCaptor = ArgumentCaptor.forClass(String.class);
        verify(mKeyStore, times(expectedKeyStoreCalls)).getEntry(aliasCaptor.capture(), isNull(KeyStore.ProtectionParameter.class));
        List<String> aliases = aliasCaptor.getAllValues();

        /* Check last calls: first we tried to read with the second alias (after rotation). */
        assertTrue(aliases.get(3).startsWith("appcenter.1."));

        /* Then we tried with the old one. */
        assertTrue(aliases.get(4).startsWith("appcenter.0."));
    }

    @Test
    public void readExpiredDataOnBeforeAndroidM() throws Exception {

        /* Encrypt test data. */
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.LOLLIPOP);
        String data = "oldData";
        String encryptedData = cryptoUtils.encrypt(data);

        /* Make key rotate on next encryption. */
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new InvalidKeyException(new CertificateExpiredException())).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                return (byte[]) invocation.getArguments()[0];
            }
        });
        cryptoUtils.encrypt("otherData");

        /*
         * Make decrypt fail with current key and work with expired key (i.e. the second call).
         */
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new BadPaddingException()).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                return (byte[]) invocation.getArguments()[0];
            }
        });
        int expectedKeyStoreCalls = 3;
        verify(mKeyStore, times(expectedKeyStoreCalls)).getEntry(notNull(String.class), isNull(KeyStore.ProtectionParameter.class));

        /* Verify we can decrypt with retry on expired key. */
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Verify the second alias was picked for decryption. */
        expectedKeyStoreCalls += 2;
        ArgumentCaptor<String> aliasCaptor = ArgumentCaptor.forClass(String.class);
        verify(mKeyStore, times(expectedKeyStoreCalls)).getEntry(aliasCaptor.capture(), isNull(KeyStore.ProtectionParameter.class));
        List<String> aliases = aliasCaptor.getAllValues();

        /* Check last calls: first we tried to read with the second alias (after rotation). */
        assertTrue(aliases.get(3).startsWith("appcenter.1."));

        /* Then we tried with the old one. */
        assertTrue(aliases.get(4).startsWith("appcenter.0."));
    }

    private void verifyRsaPreferred(int apiLevel) throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        String encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Test old data encryption upgrade. */
        CryptoUtils.DecryptedData oldDecryptedData = cryptoUtils.decrypt("None:oldData");
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldData", oldDecryptedData.getNewEncryptedData());
        oldDecryptedData = cryptoUtils.decrypt("None:oldData");
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldData", oldDecryptedData.getNewEncryptedData());

        /* Check we can still read data after expiration. */
        doThrow(new CertificateExpiredException()).doNothing().when(mRsaCert).checkValidity();
        decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* But encrypt will use another cert. */
        encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);

        /* Verify another cert was created. */
        ArgumentCaptor<String> alias = ArgumentCaptor.forClass(String.class);
        verify(mRsaBuilder, times(2)).setAlias(alias.capture());
        String alias0 = alias.getAllValues().get(0);
        String alias1 = alias.getAllValues().get(1);
        assertNotEquals(alias0, alias1);
        verify(mKeyStore).getEntry(alias1, null);

        /* Count how many times alias0 was used to test interactions after more easily... */
        alias = ArgumentCaptor.forClass(String.class);
        verify(mKeyStore, atLeastOnce()).getEntry(alias.capture(), any(KeyStore.ProtectionParameter.class));
        int alias0count = 0;
        for (String aliasValue : alias.getAllValues()) {
            if (aliasValue.equals(alias0)) {
                alias0count++;
            }
        }

        /* If we restart crypto utils it must pick up the second cert. */
        when(mKeyStore.containsAlias(alias0)).thenReturn(true);
        when(mKeyStore.containsAlias(alias1)).thenReturn(true);
        Calendar calendar = Calendar.getInstance();
        when(mKeyStore.getCreationDate(alias0)).thenReturn(calendar.getTime());
        calendar.add(Calendar.YEAR, 1);
        when(mKeyStore.getCreationDate(alias1)).thenReturn(calendar.getTime());
        cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);

        /* Check alias0 no more used and that we used second alias to encrypt that value. */
        verify(mKeyStore, times(alias0count)).getEntry(alias0, null);
        verify(mKeyStore, times(2)).getEntry(alias1, null);

        /* Roll over a second time. */
        doThrow(new CertificateExpiredException()).doNothing().when(mRsaCert).checkValidity();
        encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);

        /* Verify another cert was created with reusing first alias name, deleting old one. */
        alias = ArgumentCaptor.forClass(String.class);
        verify(mRsaBuilder, times(3)).setAlias(alias.capture());
        assertNotEquals(alias0, alias1);
        assertEquals(alias0, alias.getAllValues().get(2));
        verify(mKeyStore).deleteEntry(alias0);
        verify(mKeyStore, times(alias0count + 1)).getEntry(alias0, null);
        verify(mKeyStore, times(3)).getEntry(alias1, null);

        /* Check that it will reload alias0 again after restart. */
        calendar.add(Calendar.YEAR, 1);
        when(mKeyStore.getCreationDate(alias0)).thenReturn(calendar.getTime());
        cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);
        verify(mKeyStore, times(alias0count + 2)).getEntry(alias0, null);
        verify(mKeyStore, times(3)).getEntry(alias1, null);
    }

    @Test
    public void rsaPreferredInLolipop() throws Exception {
        verifyRsaPreferred(Build.VERSION_CODES.LOLLIPOP);
    }

    @Test
    public void aesPreferredInM() throws Exception {

        /* Encrypt. */
        String sourceText = "anything";
        when(mCipher.getIV()).thenReturn(new byte[16]);
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        String encrypted = cryptoUtils.encrypt(sourceText);

        /* The init vector is encoded alongside data, in the mock setup it's just a word. */
        String expectedPrefix = CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256 + ALGORITHM_DATA_SEPARATOR;
        assertTrue(encrypted.contains(expectedPrefix) && encrypted.contains(sourceText));
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals(sourceText, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted);
        assertEquals(sourceText, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Test old data encryption upgrade. */
        String oldSourceText = "oldData";
        CryptoUtils.DecryptedData oldDecryptedData = cryptoUtils.decrypt("None:" + oldSourceText);
        assertEquals(oldSourceText, oldDecryptedData.getDecryptedData());
        assertTrue(oldDecryptedData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedData.getNewEncryptedData().contains(oldSourceText));
        oldDecryptedData = cryptoUtils.decrypt("None:" + oldSourceText);
        assertEquals(oldSourceText, oldDecryptedData.getDecryptedData());
        assertTrue(oldDecryptedData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedData.getNewEncryptedData().contains(oldSourceText));

        /* Test old etm data encryption upgrade. */
        String oldAesSourceText = "oldAesData";
        when(mCipher.getBlockSize()).thenReturn(2);
        CryptoUtils.DecryptedData oldDecryptedAesData = cryptoUtils.decrypt(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + oldAesSourceText);
        assertEquals(oldAesSourceText, oldDecryptedAesData.getDecryptedData());
        assertTrue(oldDecryptedAesData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedAesData.getNewEncryptedData().contains(oldAesSourceText));
        oldDecryptedAesData = cryptoUtils.decrypt(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + oldAesSourceText);
        String t = oldDecryptedAesData.getDecryptedData();
        assertEquals(oldAesSourceText, oldDecryptedAesData.getDecryptedData());
        assertTrue(oldDecryptedAesData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedAesData.getNewEncryptedData().contains(oldAesSourceText));

        /* Test old rsa data encryption upgrade. */
        String oldRsaSourceText = "oldRsaData";
        CryptoUtils.DecryptedData oldDecryptedRsaData = cryptoUtils.decrypt(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + oldRsaSourceText);
        assertEquals(oldRsaSourceText, oldDecryptedRsaData.getDecryptedData());
        assertTrue(oldDecryptedRsaData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedRsaData.getNewEncryptedData().contains(oldRsaSourceText));
        oldDecryptedRsaData = cryptoUtils.decrypt(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + oldRsaSourceText);
        assertEquals(oldRsaSourceText, oldDecryptedRsaData.getDecryptedData());
        assertTrue(oldDecryptedRsaData.getNewEncryptedData().contains(expectedPrefix) && oldDecryptedRsaData.getNewEncryptedData().contains(oldRsaSourceText));

        /* Verify we created the alias only for AES, RSA is read only with existing aliases only. */
        ArgumentCaptor<String> alias = ArgumentCaptor.forClass(String.class);
        verify(mKeyStore).containsAlias(alias.capture());
        assertTrue(alias.getValue().contains(CIPHER_AES));
    }

    @Test
    public void verifyEncryptAes() throws Exception {

        // Mock key store.
        when(mKeyStore.containsAlias(anyString())).thenReturn(false);

        // Mock IV value.
        when(mCipher.getIV()).thenReturn("IV".getBytes());

        // Mock collection of handlers.
        String algorithmName = CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
        LinkedHashMap mockCryptoHandlers = spy(LinkedHashMap.class);
        whenNew(LinkedHashMap.class).withNoArguments().thenReturn(mockCryptoHandlers);
        when(mockCryptoHandlers.isEmpty()).thenReturn(true);

        // Start encryption.
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);

        // Remove first algorithm.
        mockCryptoHandlers.remove(algorithmName);

        // Start encryption.
        String sourceText = "Some text!";
        String encryptedText = cryptoUtils.encrypt(sourceText);

        // Verify that data was encoded.
        String expectedText = CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + sourceText;
        assertEquals(encryptedText, expectedText);

        // Verify that data was decoded.
        when(mCipher.getBlockSize()).thenReturn(2);
        String decryptionString = cryptoUtils.decrypt(encryptedText).mDecryptedData;
        assertEquals(decryptionString, sourceText);
    }

    @Test
    public void verifyEncryptAesWithEtm() {

        // Mock IV value.
        byte[] mockIv = new byte[16];
        when(mCipher.getIV()).thenReturn(mockIv);

        // Mock secret key.
        KeyStore.SecretKeyEntry mockSecretKey = mock(KeyStore.SecretKeyEntry.class);
        byte[] key = new byte[16];
        SecretKey secretKeyMock = new SecretKeySpec(key, "AES");
        when(mockSecretKey.getSecretKey()).thenReturn(secretKeyMock);

        // Start encryption.
        String sourceText = "Some text!";
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        String encryptedText = cryptoUtils.encrypt(sourceText);

        // Verify that data was encoded.
        String expectedPrefix = CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256 + ALGORITHM_DATA_SEPARATOR;
        assertNotEquals(encryptedText, sourceText);
        assertTrue(encryptedText.contains(expectedPrefix));
        assertTrue(encryptedText.contains(sourceText));

        // Verify that data was decoded.
        String decryptionString = cryptoUtils.decrypt(encryptedText).mDecryptedData;
        assertEquals(decryptionString, sourceText);
    }

    @Test
    public void verifyExceptionDuringCreateSubkeyWhenOutputLengthLessOne() throws Exception {

        // Mock IV value.
        byte[] mockIv = new byte[16];
        when(mCipher.getIV()).thenReturn(mockIv);

        // Mock collection of handlers.
        String algorithmName = CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
        LinkedHashMap mockCryptoHandlers = spy(LinkedHashMap.class);
        whenNew(LinkedHashMap.class).withNoArguments().thenReturn(mockCryptoHandlers);
        when(mockCryptoHandlers.isEmpty()).thenReturn(true);

        // Mock secret key.
        KeyStore.SecretKeyEntry mockSecretKey = mock(KeyStore.SecretKeyEntry.class);
        byte[] key = new byte[16];
        SecretKey secretKeyMock = new SecretKeySpec(key, "AES");
        when(mockSecretKey.getSecretKey()).thenReturn(secretKeyMock);

        // Start encryption.
        String sourceText = "Some text!";
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);

        // Mock generate subkeys method.
        final CryptoAesAndEtmHandler spyCryptoAesAndEtmHandler = spy((CryptoAesAndEtmHandler) ((CryptoUtils.CryptoHandlerEntry) Objects.requireNonNull(mockCryptoHandlers.get(algorithmName))).mCryptoHandler);
        final int spyAlias = ((CryptoUtils.CryptoHandlerEntry) Objects.requireNonNull(mockCryptoHandlers.get(algorithmName))).mAliasIndex;
        mockCryptoHandlers.replace(algorithmName, new CryptoUtils.CryptoHandlerEntry(spyAlias, spyCryptoAesAndEtmHandler));
        doAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                spyCryptoAesAndEtmHandler.getSubkey((SecretKey)invocation.getArguments()[0], 0);
                return new byte[0];
            }
        }).when(spyCryptoAesAndEtmHandler).getSubkey(Matchers.<SecretKey>any(), eq(16));

        // Start encryption.
        String encryptedText = cryptoUtils.encrypt(sourceText);

        // Verify that data was encoded.
        assertEquals(encryptedText, sourceText);
    }

    @Test
    public void verifyExceptionDuringCreateSubkeyWithTooMuchOperations() throws Exception {

        // Mock IV value.
        byte[] mockIv = new byte[16];
        when(mCipher.getIV()).thenReturn(mockIv);

        // Mock collection of handlers.
        String algorithmName = CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256;
        LinkedHashMap mockCryptoHandlers = spy(LinkedHashMap.class);
        whenNew(LinkedHashMap.class).withNoArguments().thenReturn(mockCryptoHandlers);
        when(mockCryptoHandlers.isEmpty()).thenReturn(true);

        // Mock secret key.
        KeyStore.SecretKeyEntry mockSecretKey = mock(KeyStore.SecretKeyEntry.class);
        byte[] key = new byte[16];
        SecretKey secretKeyMock = new SecretKeySpec(key, "AES");
        when(mockSecretKey.getSecretKey()).thenReturn(secretKeyMock);

        // Start encryption.
        String sourceText = "Some text!";
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);

        // Mock generate subkeys method.
        final CryptoAesAndEtmHandler spyCryptoAesAndEtmHandler = spy((CryptoAesAndEtmHandler) ((CryptoUtils.CryptoHandlerEntry) Objects.requireNonNull(mockCryptoHandlers.get(algorithmName))).mCryptoHandler);
        final int spyAlias = ((CryptoUtils.CryptoHandlerEntry) Objects.requireNonNull(mockCryptoHandlers.get(algorithmName))).mAliasIndex;
        mockCryptoHandlers.replace(algorithmName, new CryptoUtils.CryptoHandlerEntry(spyAlias, spyCryptoAesAndEtmHandler));
        doAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) throws Throwable {
                spyCryptoAesAndEtmHandler.getSubkey((SecretKey)invocation.getArguments()[0], 999999999);
                return new byte[0];
            }
        }).when(spyCryptoAesAndEtmHandler).getSubkey(Matchers.<SecretKey>any(), eq(16));

        // Start encryption.
        String encryptedText = cryptoUtils.encrypt(sourceText);

        // Verify that data was encoded.
        assertEquals(encryptedText, sourceText);
    }

    @Test
    public void verifyDecryptAesWithEtmCouldNotAuthenticateMac() throws Exception {
        verifyExceptionDuringDecryptionAesWithEtm(16, 16, 32, SecurityException.class);
    }

    @Test
    public void verifyDecryptAesWithEtmWhenIvLengthInvalid() throws Exception {
        verifyExceptionDuringDecryptionAesWithEtm(0, 16, 32, IllegalArgumentException.class);
    }

    @Test
    public void verifyDecryptAesWithEtmWhenMacLengthInvalid() throws Exception {
        verifyExceptionDuringDecryptionAesWithEtm(16, 16, 0, IllegalArgumentException.class);
    }

    private void verifyExceptionDuringDecryptionAesWithEtm(int ivLength, int cipherOutputLength, int hMacLength, Class exceptionClass) throws Exception {

        // Mock MessageDigest.
        mockStatic(MessageDigest.class);

        // Prepare data.
        byte[] iv = new byte[ivLength];
        byte[] cipherOutput = new byte[cipherOutputLength];
        byte[] hMac = new byte[hMacLength];

        // Start encryption.
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);

        // Prepare data for decryption.
        ByteBuffer byteBuffer = ByteBuffer.allocate(1 + iv.length + 1 + hMac.length + cipherOutput.length);
        byteBuffer.put((byte) iv.length);
        byteBuffer.put(iv);
        byteBuffer.put((byte) hMac.length);
        byteBuffer.put(hMac);
        byteBuffer.put(cipherOutput);

        // Mock IV.
        when(mCipher.getIV()).thenReturn(iv);

        // Verify that error was thrown.
        String encryptPrefix = CryptoConstants.CIPHER_AES + "/" + AES_KEY_SIZE + "/" + KeyProperties.KEY_ALGORITHM_HMAC_SHA256 + ALGORITHM_DATA_SEPARATOR;
        cryptoUtils.decrypt(encryptPrefix + Base64.encodeToString(byteBuffer.array(), Base64.DEFAULT));
    }
}

