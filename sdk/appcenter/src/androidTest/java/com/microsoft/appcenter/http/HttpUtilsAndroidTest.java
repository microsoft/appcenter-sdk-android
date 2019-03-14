/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import org.junit.Test;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.cert.CertPathValidatorException;
import java.util.concurrent.RejectedExecutionException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import static com.microsoft.appcenter.http.HttpUtils.isRecoverableError;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        assertTrue(isRecoverableError(new RejectedExecutionException()));
        assertFalse(isRecoverableError(new MalformedURLException()));
        assertFalse(isRecoverableError(new IOException()));
        assertTrue(isRecoverableError(new IOException(new EOFException())));
        assertFalse(isRecoverableError(new IOException(new Exception())));
        for (int i = 0; i <= 4; i++)
            assertTrue(isRecoverableError(new HttpException(500 + i)));
        for (int i = 0; i <= 6; i++)
            assertFalse(isRecoverableError(new HttpException(400 + i)));
        assertTrue(isRecoverableError(new HttpException(408)));
        assertFalse(isRecoverableError(new HttpException(413)));
        assertTrue(isRecoverableError(new HttpException(429)));
        assertTrue(isRecoverableError(new SSLException("Write error: ssl=0x59c28f90: I/O error during system call, Connection timed out")));
        assertFalse(isRecoverableError(new SSLException(null, new CertPathValidatorException("Trust anchor for certification path not found."))));
        assertFalse(isRecoverableError(new SSLException("java.lang.RuntimeException: Unexpected error: java.security.InvalidAlgorithmParameterException: the trustAnchors parameter must be non-empty")));
        assertTrue(isRecoverableError(new SSLException("Read error: ssl=0x9dd07200: I/O error during system call, Connection reset by peer")));
        assertTrue(isRecoverableError(new SSLException("Read error: ssl=0x79cc1e4880: I/O error during system call, Software caused connection abort")));
        assertTrue(isRecoverableError(new SSLException("SSL handshake aborted: ssl=0x1cc160: I/O error during system call, Connection reset by peer")));
        assertTrue(isRecoverableError(new SSLHandshakeException("java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.")));
        assertTrue(isRecoverableError(new SSLHandshakeException("javax.net.ssl.SSLProtocolException: SSL handshake aborted: ssl=0x870c918: Failure in SSL library, usually a protocol error\nerror:14077410:SSL routines:SSL23_GET_SERVER_HELLO:sslv3 alert handshake failure (external/openssl/ssl/s23_clnt.c:658 0xb7c393a1:0x00000000)")));
    }
}
