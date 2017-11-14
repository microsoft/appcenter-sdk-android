package com.microsoft.appcenter.utils;

import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

public class HashUtilsTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Test
    public void init() {
        assertNotNull(new HashUtils());
    }

    @Test
    public void sha256() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", HashUtils.sha256(""));
        assertEquals("7efd873c874fbf92d6c3eccc2f24f7eaa349d9d7b512d81ff3f1b44e896362fb", HashUtils.sha256("This hash function rocks!"));
    }

    @Test(expected = RuntimeException.class)
    @PrepareForTest(HashUtils.class)
    public void algorithmNotFound() throws NoSuchAlgorithmException {
        mockStatic(MessageDigest.class);
        NoSuchAlgorithmException cause = new NoSuchAlgorithmException();
        doThrow(cause).when(MessageDigest.class);
        MessageDigest.getInstance(anyString());
        try {
            HashUtils.sha256("");
        } catch (RuntimeException e) {
            assertEquals(cause, e.getCause());
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void utf8NotFound() throws UnsupportedEncodingException {
        try {
            HashUtils.sha256("", "Some Invalid Encoding");
        } catch (RuntimeException e) {
            assertTrue(e.getCause() instanceof UnsupportedEncodingException);
            throw e;
        }
    }
}
