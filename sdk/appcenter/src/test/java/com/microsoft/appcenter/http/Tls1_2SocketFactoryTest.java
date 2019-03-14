/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({ HttpsURLConnection.class, SSLContext.class })
@RunWith(PowerMockRunner.class)
public class Tls1_2SocketFactoryTest {

    private static final String[] DEFAULT_CIPHER_SUITES = {"mockCipher1"};

    private static final String[] SUPPORTED_CIPHER_SUITES = {"mockCipher1", "mockCipher2"};

    private TLS1_2SocketFactory getFactory() throws IOException {
        mockStatic(HttpsURLConnection.class);
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        when(HttpsURLConnection.getDefaultSSLSocketFactory()).thenReturn(sslSocketFactory);
        when(sslSocketFactory.getDefaultCipherSuites()).thenReturn(DEFAULT_CIPHER_SUITES);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(SUPPORTED_CIPHER_SUITES);
        SSLSocket sslSocket = mock(SSLSocket.class);
        when(sslSocketFactory.createSocket()).thenReturn(sslSocket);
        when(sslSocketFactory.createSocket(anyString(), anyInt())).thenReturn(sslSocket);
        when(sslSocketFactory.createSocket(any(InetAddress.class), anyInt())).thenReturn(sslSocket);
        when(sslSocketFactory.createSocket(anyString(), anyInt(), any(InetAddress.class), anyInt())).thenReturn(sslSocket);
        when(sslSocketFactory.createSocket(any(Socket.class), anyString(), anyInt(), anyBoolean())).thenReturn(sslSocket);
        when(sslSocketFactory.createSocket(any(InetAddress.class), anyInt(), any(InetAddress.class), anyInt())).thenReturn(sslSocket);
        return new TLS1_2SocketFactory();
    }

    private void checkProtocol(SSLSocket socket) {
        verify(socket).setEnabledProtocols(new String[]{"TLSv1.2"});
    }

    @SuppressWarnings("ObviousNullCheck")
    @Test
    public void createFactory() throws Exception {
        SSLContext sslContext = mock(SSLContext.class);
        doNothing().doThrow(new KeyManagementException())
                .when(sslContext).init(any(KeyManager[].class), any(TrustManager[].class), any(SecureRandom.class));
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslContext.getSocketFactory())
                .thenReturn(sslSocketFactory);
        mockStatic(SSLContext.class);
        when(SSLContext.getInstance("TLSv1.2"))
                .thenReturn(sslContext)
                .thenReturn(sslContext)
                .thenThrow(new NoSuchAlgorithmException());

        /* Mock default factory. */
        mockStatic(HttpsURLConnection.class);
        when(HttpsURLConnection.getDefaultSSLSocketFactory())
                .thenReturn(sslSocketFactory);

        /* Get factory from context. */
        assertNotNull(new TLS1_2SocketFactory());
        verifyStatic(never());
        HttpsURLConnection.getDefaultSSLSocketFactory();

        /* KeyManagementException */
        assertNotNull(new TLS1_2SocketFactory());
        verifyStatic(times(1));
        HttpsURLConnection.getDefaultSSLSocketFactory();

        /* NoSuchAlgorithmException */
        assertNotNull(new TLS1_2SocketFactory());
        verifyStatic(times(2));
        HttpsURLConnection.getDefaultSSLSocketFactory();
    }

    @Test
    public void createSocket() throws Exception {
        checkProtocol(getFactory().createSocket());
        checkProtocol(getFactory().createSocket("localhost", 80));
        checkProtocol(getFactory().createSocket(mock(InetAddress.class), 80));
        checkProtocol(getFactory().createSocket("localhost", 80, mock(InetAddress.class), 8080));
        checkProtocol(getFactory().createSocket(mock(InetAddress.class), 80, mock(InetAddress.class), 8080));
        checkProtocol(getFactory().createSocket(mock(Socket.class), "localhost", 80, true));
        assertArrayEquals(DEFAULT_CIPHER_SUITES, getFactory().getDefaultCipherSuites());
        assertArrayEquals(SUPPORTED_CIPHER_SUITES, getFactory().getSupportedCipherSuites());
    }
}