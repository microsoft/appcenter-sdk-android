/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.http;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.NetworkStateHelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

/**
 * HTTP utilities.
 */
public class HttpUtils {

    /**
     * Thread stats tag for App Center HTTP calls.
     */
    public static final int THREAD_STATS_TAG = 0xD83DDC19;

    /**
     * Read buffer size.
     */
    public static final int READ_BUFFER_SIZE = 1024;

    /**
     * Write buffer size.
     */
    public static final int WRITE_BUFFER_SIZE = 1024;

    /**
     * HTTP connection timeout.
     */
    public static final int CONNECT_TIMEOUT = 10000;

    /**
     * HTTP read timeout.
     */
    public static final int READ_TIMEOUT = 10000;

    /**
     * Maximum characters to be displayed in a log for application secret.
     */
    @VisibleForTesting
    static final int MAX_CHARACTERS_DISPLAYED_FOR_SECRET = 8;

    /**
     * Types of exception that can be retried, no matter what the details are. Sub-classes are included.
     */
    private static final Class[] RECOVERABLE_EXCEPTIONS = {
            EOFException.class,
            InterruptedIOException.class,
            SocketException.class,
            UnknownHostException.class,
            RejectedExecutionException.class
    };
    /**
     * Some transient exceptions can only be detected by interpreting the message...
     */
    private static final Pattern CONNECTION_ISSUE_PATTERN = Pattern.compile("connection (time|reset|abort)|failure in ssl library, usually a protocol error|anchor for certification path not found");

    /**
     * Pattern for token value within ticket header (to replace with * characters).
     */
    private static final Pattern TOKEN_VALUE_PATTERN = Pattern.compile(":[^\"]+");

    /**
     * One Collector Ingestion API key pattern (secret key within the header value).
     */
    private static final Pattern API_KEY_PATTERN = Pattern.compile("-[^,]+(,|$)");

    @VisibleForTesting
    HttpUtils() {
    }

    /**
     * Check whether an exception/error describes a recoverable error or not.
     *
     * @param t exception or error.
     * @return true if the exception/error should be retried, false otherwise.
     */
    public static boolean isRecoverableError(Throwable t) {

        /* Check HTTP exception details. */
        if (t instanceof HttpException) {
            HttpException exception = (HttpException) t;
            int code = exception.getHttpResponse().getStatusCode();
            return code >= 500 || code == 408 || code == 429;
        }

        /* Check for a generic exception to retry. */
        for (Class<?> type : RECOVERABLE_EXCEPTIONS) {
            if (type.isAssignableFrom(t.getClass())) {
                return true;
            }
        }

        /* Check the cause. */
        Throwable cause = t.getCause();
        if (cause != null) {
            for (Class<?> type : RECOVERABLE_EXCEPTIONS) {
                if (type.isAssignableFrom(cause.getClass())) {
                    return true;
                }
            }
        }

        /* Check corner cases. */
        if (t instanceof SSLException) {
            String message = t.getMessage();

            //noinspection RedundantIfStatement simplifying would break adding a new block of code later.
            if (message != null && CONNECTION_ISSUE_PATTERN.matcher(message.toLowerCase(Locale.US)).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hide secret string.
     *
     * @param secret like an app secret or a bearer token.
     * @return obfuscated secret.
     */
    public static String hideSecret(@NonNull String secret) {

        /* Hide secret if string is neither null nor empty string. */
        int hidingEndIndex = secret.length() - (secret.length() >= MAX_CHARACTERS_DISPLAYED_FOR_SECRET ? MAX_CHARACTERS_DISPLAYED_FOR_SECRET : 0);
        char[] fill = new char[hidingEndIndex];
        Arrays.fill(fill, '*');
        return new String(fill) + secret.substring(hidingEndIndex);
    }

    /**
     * Hide secret parts in api keys, expecting One Collector header format.
     *
     * @param apiKeys api keys string header value.
     * @return obfuscated api keys or the original string as is if the format does not match.
     */
    public static String hideApiKeys(@NonNull String apiKeys) {

        /* Replace all secret parts. */
        StringBuilder buffer = new StringBuilder();
        Matcher matcher = API_KEY_PATTERN.matcher(apiKeys);
        int lastEnd = 0;
        while (matcher.find()) {
            buffer.append(apiKeys.substring(lastEnd, matcher.start()));
            buffer.append("-***");

            /* This will be either comma or end of line, thus empty string, for the last key. */
            buffer.append(matcher.group(1));
            lastEnd = matcher.end();
        }
        if (lastEnd < apiKeys.length()) {
            buffer.append(apiKeys.substring(lastEnd));
        }
        return buffer.toString();
    }

    /**
     * Hide token values in Tickets header string, expecting One Collector format.
     *
     * @param tickets tickets string header value.
     * @return obfuscated tickets or the original string as is if the format does not match.
     */
    public static String hideTickets(@NonNull String tickets) {
        return TOKEN_VALUE_PATTERN.matcher(tickets).replaceAll(":***");
    }

    public static HttpClient createHttpClient(@NonNull Context context) {
        return createHttpClient(context, true);
    }

    public static HttpClient createHttpClient(@NonNull Context context, boolean compressionEnabled) {

        /* Retryer should be applied last to avoid retries in offline. */
        return new HttpClientRetryer(createHttpClientWithoutRetryer(context, compressionEnabled));
    }

    public static HttpClient createHttpClientWithoutRetryer(@NonNull Context context, boolean compressionEnabled) {
        HttpClient httpClient = new DefaultHttpClient(compressionEnabled);
        NetworkStateHelper networkStateHelper = NetworkStateHelper.getSharedInstance(context);
        httpClient = new HttpClientNetworkStateHandler(httpClient, networkStateHelper);
        return httpClient;
    }

    /**
     * Create HTTPS connection.
     *
     * @param url a URL.
     * @return instance of {@link HttpsURLConnection}.
     * @throws IOException if connection fails.
     */
    @NonNull
    public static HttpsURLConnection createHttpsConnection(@NonNull URL url) throws IOException {
        if (!"https".equals(url.getProtocol())) {
            throw new IOException("App Center support only HTTPS connection.");
        }
        URLConnection urlConnection = url.openConnection();
        HttpsURLConnection httpsURLConnection;
        if (urlConnection instanceof HttpsURLConnection) {
            httpsURLConnection = (HttpsURLConnection) urlConnection;
        } else {
            throw new IOException("App Center supports only HTTPS connection.");
        }

        /*
         * Make sure we use TLS 1.2 when the device supports it but not enabled by default.
         * Don't hardcode TLS version when enabled by default to avoid unnecessary wrapping and
         * to support future versions of TLS such as say 1.3 without having to patch this code.
         *
         * TLS 1.2 was enabled by default only on Android 5.0:
         * https://developer.android.com/about/versions/android-5.0-changes#ssl
         * https://developer.android.com/reference/javax/net/ssl/SSLSocket#default-configuration-for-different-android-versions
         *
         * There is a problem that TLS 1.2 is still disabled by default on some Samsung devices
         * with API 21, so apply the rule to this API level as well.
         * See https://github.com/square/okhttp/issues/2372#issuecomment-244807676
         */
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            httpsURLConnection.setSSLSocketFactory(new TLS1_2SocketFactory());
        }

        /* Configure connection timeouts. */
        httpsURLConnection.setConnectTimeout(CONNECT_TIMEOUT);
        httpsURLConnection.setReadTimeout(READ_TIMEOUT);
        return httpsURLConnection;
    }
}
