package com.microsoft.appcenter.http;

import android.support.annotation.VisibleForTesting;

import java.io.EOFException;
import java.io.InterruptedIOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

/**
 * HTTP utilities.
 */
public class HttpUtils {

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
    private static final Pattern CONNECTION_ISSUE_PATTERN = Pattern.compile("connection (time|reset)|failure in ssl library, usually a protocol error|anchor for certification path not found");

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

    public static String hideSecret(String secret) {

        /* Cannot hide null or empty string. */
        if (secret == null || secret.isEmpty()) {
            return secret;
        }

        /* Hide secret if string is neither null nor empty string. */
        int hidingEndIndex = secret.length() - (secret.length() >= MAX_CHARACTERS_DISPLAYED_FOR_SECRET ? MAX_CHARACTERS_DISPLAYED_FOR_SECRET : 0);
        char[] fill = new char[hidingEndIndex];
        Arrays.fill(fill, '*');
        return new String(fill) + secret.substring(hidingEndIndex);
    }
}
