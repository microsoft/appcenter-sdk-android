package com.microsoft.appcenter.utils.crypto;

import android.content.Context;

import org.junit.Test;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import static com.microsoft.appcenter.utils.crypto.CryptoConstants.CHARSET;
import static javax.crypto.Cipher.DECRYPT_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.powermock.api.mockito.PowerMockito.mock;

/**
 * This class covers the default crypto factory implementation.
 * This is separate from other tests as we don't use PowerMockRule here.
 */
public class CryptoDefaultFactoryTest {

    @Test
    public void coverNoOpHandlerGenerate() {
        new CryptoNoOpHandler().generateKey(CryptoUtils.DEFAULT_CRYPTO_FACTORY, null, null);
    }

    @Test
    public void coverSingleton() {
        CryptoUtils instance = CryptoUtils.getInstance(mock(Context.class));
        assertSame(instance, CryptoUtils.getInstance(mock(Context.class)));
        assertSame(CryptoUtils.DEFAULT_CRYPTO_FACTORY, instance.getCryptoFactory());
    }

    @Test
    public void coverDefaultCipherParameterPassing() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        String algorithm = "AES/CBC/PKCS5Padding";
        Cipher cipherTmp = Cipher.getInstance(algorithm);
        String provider = cipherTmp.getProvider().getName();
        CryptoUtils.ICipher encryptCipher = CryptoUtils.DEFAULT_CRYPTO_FACTORY.getCipher(algorithm, provider);
        assertEquals(algorithm, encryptCipher.getAlgorithm());
        assertEquals(provider, encryptCipher.getProvider());
        encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] data = encryptCipher.doFinal("test".getBytes(CHARSET));
        CryptoUtils.ICipher decryptCipher = CryptoUtils.DEFAULT_CRYPTO_FACTORY.getCipher(algorithm, provider);
        decryptCipher.init(DECRYPT_MODE, secretKey, new IvParameterSpec(encryptCipher.getIV()));
        assertEquals("test", new String(decryptCipher.doFinal(data), CHARSET));
        assertEquals("test", new String(decryptCipher.doFinal(data, 0, data.length), CHARSET));
        assertEquals(decryptCipher.getIV().length, decryptCipher.getBlockSize());
    }
}

