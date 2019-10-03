/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory;

/**
 * This class forces TLS 1.2 protocol via adapter pattern.
 */
class TLS1_2SocketFactory extends SSLSocketFactory {

    /**
     * TLS 1.2 protocol name.
     */
    private static final String TLS1_2_PROTOCOL = "TLSv1.2";

    /**
     * Protocols that we allow.
     */
    private static final String[] ENABLED_PROTOCOLS = { TLS1_2_PROTOCOL };

    /**
     * Socket factory.
     *
     * Do not rename it! See https://github.com/square/okhttp/issues/2323
     */
    private final SSLSocketFactory delegate;

    TLS1_2SocketFactory() {
        SSLSocketFactory socketFactory = null;
        try {

            /*
             * Explicitly specify protocol for SSL context.
             * See https://www.java.com/en/configure_crypto.html#enableTLSv1_2
             */
            SSLContext sc = SSLContext.getInstance(TLS1_2_PROTOCOL);
            sc.init(null, null, null);
            socketFactory = sc.getSocketFactory();
        } catch (KeyManagementException | NoSuchAlgorithmException ignored) {
        }
        delegate = socketFactory != null ? socketFactory : getDefaultSSLSocketFactory();
    }

    /**
     * Force TLS 1.2 protocol on a socket.
     *
     * @param socket socket.
     * @return that same socket for chaining calls.
     */
    private SSLSocket forceTLS1_2(Socket socket) {
        SSLSocket sslSocket = (SSLSocket) socket;
        sslSocket.setEnabledProtocols(ENABLED_PROTOCOLS);
        return sslSocket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public SSLSocket createSocket() throws IOException {
        return forceTLS1_2(delegate.createSocket());
    }

    @Override
    public SSLSocket createSocket(String host, int port) throws IOException {
        return forceTLS1_2(delegate.createSocket(host, port));
    }

    @Override
    public SSLSocket createSocket(InetAddress host, int port) throws IOException {
        return forceTLS1_2(delegate.createSocket(host, port));
    }

    @Override
    public SSLSocket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return forceTLS1_2(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public SSLSocket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return forceTLS1_2(delegate.createSocket(address, port, localAddress, localPort));
    }

    @Override
    public SSLSocket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return forceTLS1_2(delegate.createSocket(socket, host, port, autoClose));
    }
}
