package com.microsoft.sonoma.errors;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.sonoma.core.AbstractSonomaFeature;
import com.microsoft.sonoma.core.Constants;
import com.microsoft.sonoma.core.channel.Channel;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.core.ingestion.models.json.LogSerializer;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.errors.ingestion.models.JavaErrorLog;
import com.microsoft.sonoma.errors.ingestion.models.json.JavaErrorLogFactory;
import com.microsoft.sonoma.errors.model.ErrorAttachment;
import com.microsoft.sonoma.errors.model.ErrorReport;
import com.microsoft.sonoma.errors.model.TestCrashException;
import com.microsoft.sonoma.errors.utils.ErrorLogHelper;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Error reporting feature set.
 */
public class ErrorReporting extends AbstractSonomaFeature {

    /**
     * TAG used in logging for ErrorReporting
     */
    public static final String LOG_TAG = SonomaLog.LOG_TAG + "ErrorReporting";

    /**
     * Constant for SEND crash report.
     */
    public static final int SEND = 0;

    /**
     * Constant for DO NOT SEND crash report.
     */
    public static final int DONT_SEND = 1;

    /**
     * Constant for ALWAYS SEND crash reports.
     */
    public static final int ALWAYS_SEND = 2;

    /**
     * Preference storage key for ALWAYS SEND.
     */
    /* TODO maybe add an API to reset and make that private. */
    @VisibleForTesting
    public static final String PREF_KEY_ALWAYS_SEND = "com.microsoft.sonoma.errors.crash.always.send";

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String ERROR_GROUP = "group_errors";

    /**
     * Default error reporting listener.
     */
    private static final ErrorReportingListener DEFAULT_ERROR_REPORTING_LISTENER = new DefaultErrorReportingListener();

    /**
     * Singleton.
     */
    private static ErrorReporting sInstance = null;

    /**
     * Log factories managed by this feature.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Error reports not processed yet.
     */
    private final Map<UUID, ErrorLogReport> mUnprocessedErrorReports;

    /**
     * Cache for reports that are queued to channel but not yet sent.
     */
    private final Map<UUID, ErrorLogReport> mErrorReportCache;

    /**
     * Log serializer.
     */
    private LogSerializer mLogSerializer;

    /**
     * Application context.
     */
    private Context mContext;

    /**
     * Timestamp of initialization.
     */
    private long mInitializeTimestamp;

    /**
     * Crash handler.
     */
    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * Custom error reporting listener.
     */
    private ErrorReportingListener mErrorReportingListener;

    private ErrorReport mLastSessionErrorReport;

