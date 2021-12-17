/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import static android.util.Log.VERBOSE;

import com.microsoft.appcenter.BuildConfig;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

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
     * Custom logger.
     */
    private static Logger mCustomLogger;

    /**
     * Set custom logger.
     *
     * @param logger custom logger.
     */
    public static void setLogger(Logger logger) {
        mCustomLogger = logger;
    }

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
    public static void verbose(@NonNull String tag, @NonNull String message) {
        if (sLogLevel <= Log.VERBOSE) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.ALL, getMessageWithTag(tag, message));
            } else {
                Log.v(tag, message);
            }
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
    public static void verbose(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.VERBOSE) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.ALL, getMessageWithTag(tag, message), throwable);
            } else {
                Log.v(tag, message, throwable);
            }
        }
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void debug(@NonNull String tag, @NonNull String message) {
        if (sLogLevel <= Log.DEBUG) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.FINE, getMessageWithTag(tag, message));
            } else {
                Log.d(tag, message);
            }
        }
    }

    /**
     * Log a message with level DEBUG
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void debug(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.DEBUG) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.FINE, getMessageWithTag(tag, message), throwable);
            } else {
                Log.d(tag, message, throwable);
            }
        }
    }

    /**
     * Log a message with level INFO
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void info(@NonNull String tag, @NonNull String message) {
        if (sLogLevel <= Log.INFO) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.INFO, getMessageWithTag(tag, message));
            } else {
                Log.i(tag, message);
            }
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
    public static void info(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.INFO) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.INFO, getMessageWithTag(tag, message), throwable);
            } else {
                Log.i(tag, message, throwable);
            }
        }
    }

    /**
     * Log a message with level WARN
     *
     * @param tag     the TAG
     * @param message the log message
     */
    public static void warn(@NonNull String tag, @NonNull String message) {
        if (sLogLevel <= Log.WARN) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.WARNING, getMessageWithTag(tag, message));
            } else {
                Log.w(tag, message);
            }
        }
    }

    /**
     * Log a message with level WARN
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void warn(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.WARN) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.WARNING, getMessageWithTag(tag, message), throwable);
            } else {
                Log.w(tag, message, throwable);
            }
        }
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    public static void error(@NonNull String tag, @NonNull String message) {
        if (sLogLevel <= Log.ERROR) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.SEVERE, getMessageWithTag(tag, message));
            } else {
                Log.e(tag, message);
            }
        }
    }

    /**
     * Log a message with level ERROR
     *
     * @param tag       the log tag for your message
     * @param message   the log message
     * @param throwable the throwable you want to log
     */
    public static void error(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.ERROR) {
            if (mCustomLogger != null) {
                mCustomLogger.log(Level.SEVERE, getMessageWithTag(tag, message), throwable);
            } else {
                Log.e(tag, message, throwable);
            }
        }
    }

    /**
     * Log a message with level ASSERT
     *
     * @param tag     the log tag for your message
     * @param message the log message
     */
    @SuppressWarnings("WeakerAccess")
    public static void logAssert(@NonNull String tag, @NonNull String message) {
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
    public static void logAssert(@NonNull String tag, @NonNull String message, Throwable throwable) {
        if (sLogLevel <= Log.ASSERT) {
            Log.println(Log.ASSERT, tag, message + "\n" + Log.getStackTraceString(throwable));
        }
    }

    private static String getMessageWithTag(String tag, String message) {
        return String.format("%s: %s", tag, message);
    }
}
