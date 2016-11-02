package com.microsoft.sonoma.crashes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.SystemClock;
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
import com.microsoft.sonoma.core.utils.storage.StorageHelper;
import com.microsoft.sonoma.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.sonoma.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.sonoma.crashes.model.ErrorAttachment;
import com.microsoft.sonoma.crashes.model.ErrorReport;
import com.microsoft.sonoma.crashes.model.TestCrashException;
import com.microsoft.sonoma.crashes.utils.ErrorLogHelper;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Crashes feature.
 */
public class Crashes extends AbstractSonomaFeature {

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
     * Name of the feature.
     */
    private static final String FEATURE_NAME = "Crashes";

    /**
     * TAG used in logging for Crashes.
     */
    public static final String LOG_TAG = SonomaLog.LOG_TAG + FEATURE_NAME;

    /**
     * Default crashes listener.
     */
    private static final CrashesListener DEFAULT_ERROR_REPORTING_LISTENER = new DefaultCrashesListener();

    /**
     * Singleton.
     */
    @SuppressLint("StaticFieldLeak")
    private static Crashes sInstance = null;

    /**
     * Log factories managed by this feature.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Crash reports not processed yet.
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
     * Custom crashes listener.
     */
    private CrashesListener mCrashesListener;

    /**
     * Wrapper SDK listener.
     */
    private WrapperSdkListener mWrapperSdkListener;

    private ErrorReport mLastSessionErrorReport;

    private Crashes() {
        mFactories = new HashMap<>();
        mFactories.put(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());
        mCrashesListener = DEFAULT_ERROR_REPORTING_LISTENER;
        mUnprocessedErrorReports = new LinkedHashMap<>();
        mErrorReportCache = new LinkedHashMap<>();
    }

