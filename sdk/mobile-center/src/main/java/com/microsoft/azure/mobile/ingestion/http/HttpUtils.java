package com.microsoft.azure.mobile.ingestion.http;

import android.support.annotation.VisibleForTesting;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

/**
 * HTTP utilities.
 */
public final class HttpUtils {

    /**
     * Types of exception that can be retried, no matter what the details are. Sub-classes are included.
     */
    private static final Class[] RECOVERABLE_EXCEPTIONS = {
            EOFException.class,
            InterruptedIOException.class,
            SocketException.class,
            UnknownHostException.class
    };

    /**
     * Some transient exceptions can only be detected by interpreting the message...
     */
    private static final Pattern CONNECTION_ISSUE_PATTERN = Pattern.compile("connection (time|reset)|failure in ssl library, usually a protocol error");

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
            int code = exception.getStatusCode();
            return code >= 500 || code == 408 || code == 429 || code == 401;
        }

        /* Check for a generic exception to retry. */
        for (Class<?> type : RECOVERABLE_EXCEPTIONS)
            if (type.isAssignableFrom(t.getClass()))
                return true;

        /* Check corner cases. */
        if (t instanceof SSLException) {
            String message = t.getMessage();
            if (message != null && CONNECTION_ISSUE_PATTERN.matcher(message.toLowerCase()).find())
                return true;
        }
        return false;
    }
}