    private ErrorReporting() {
        mFactories = new HashMap<>();
        mFactories.put(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
        mErrorReportingListener = DEFAULT_ERROR_REPORTING_LISTENER;
        mUnprocessedErrorReports = new LinkedHashMap<>();
        mErrorReportCache = new LinkedHashMap<>();
    }

    @NonNull
    public static synchronized ErrorReporting getInstance() {
        if (sInstance == null) {
            sInstance = new ErrorReporting();
        }
        return sInstance;
    }


    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Generates crash for test purpose.
     */
    public static void generateTestCrash() {
        if (Constants.APPLICATION_DEBUGGABLE)
            throw new TestCrashException();
        else
            SonomaLog.warn(LOG_TAG, "The application is not debuggable so SDK won't generate test crash");
    }

    /**
     * Sets an error reporting listener.
     *
     * @param listener The custom error reporting listener.
     */
    public static void setListener(ErrorReportingListener listener) {
        getInstance().setInstanceListener(listener);
    }

    /**
     * Notifies a user confirmation for crashes.
     *
     * @param userConfirmation A response of the confirmation. Should be one of {@link #SEND}, {@link #DONT_SEND} or {@link #ALWAYS_SEND}
     * @see #SEND
     * @see #DONT_SEND
     * @see #ALWAYS_SEND
     */
    public static void notifyUserConfirmation(@ConfirmationDef int userConfirmation) {
        getInstance().handleUserConfirmation(userConfirmation);
    }

    /**
     * Provides information whether the app crashed in its last session.
     *
     * @return {@code true} if a crash was recorded in the last session, otherwise {@code false}.
     */
    public static boolean hasCrashedInLastSession() {
        return getLastSessionErrorReport() != null;
    }

    /**
     * Provides information about any available error report from the last session, if it crashed.
     *
     * @return The error report from the last session if one was set.
     */
    @Nullable
    public static ErrorReport getLastSessionErrorReport() {
        return getInstance().getInstanceLastSessionErrorReport();
    }

    /**
     * Implements {@link #getLastSessionErrorReport()} at instance level.
     */
    private synchronized ErrorReport getInstanceLastSessionErrorReport() {
        return mLastSessionErrorReport;
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        initialize();
        if (!enabled) {
            for (File file : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
                SonomaLog.debug(LOG_TAG, "Deleting file " + file);
                if (!file.delete()) {
                    SonomaLog.warn(LOG_TAG, "Failed to delete file " + file);
                }
            }
            SonomaLog.info(LOG_TAG, "Deleted error reporting local files");
        }
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull Channel channel) {
        super.onChannelReady(context, channel);
        mContext = context;
        initialize();
        if (isInstanceEnabled()) {
            processPendingErrors();
        }
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    @Override
    protected int getTriggerCount() {
        return 1;
    }

    @Override
    protected Channel.GroupListener getChannelListener() {
        return new Channel.GroupListener() {
            private static final int BEFORE_SENDING = 0;
            private static final int SENDING_SUCCEEDED = 1;
            private static final int SENDING_FAILED = 2;

            private void callback(int type, Log log, Exception e) {
                if (log instanceof JavaErrorLog) {
                    JavaErrorLog errorLog = (JavaErrorLog) log;
                    ErrorReport report = buildErrorReport(errorLog);
                    UUID id = errorLog.getId();
                    if (report != null) {

                        if (type == BEFORE_SENDING) {
                            mErrorReportingListener.onBeforeSending(report);
                        } else {

                            /* Clean up before calling callbacks. */
                            removeStoredThrowable(id);

                            if (type == SENDING_SUCCEEDED) {
                                mErrorReportingListener.onSendingSucceeded(report);
                            } else {
                                mErrorReportingListener.onSendingFailed(report, e);
                            }
                        }
                    } else
                        SonomaLog.warn(LOG_TAG, "Cannot find error report for the error log: " + id);
                } else {
                    SonomaLog.warn(LOG_TAG, "A different type of log comes to error reporting: " + log.getClass().getName());
                }
            }

            @Override
            public void onBeforeSending(Log log) {
                callback(BEFORE_SENDING, log, null);
            }

            @Override
            public void onSuccess(Log log) {
                callback(SENDING_SUCCEEDED, log, null);
            }

            @Override
            public void onFailure(Log log, Exception e) {
                callback(SENDING_FAILED, log, e);
            }
        };
    }

    /**
     * Get initialization timestamp.
     *
     * @return initialization timestamp expressed using {@link SystemClock#elapsedRealtime()}.
     */
    synchronized long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    private void initialize() {
        boolean enabled = isInstanceEnabled();
        mInitializeTimestamp = enabled ? SystemClock.elapsedRealtime() : -1;

        if (!enabled) {
            if (mUncaughtExceptionHandler != null) {
                mUncaughtExceptionHandler.unregister();
                mUncaughtExceptionHandler = null;
            }
        } else if (mContext != null) {
            mUncaughtExceptionHandler = new UncaughtExceptionHandler(mContext);
            mUncaughtExceptionHandler.register();
        }

        if (enabled) {
            File logFile = ErrorLogHelper.getLastErrorLogFile();
            if (logFile != null) {
                String logFileContents = StorageHelper.InternalStorage.read(logFile);
                try {
                    JavaErrorLog log = (JavaErrorLog) mLogSerializer.deserializeLog(logFileContents);
                    if (log != null) {
                        mLastSessionErrorReport = buildErrorReport(log);
                    }
                } catch (JSONException e) {
                    SonomaLog.error(LOG_TAG, "Error parsing last session error log", e);
                }
            }
        }
    }

    private void processPendingErrors() {
        for (File logFile : ErrorLogHelper.getStoredErrorLogFiles()) {
            SonomaLog.debug(LOG_TAG, "Process pending error file: " + logFile);
            String logfileContents = StorageHelper.InternalStorage.read(logFile);
            try {
                JavaErrorLog log = (JavaErrorLog) mLogSerializer.deserializeLog(logfileContents);
                if (log != null) {
                    UUID id = log.getId();
                    ErrorReport errorReport = buildErrorReport(log);
                    if (errorReport != null && mErrorReportingListener.shouldProcess(errorReport)) {
                        SonomaLog.debug(LOG_TAG, "ErrorReportingListener.shouldProcess returned true, continue processing log: " + id.toString());
                        mUnprocessedErrorReports.put(id, mErrorReportCache.get(id));
                    } else {
                        if (errorReport != null)
                            SonomaLog.debug(LOG_TAG, "ErrorReportingListener.shouldProcess returned false, clean up and ignore log: " + id.toString());

                        ErrorLogHelper.removeStoredErrorLogFile(id);
                        removeStoredThrowable(id);
                    }
                }
            } catch (JSONException e) {
                SonomaLog.error(LOG_TAG, "Error parsing error log", e);
            }
        }

        boolean shouldAwaitUserConfirmation = true;
        if (mUnprocessedErrorReports.size() > 0 &&
                (StorageHelper.PreferencesStorage.getBoolean(PREF_KEY_ALWAYS_SEND, false)
                        || !(shouldAwaitUserConfirmation = mErrorReportingListener.shouldAwaitUserConfirmation()))) {
            if (shouldAwaitUserConfirmation)
                SonomaLog.debug(LOG_TAG, "ErrorReportingListener.shouldAwaitUserConfirmation returned false, continue sending logs");
            else
                SonomaLog.debug(LOG_TAG, "The flag for user confirmation is set to ALWAYS_SEND, continue sending logs");
            handleUserConfirmation(SEND);
        }
    }

    private void removeStoredThrowable(UUID id) {
        mErrorReportCache.remove(id);
        ErrorLogHelper.removeStoredThrowableFile(id);
    }

    @VisibleForTesting
    UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return mUncaughtExceptionHandler;
    }

    @VisibleForTesting
    void setUncaughtExceptionHandler(UncaughtExceptionHandler handler) {
        mUncaughtExceptionHandler = handler;
    }

    @VisibleForTesting
    @Nullable
    ErrorReport buildErrorReport(JavaErrorLog log) {
        UUID id = log.getId();
        if (mErrorReportCache.containsKey(id)) {
            return mErrorReportCache.get(id).report;
        } else {
            File file = ErrorLogHelper.getStoredThrowableFile(id);
            if (file != null) {
                try {
                    Throwable throwable = StorageHelper.InternalStorage.readObject(file);
                    ErrorReport report = ErrorLogHelper.getErrorReportFromErrorLog(log, throwable);
                    mErrorReportCache.put(id, new ErrorLogReport(log, report));
                    return report;
                } catch (ClassNotFoundException ignored) {
                    SonomaLog.error(LOG_TAG, "Cannot read throwable file " + file.getName(), ignored);
                } catch (IOException ignored) {
                    SonomaLog.error(LOG_TAG, "Cannot access serialized throwable file " + file.getName(), ignored);
                }
            }
        }
        return null;
    }

    @VisibleForTesting
    ErrorReportingListener getInstanceListener() {
        return mErrorReportingListener;
    }

    @VisibleForTesting
    synchronized void setInstanceListener(ErrorReportingListener listener) {
        if (listener == null) {
            listener = DEFAULT_ERROR_REPORTING_LISTENER;
        }
        mErrorReportingListener = listener;
    }

    @VisibleForTesting
    private synchronized void handleUserConfirmation(@ConfirmationDef int userConfirmation) {
        if (mChannel == null) {
            SonomaLog.error(LOG_TAG, "ErrorReporting feature not initialized, discarding calls.");
            return;
        }

        if (userConfirmation == DONT_SEND) {

            /* Clean up all pending error log and throwable files. */
            for (Iterator<UUID> iterator = mUnprocessedErrorReports.keySet().iterator(); iterator.hasNext(); ) {
                UUID id = iterator.next();
                iterator.remove();
                ErrorLogHelper.removeStoredErrorLogFile(id);
                removeStoredThrowable(id);
            }
            return;
        }

        if (userConfirmation == ALWAYS_SEND) {
            StorageHelper.PreferencesStorage.putBoolean(PREF_KEY_ALWAYS_SEND, true);
        }

        Iterator<Map.Entry<UUID, ErrorLogReport>> unprocessedIterator = mUnprocessedErrorReports.entrySet().iterator();
        while (unprocessedIterator.hasNext()) {

            Map.Entry<UUID, ErrorLogReport> unprocessedEntry = unprocessedIterator.next();
            ErrorLogReport errorLogReport = unprocessedEntry.getValue();
            ErrorAttachment attachment = mErrorReportingListener.getErrorAttachment(errorLogReport.report);
            if (attachment == null)
                SonomaLog.debug(LOG_TAG, "ErrorReportingListener.getErrorAttachment returned null, no additional information will be attached to log: " + errorLogReport.log.getId().toString());
            else
                errorLogReport.log.setErrorAttachment(attachment);
            mChannel.enqueue(errorLogReport.log, ERROR_GROUP);

            /* Clean up an error log file and map entry. */
            unprocessedIterator.remove();
            ErrorLogHelper.removeStoredErrorLogFile(unprocessedEntry.getKey());
        }
    }

    @VisibleForTesting
    void setLogSerializer(LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            SEND,
            DONT_SEND,
            ALWAYS_SEND
    })
    private @interface ConfirmationDef {
    }

    /**
     * Default error reporting listener class.
     */
    private static class DefaultErrorReportingListener extends AbstractErrorReportingListener {
    }

    /**
     * Class holding an error log and its corresponding error report.
     */
    private static class ErrorLogReport {
        private final JavaErrorLog log;

        private final ErrorReport report;

        private ErrorLogReport(JavaErrorLog log, ErrorReport report) {
            this.log = log;
            this.report = report;
        }
    }
}