    @NonNull
    public static synchronized Crashes getInstance() {
        if (sInstance == null) {
            sInstance = new Crashes();
        }
        return sInstance;
    }


    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Crashes module is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Crashes module.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Track an exception.
     *
     * TODO the backend does not support that feature yet, will be public method later.
     *
     * @param throwable An exception.
     */
    static void trackException(@NonNull Throwable throwable) {
        getInstance().queueException(throwable);
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
     * Sets a crashes listener.
     *
     * @param listener The custom crashes listener.
     */
    public static void setListener(CrashesListener listener) {
        getInstance().setInstanceListener(listener);
    }

    /**
     * Notifies SDK with a confirmation to handle the crash report.
     *
     * @param userConfirmation A user confirmation. Should be one of {@link #SEND}, {@link #DONT_SEND} or {@link #ALWAYS_SEND}
     * @see #SEND
     * @see #DONT_SEND
     * @see #ALWAYS_SEND
     */
    public static void notifyUserConfirmation(@UserConfirmationDef int userConfirmation) {
        getInstance().handleUserConfirmation(userConfirmation);
    }

    /**
     * Provides information whether the app crashed in its last session.
     *
     * @return {@code true} if a crash was recorded in the last session, otherwise {@code false}.
     */
    public static boolean hasCrashedInLastSession() {
        return getLastSessionCrashReport() != null;
    }

    /**
     * Provides information about any available crash report from the last session, if it crashed.
     *
     * @return The crash report from the last session if one was set.
     */
    @Nullable
    public static ErrorReport getLastSessionCrashReport() {
        return getInstance().getInstanceLastSessionCrashReport();
    }

    /**
     * Implements {@link #getLastSessionCrashReport()} at instance level.
     */
    private synchronized ErrorReport getInstanceLastSessionCrashReport() {
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
            SonomaLog.info(LOG_TAG, "Deleted crashes local files");
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

    /**
     * Track an exception.
     *
     * TODO the backend does not support that feature yet, will be public method later.
     *
     * @param exception An exception.
     */
    synchronized void trackException(@NonNull com.microsoft.sonoma.crashes.ingestion.models.Exception exception) {
        if (isInactive())
            return;

        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(
                mContext,
                Thread.currentThread(),
                exception,
                Thread.getAllStackTraces(),
                getInitializeTimestamp(),
                false);
        mChannel.enqueue(errorLog, ERROR_GROUP);
    }

    @Override
    protected String getGroupName() {
        return ERROR_GROUP;
    }

    @Override
    protected String getFeatureName() {
        return FEATURE_NAME;
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
                if (log instanceof ManagedErrorLog) {
                    ManagedErrorLog errorLog = (ManagedErrorLog) log;
                    ErrorReport report = buildErrorReport(errorLog);
                    UUID id = errorLog.getId();
                    if (report != null) {

                        if (type == BEFORE_SENDING) {
                            mCrashesListener.onBeforeSending(report);
                        } else {

                            /* Clean up before calling callbacks. */
                            removeStoredThrowable(id);

                            if (type == SENDING_SUCCEEDED) {
                                mCrashesListener.onSendingSucceeded(report);
                            } else {
                                mCrashesListener.onSendingFailed(report, e);
                            }
                        }
                    } else
                        SonomaLog.warn(LOG_TAG, "Cannot find crash report for the error log: " + id);
                } else {
                    SonomaLog.warn(LOG_TAG, "A different type of log comes to crashes: " + log.getClass().getName());
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
    @VisibleForTesting
    synchronized long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    /**
     * Send an exception.
     *
     * @param throwable An exception.
     */
    private synchronized void queueException(@NonNull final Throwable throwable) {
        if (isInactive())
            return;
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(
                mContext,
                Thread.currentThread(),
                throwable,
                Thread.getAllStackTraces(),
                getInitializeTimestamp(),
                false);
        mChannel.enqueue(errorLog, ERROR_GROUP);
    }

    private void initialize() {
        boolean enabled = isInstanceEnabled();
        mInitializeTimestamp = enabled ? SystemClock.elapsedRealtime() : -1;

        if (!enabled) {
            if (mUncaughtExceptionHandler != null) {
                mUncaughtExceptionHandler.unregister();
                mUncaughtExceptionHandler = null;
            }
        } else if (mContext != null && mUncaughtExceptionHandler == null) {
            mUncaughtExceptionHandler = new UncaughtExceptionHandler();
            mUncaughtExceptionHandler.register();
            File logFile = ErrorLogHelper.getLastErrorLogFile();
            if (logFile != null) {
                String logFileContents = StorageHelper.InternalStorage.read(logFile);
                if (logFileContents != null)
                    try {
                        ManagedErrorLog log = (ManagedErrorLog) mLogSerializer.deserializeLog(logFileContents);
                        mLastSessionErrorReport = buildErrorReport(log);
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
            if (logfileContents != null)
                try {
                    ManagedErrorLog log = (ManagedErrorLog) mLogSerializer.deserializeLog(logfileContents);
                    UUID id = log.getId();
                    ErrorReport report = buildErrorReport(log);
                    if (report == null) {
                        removeAllStoredErrorLogFiles(id);
                    } else if (mCrashesListener.shouldProcess(report)) {
                        SonomaLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned true, continue processing log: " + id.toString());
                        mUnprocessedErrorReports.put(id, mErrorReportCache.get(id));
                    } else {
                        SonomaLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned false, clean up and ignore log: " + id.toString());
                        removeAllStoredErrorLogFiles(id);
                    }
                } catch (JSONException e) {
                    SonomaLog.error(LOG_TAG, "Error parsing error log", e);
                }
        }

        boolean shouldAwaitUserConfirmation = true;
        if (mUnprocessedErrorReports.size() > 0 &&
                (StorageHelper.PreferencesStorage.getBoolean(PREF_KEY_ALWAYS_SEND, false)
                        || !(shouldAwaitUserConfirmation = mCrashesListener.shouldAwaitUserConfirmation()))) {
            if (shouldAwaitUserConfirmation)
                SonomaLog.debug(LOG_TAG, "CrashesListener.shouldAwaitUserConfirmation returned false, continue sending logs");
            else
                SonomaLog.debug(LOG_TAG, "The flag for user confirmation is set to ALWAYS_SEND, continue sending logs");
            handleUserConfirmation(SEND);
        }
    }

    private void removeAllStoredErrorLogFiles(UUID id) {
        ErrorLogHelper.removeStoredErrorLogFile(id);
        removeStoredThrowable(id);
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
    ErrorReport buildErrorReport(ManagedErrorLog log) {
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
    CrashesListener getInstanceListener() {
        return mCrashesListener;
    }

    @VisibleForTesting
    synchronized void setInstanceListener(CrashesListener listener) {
        if (listener == null) {
            listener = DEFAULT_ERROR_REPORTING_LISTENER;
        }
        mCrashesListener = listener;
    }

    /**
     * Set wrapper SDK listener.
     *
     * @param wrapperSdkListener listener.
     */
    @SuppressWarnings("WeakerAccess")
    public void setWrapperSdkListener(WrapperSdkListener wrapperSdkListener) {
        mWrapperSdkListener = wrapperSdkListener;
    }

    @VisibleForTesting
    private synchronized void handleUserConfirmation(@UserConfirmationDef int userConfirmation) {
        if (mChannel == null) {
            SonomaLog.error(LOG_TAG, "Crashes feature not initialized, discarding calls.");
            return;
        }

        if (userConfirmation == DONT_SEND) {

            /* Clean up all pending error log and throwable files. */
            for (Iterator<UUID> iterator = mUnprocessedErrorReports.keySet().iterator(); iterator.hasNext(); ) {
                UUID id = iterator.next();
                iterator.remove();
                removeAllStoredErrorLogFiles(id);
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
            ErrorAttachment attachment = mCrashesListener.getErrorAttachment(errorLogReport.report);
            if (attachment == null)
                SonomaLog.debug(LOG_TAG, "CrashesListener.getErrorAttachment returned null, no additional information will be attached to log: " + errorLogReport.log.getId().toString());
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

    /**
     * Save a crash.
     *
     * @param thread    origin thread.
     * @param exception exception.
     */
    void saveUncaughtException(Thread thread, Throwable exception) {
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mContext, thread, exception, Thread.getAllStackTraces(), mInitializeTimestamp, true);
        try {
            File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
            String filename = errorLog.getId().toString();
            SonomaLog.debug(Crashes.LOG_TAG, "Saving uncaught exception:", exception);
            saveErrorLog(errorLog, errorStorageDirectory, filename);
            File throwableFile = new File(errorStorageDirectory, filename + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
            StorageHelper.InternalStorage.writeObject(throwableFile, exception);
            SonomaLog.debug(Crashes.LOG_TAG, "Saved Throwable as is for client side inspection in " + throwableFile);
            if (mWrapperSdkListener != null) {
                mWrapperSdkListener.onCrashCaptured(errorLog);
            }
        } catch (JSONException e) {
            SonomaLog.error(Crashes.LOG_TAG, "Error serializing error log to JSON", e);
        } catch (IOException e) {
            SonomaLog.error(Crashes.LOG_TAG, "Error writing error log to file", e);
        }
    }

    /**
     * Serialize error log to a file.
     */
    private void saveErrorLog(ManagedErrorLog errorLog, File errorStorageDirectory, String filename) throws JSONException, IOException {
        File errorLogFile = new File(errorStorageDirectory, filename + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);
        String errorLogString = mLogSerializer.serializeLog(errorLog);
        StorageHelper.InternalStorage.write(errorLogFile, errorLogString);
        SonomaLog.debug(Crashes.LOG_TAG, "Saved JSON content for ingestion into " + errorLogFile);
    }

    /**
     * Save error log modified by a wrapper SDK.
     *
     * @param errorLog error log to  save or overwrite.
     */
    @SuppressWarnings("WeakerAccess")
    public void saveWrapperSdkErrorLog(ManagedErrorLog errorLog) {
        try {
            saveErrorLog(errorLog, ErrorLogHelper.getErrorStorageDirectory(), errorLog.getId().toString());
        } catch (JSONException e) {
            SonomaLog.error(Crashes.LOG_TAG, "Error serializing error log to JSON", e);
        } catch (IOException e) {
            SonomaLog.error(Crashes.LOG_TAG, "Error writing error log to file", e);
        }
    }

    /**
     * Listener for Wrapper SDK. Meant only for internal use by wrapper SDK developers.
     */
    @SuppressWarnings("WeakerAccess")
    public interface WrapperSdkListener {

        /**
         * Called when crash has been caught and saved.
         *
         * @param errorLog generated error log for the crash.
         */
        void onCrashCaptured(ManagedErrorLog errorLog);
    }

    /**
     * Default crashes listener class.
     */
    private static class DefaultCrashesListener extends AbstractCrashesListener {

    }

    /**
     * Class holding an error log and its corresponding error report.
     */
    private static class ErrorLogReport {

        private final ManagedErrorLog log;

        private final ErrorReport report;

        private ErrorLogReport(ManagedErrorLog log, ErrorReport report) {
            this.log = log;
            this.report = report;
        }
    }
}
