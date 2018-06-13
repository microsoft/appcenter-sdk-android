package com.microsoft.appcenter.http;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import static javax.net.ssl.HttpsURLConnection.getDefaultSSLSocketFactory;

/**
 * This class forces TLS 1.2 protocol via adapter pattern.
 */
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class TLS1_2SocketFactory extends SSLSocketFactory {

    /**
     * Protocols that we allow.
     */
    private static final String[] ENABLED_PROTOCOLS = {"TLSv1.2"};

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
        return getDefaultSSLSocketFactory().getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return getDefaultSSLSocketFactory().getSupportedCipherSuites();
    }

    @Override
    public SSLSocket createSocket() throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket());
    }

    @Override
    public SSLSocket createSocket(String host, int port) throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket(host, port));
    }

    @Override
    public SSLSocket createSocket(InetAddress host, int port) throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket(host, port));
    }

    @Override
    public SSLSocket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket(host, port, localHost, localPort));
    }

    @Override
    public SSLSocket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket(address, port, localAddress, localPort));
    }

    @Override
    public SSLSocket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
        return forceTLS1_2(getDefaultSSLSocketFactory().createSocket(socket, host, port, autoClose));
    }
}
