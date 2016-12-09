package com.microsoft.azure.mobile.ingestion.http;

import org.junit.Test;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import static com.microsoft.azure.mobile.ingestion.http.HttpUtils.isRecoverableError;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@SuppressWarnings("unused")
public class HttpUtilsAndroidTest {

    @Test
    public void utilsCoverage() {
        new HttpUtils();
    }

    @Test
    public void isRecoverableErrorTest() {
        assertTrue(isRecoverableError(new EOFException()));
        assertTrue(isRecoverableError(new InterruptedIOException()));
        assertTrue(isRecoverableError(new SocketTimeoutException()));
        assertTrue(isRecoverableError(new SocketException()));
        assertTrue(isRecoverableError(new PortUnreachableException()));
        assertTrue(isRecoverableError(new UnknownHostException()));
        assertFalse(isRecoverableError(new MalformedURLException()));
        for (int i = 0; i <= 4; i++)
            assertTrue(isRecoverableError(new HttpException(500 + i)));
        for (int i = 2; i <= 6; i++)
            assertFalse(isRecoverableError(new HttpException(400 + i)));
        assertTrue(isRecoverableError(new HttpException(408)));
        assertFalse(isRecoverableError(new HttpException(413)));
        assertTrue(isRecoverableError(new HttpException(429)));
        assertTrue(isRecoverableError(new HttpException(401)));
        assertTrue(isRecoverableError(new SSLException("Write error: ssl=0x59c28f90: I/O error during system call, Connection timed out")));
        assertFalse(isRecoverableError(new SSLHandshakeException("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.")));
        assertFalse(isRecoverableError(new SSLException(null, new CertPathValidatorException("Trust anchor for certification path not found."))));
        assertFalse(isRecoverableError(new SSLException("java.lang.RuntimeException: Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty")));
        assertTrue(isRecoverableError(new SSLException("Read error: ssl=0x9dd07200: I/O error during system call, Connection reset by peer")));
        assertTrue(isRecoverableError(new SSLException("SSL handshake aborted: ssl=0x1cc160: I/O error during system call, Connection reset by peer")));
    }
}
