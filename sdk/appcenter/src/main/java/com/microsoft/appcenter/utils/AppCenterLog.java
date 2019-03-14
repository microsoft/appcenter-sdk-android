/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.IntRange;
import android.util.Log;

import static android.util.Log.VERBOSE;

/**
 * <h3>Description</h3>
 * Wrapper class for logging in the SDK as well as
 * setting the desired log level for end users.
 * Log levels correspond to those of android.util.Log.
 *
 * @see Log
 */
@SuppressWarnings("SameParameterValue")
public class AppCenterLog {

    /**
     * Log tag prefix that the SDK uses for all logs.
     */
    public static final String LOG_TAG = "AppCenter";

    /**
     * Log level to disable all logs, even assert logs.
     */
    public static final int NONE = 8;

    /**
     * Current log level.
     */
    private static int sLogLevel = Log.ASSERT;

    /**
     * Get the log level used to filter logs from the SDK. The Default will be
     * LOG_LEVEL.ASSERT so nothing shows up in LogCat.
     *
     * @return the log level
     */
    @IntRange(from = VERBOSE, to = NONE)
    public static int getLogLevel() {
        return sLogLevel;
    }

    /**
     * Set the log level to filter logs from the SDK.
     *
     * @param logLevel The log level for SDK logging.
     */
    public static void setLogLevel(@IntRange(from = VERBOSE, to = NONE) int logLevel) {
        sLogLevel = logLevel;
    }

    /**
     * Log a message with level VERBOSE
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void verbose(String tag, String message) {
        if (sLogLevel <= Log.VERBOSE) {
            Log.v(tag, message);
        }
    }

    /**
     * Log a message with level VERBOSE
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings("SameParameterValue")
    public static void verbose(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.VERBOSE) {
            Log.v(tag, message, throwable);
        }
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void debug(String tag, String message) {
        if (sLogLevel <= Log.DEBUG) {
            Log.d(tag, message);
        }
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void debug(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.DEBUG) {
            Log.d(tag, message, throwable);
        }
    }

    /**
     * Log a message with level INFO
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void info(String tag, String message) {
        if (sLogLevel <= Log.INFO) {
            Log.i(tag, message);
        }
    }

    /**
     * Log a message with level INFO
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings("SameParameterValue")
    public static void info(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.INFO) {
            Log.i(tag, message, throwable);
        }
    }

    /**
     * Log a message with level WARN
     *
     * @param tag     the TAG
     * @param message the log message
     */
    public static void warn(String tag, String message) {
        if (sLogLevel <= Log.WARN) {
            Log.w(tag, message);
        }
    }

    /**
     * Log a message with level WARN
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void warn(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.WARN) {
            Log.w(tag, message, throwable);
        }
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void error(String tag, String message) {
        if (sLogLevel <= Log.ERROR) {
            Log.e(tag, message);
        }
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void error(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.ERROR) {
            Log.e(tag, message, throwable);
        }
    }

    /**
     * Log a message with level ASSERT
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    @SuppressWarnings("WeakerAccess")
    public static void logAssert(String tag, String message) {
        if (sLogLevel <= Log.ASSERT) {
            Log.println(Log.ASSERT, tag, message);
        }
    }

    /**
     * Log a message with level ASSERT
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void logAssert(String tag, String message, Throwable throwable) {
        if (sLogLevel <= Log.ASSERT) {
            Log.println(Log.ASSERT, tag, message + "\n" + Log.getStackTraceString(throwable));
        }
    }
}
