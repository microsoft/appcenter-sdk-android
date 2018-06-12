package com.microsoft.appcenter.utils.crypto;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.util.Base64;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.BadPaddingException;
import javax.security.auth.x500.X500Principal;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.AES_KEY_SIZE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ALGORITHM_DATA_SEPARATOR;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ALIAS_SEPARATOR;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.ANDROID_KEY_STORE;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CIPHER_AES;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CIPHER_RSA;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.KEYSTORE_ALIAS_PREFIX;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER;
import static com.microsoft.appcenter.utils.crypto.CryptoConstants.RSA_KEY_SIZE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
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
@PrepareForTest({KeyStore.class, KeyPairGenerator.class, Base64.class, CryptoUtils.class, CryptoRsaHandler.class, CryptoAesHandler.class})
public class CryptoTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private KeyStore mKeyStore;

    @Mock
    private X509Certificate mRsaCert;

    @Mock
    @SuppressWarnings("deprecation")
    private android.security.KeyPairGeneratorSpec.Builder mRsaBuilder;

    @Mock
    private KeyGenParameterSpec.Builder mAesBuilder;

    @Mock
    private Context mContext;

    @Mock
    private CryptoUtils.ICryptoFactory mCryptoFactory;

    @Mock
    private CryptoUtils.ICipher mCipher;

    @Before
    @SuppressWarnings({"deprecation", "WrongConstant"})
    public void setUp() throws Exception {
        when(mContext.getApplicationContext()).thenReturn(mContext);
        mockStatic(KeyStore.class);
        mockStatic(KeyPairGenerator.class);
        mockStatic(Base64.class);
        when(Base64.encodeToString(any(byte[].class), anyInt())).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) {
                return new String((byte[]) invocation.getArguments()[0]);
            }
        });
        when(Base64.decode(anyString(), anyInt())).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                return invocation.getArguments()[0].toString().getBytes();
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
        KeyStore.SecretKeyEntry aesKey = mock(KeyStore.SecretKeyEntry.class);
        whenNew(KeyGenParameterSpec.Builder.class).withAnyArguments().thenReturn(mAesBuilder);
        when(mAesBuilder.setBlockModes(anyString())).thenReturn(mAesBuilder);
        when(mAesBuilder.setEncryptionPaddings(anyString())).thenReturn(mAesBuilder);
        when(mAesBuilder.setKeySize(anyInt())).thenReturn(mAesBuilder);
        when(mAesBuilder.setKeyValidityForOriginationEnd(any(Date.class))).thenReturn(mAesBuilder);
        when(mAesBuilder.build()).thenReturn(mock(KeyGenParameterSpec.class));
        when(mKeyStore.getEntry(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return String.valueOf(argument).contains(CIPHER_AES);
            }
        }), any(KeyStore.ProtectionParameter.class))).thenReturn(aesKey);
        when(mCryptoFactory.getKeyGenerator(anyString(), anyString())).thenReturn(mock(CryptoUtils.IKeyGenerator.class));
        final byte[] mockInitVector = "IV".getBytes();
        when(mCipher.getBlockSize()).thenReturn(mockInitVector.length);
        when(mCipher.getIV()).thenReturn(mockInitVector);
        when(mCipher.doFinal(any(byte[].class), anyInt(), anyInt())).thenAnswer(new Answer<byte[]>() {

            @Override
            public byte[] answer(InvocationOnMock invocation) {
                byte[] input = (byte[]) invocation.getArguments()[0];
                int offset = (int) invocation.getArguments()[1];
                int length = (int) invocation.getArguments()[2];
                byte[] data = new byte[length];
                System.arraycopy(input, offset, data, 0, length);
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
        CryptoUtils.DecryptedData nullDecryptedData = cryptoUtils.decrypt(null, false);
        assertNull(nullDecryptedData.getDecryptedData());
        assertNull(nullDecryptedData.getNewEncryptedData());
        nullDecryptedData = cryptoUtils.decrypt(null, true);
        assertNull(nullDecryptedData.getDecryptedData());
        assertNull(nullDecryptedData.getNewEncryptedData());
    }

    private void verifyNoCrypto(int apiLevel) {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        String encrypted = cryptoUtils.encrypt("anything");
        assertEquals("None" + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted, false);
        assertEquals("anything", decryptedData.getDecryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted, true);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    @Test
    public void noCryptoInIceCreamSandwich() throws Exception {
        verifyNoCrypto(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        verifyStatic(never());
        KeyStore.getInstance(anyString());
    }

    @Test
    public void keyStoreNotFound() throws Exception {
        when(KeyStore.getInstance(ANDROID_KEY_STORE)).thenThrow(new KeyStoreException());
        verifyNoCrypto(Build.VERSION_CODES.KITKAT);
        verifyStatic();
        KeyStore.getInstance(anyString());
    }

    @Test
    public void rsaFailsToLoadWhenPreferred() throws Exception {
        when(KeyPairGenerator.getInstance(anyString(), anyString())).thenThrow(new NoSuchAlgorithmException());
        verifyNoCrypto(Build.VERSION_CODES.KITKAT);
        verifyStatic();
        KeyStore.getInstance(anyString());
    }

    @Test
    public void aesFailsToLoadWhenPreferred() throws Exception {
        when(mCryptoFactory.getKeyGenerator(anyString(), anyString())).thenThrow(new NoSuchAlgorithmException());
        verifyRsaPreferred(Build.VERSION_CODES.M);
    }

    @Test
    public void decryptUnknownAlgorithm() {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1);
        CryptoUtils.DecryptedData data = cryptoUtils.decrypt("rot13:caesar", false);
        assertEquals("rot13:caesar", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt("rot13:caesar", true);
        assertEquals("rot13:caesar", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt(":", false);
        assertEquals(":", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
        data = cryptoUtils.decrypt(":", true);
        assertEquals(":", data.getDecryptedData());
        assertNull(data.getNewEncryptedData());
    }

    @Test
    public void failsToEncrypt() throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.KITKAT);
        when(mCipher.doFinal(any(byte[].class))).thenThrow(new BadPaddingException());
        String data = "anythingThatWouldMakeTheCipherFailForSomeReason";
        String encryptedData = cryptoUtils.encrypt(data);
        assertEquals(data, encryptedData);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encryptedData, false);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encryptedData, true);
        assertEquals(data, decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
    }

    private void verifyRsaPreferred(int apiLevel) throws Exception {
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, apiLevel);
        String encrypted = cryptoUtils.encrypt("anything");
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "anything", encrypted);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted, false);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted, true);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Test old data encryption upgrade. */
        CryptoUtils.DecryptedData oldDecryptedData = cryptoUtils.decrypt("None:oldData", false);
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldData", oldDecryptedData.getNewEncryptedData());
        oldDecryptedData = cryptoUtils.decrypt("None:oldData", true);
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldData", oldDecryptedData.getNewEncryptedData());

        /* Check we can still read data after expiration. */
        doThrow(new CertificateExpiredException()).doNothing().when(mRsaCert).checkValidity();
        decryptedData = cryptoUtils.decrypt(encrypted, false);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted, true);
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
    public void rsaPreferredInKitKat() throws Exception {
        verifyRsaPreferred(Build.VERSION_CODES.KITKAT);
    }

    @Test
    public void aesPreferredInM() throws Exception {

        /* Encrypt. */
        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.M);
        String encrypted = cryptoUtils.encrypt("anything");

        /* The init vector is encoded alongside data, in the mock setup it's just a word. */
        assertEquals(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + "anything", encrypted);
        CryptoUtils.DecryptedData decryptedData = cryptoUtils.decrypt(encrypted, false);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());
        decryptedData = cryptoUtils.decrypt(encrypted, true);
        assertEquals("anything", decryptedData.getDecryptedData());
        assertNull(decryptedData.getNewEncryptedData());

        /* Test old data encryption upgrade. */
        CryptoUtils.DecryptedData oldDecryptedData = cryptoUtils.decrypt("None:oldData", false);
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + "oldData", oldDecryptedData.getNewEncryptedData());
        oldDecryptedData = cryptoUtils.decrypt("None:oldData", true);
        assertEquals("oldData", oldDecryptedData.getDecryptedData());
        assertEquals(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + "oldData", oldDecryptedData.getNewEncryptedData());
        CryptoUtils.DecryptedData oldDecryptedRsaData = cryptoUtils.decrypt(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldRsaData", false);
        assertEquals("oldRsaData", oldDecryptedRsaData.getDecryptedData());
        assertEquals(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + "oldRsaData", oldDecryptedRsaData.getNewEncryptedData());
        oldDecryptedRsaData = cryptoUtils.decrypt(CIPHER_RSA + "/" + RSA_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "oldRsaData", true);
        assertEquals("oldRsaData", oldDecryptedRsaData.getDecryptedData());
        assertEquals(CIPHER_AES + "/" + AES_KEY_SIZE + ALGORITHM_DATA_SEPARATOR + "IV" + "oldRsaData", oldDecryptedRsaData.getNewEncryptedData());

        /* Verify we created the alias only for AES, RSA is read only with existing aliases only. */
        ArgumentCaptor<String> alias = ArgumentCaptor.forClass(String.class);
        verify(mKeyStore).containsAlias(alias.capture());
        assertTrue(alias.getValue().contains(CIPHER_AES));
    }

    @Test
    public void registerHandlerWithOldMCKeyStore() throws Exception {

        /* Basically pre-constructing the four values that CryptoUtils.getAlias() will return */
        String alias0 = KEYSTORE_ALIAS_PREFIX + ALIAS_SEPARATOR + "0" + ALIAS_SEPARATOR + CIPHER_RSA + "/" + RSA_KEY_SIZE;
        String alias1 = KEYSTORE_ALIAS_PREFIX + ALIAS_SEPARATOR + "1" + ALIAS_SEPARATOR + CIPHER_RSA + "/" + RSA_KEY_SIZE;
        String alias0MC = KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER + ALIAS_SEPARATOR + "0" + ALIAS_SEPARATOR + CIPHER_RSA + "/" + RSA_KEY_SIZE;
        String alias1MC = KEYSTORE_ALIAS_PREFIX_MOBILE_CENTER + ALIAS_SEPARATOR + "1" + ALIAS_SEPARATOR + CIPHER_RSA + "/" + RSA_KEY_SIZE;

        /* Create a calendar which will fall back to the MC aliases */
        Calendar calendar = Calendar.getInstance();
        Date d = calendar.getTime();
        when(mKeyStore.getCreationDate(alias0)).thenReturn(d);
        when(mKeyStore.getCreationDate(alias1)).thenReturn(d);
        when(mKeyStore.getCreationDate(alias0MC)).thenReturn(d);
        calendar.add(Calendar.YEAR, 1);
        d = calendar.getTime();
        when(mKeyStore.getCreationDate(alias1MC)).thenReturn(d);

        CryptoUtils cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.KITKAT);

        assertEquals(0, cryptoUtils.mCryptoHandlers.get(CIPHER_RSA + "/" + RSA_KEY_SIZE).mAliasIndex);
        assertEquals(1, cryptoUtils.mCryptoHandlers.get(CIPHER_RSA + "/" + RSA_KEY_SIZE).mAliasIndexMC);

        /* do it again for sets of dates which are the same */
        when(mKeyStore.getCreationDate(alias0MC)).thenReturn(d);
        when(mKeyStore.getCreationDate(alias1MC)).thenReturn(d);

        cryptoUtils = new CryptoUtils(mContext, mCryptoFactory, Build.VERSION_CODES.KITKAT);

        assertEquals(0, cryptoUtils.mCryptoHandlers.get(CIPHER_RSA + "/" + RSA_KEY_SIZE).mAliasIndex);
        assertEquals(0, cryptoUtils.mCryptoHandlers.get(CIPHER_RSA + "/" + RSA_KEY_SIZE).mAliasIndexMC);
    }
}

