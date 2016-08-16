package avalanche.errors;

import android.content.Context;
import android.os.SystemClock;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.model.ErrorReport;
import avalanche.errors.model.TestCrashException;
import avalanche.errors.utils.ErrorLogHelper;


public class ErrorReporting extends AbstractAvalancheFeature {

    /**
     * Constant marking event of the error reporting group.
     */
    public static final String ERROR_GROUP = "group_error";

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
    private static final String PREF_KEY_ALWAYS_SEND = "avalanche.errors.crash.always.send";

    /**
     * Default error reporting listener.
     */
    private static final ErrorReportingListener DEFAULT_ERROR_REPORTING_LISTENER = new DefaultErrorReportingListener();

    private static ErrorReporting sInstance = null;

    private final Map<String, LogFactory> mFactories;

    private LogSerializer mLogSerializer;

    private Context mContext;

    private long mInitializeTimestamp;

    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * Custom error reporting listener.
     */
    private ErrorReportingListener mErrorReportingListener;

    /**
     * Error log and error report mapping.
     */
    private Map<UUID, ErrorLogReportPair> mErrorReportMap;

    private ErrorReporting() {
        mFactories = new HashMap<>();
        mFactories.put(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
        mErrorReportingListener = DEFAULT_ERROR_REPORTING_LISTENER;
        mErrorReportMap = new LinkedHashMap<>();
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
        throw new TestCrashException();
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

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        initialize();
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull AvalancheChannel channel) {
        super.onChannelReady(context, channel);
        mContext = context;
        initialize();
        if (isInstanceEnabled()) {
            queuePendingCrashes();
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
    protected AvalancheChannel.GroupListener getChannelListener() {
        return new AvalancheChannel.GroupListener() {
            private static final int BEFORE_SENDING = 0;
            private static final int SENDING_SUCCEEDED = 1;
            private static final int SENDING_FAILED = 2;

            private void callback(int type, Log log, Exception e) {
                if (log instanceof JavaErrorLog) {
                    JavaErrorLog errorLog = (JavaErrorLog) log;
                    ErrorReport report = buildErrorReport(errorLog);
                    UUID id = errorLog.getId();
                    if (report != null) {

                        /* Clean up before calling callbacks. */
                        if (type != BEFORE_SENDING) {
                            ErrorLogHelper.removeStoredThrowableFile(id);
                            mErrorReportMap.remove(id);
                        }

                        switch (type) {
                            case BEFORE_SENDING:
                                mErrorReportingListener.onBeforeSending(report);
                                break;
                            case SENDING_SUCCEEDED:
                                mErrorReportingListener.onSendingSucceeded(report);
                                break;
                            case SENDING_FAILED:
                                mErrorReportingListener.onSendingFailed(report, e);
                                break;
                        }
                    } else
                        AvalancheLog.warn("Cannot find error report for the error log: " + id);
                } else {
                    AvalancheLog.warn("A different type of log comes to error reporting: " + log.getClass().getName());
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
        }
    }

    private ErrorReport buildErrorReport(JavaErrorLog log) {
        UUID id = log.getId();
        if (mErrorReportMap.containsKey(id)) {
            return mErrorReportMap.get(id).report;
        } else {
            File file = ErrorLogHelper.getStoredThrowableFile(id);
            if (file != null) {
                try {
                    Throwable throwable = StorageHelper.InternalStorage.readObject(file);
                    ErrorReport report = ErrorLogHelper.getErrorReportFromErrorLog(log, throwable);
                    mErrorReportMap.put(id, new ErrorLogReportPair(log, report));
                    return report;
                } catch (ClassNotFoundException ignored) {
                    AvalancheLog.error("Cannot read throwable file " + file.getName(), ignored);
                } catch (IOException ignored) {
                    AvalancheLog.error("Cannot access serialized throwable file " + file.getName(), ignored);
                }
            }
        }
        return null;
    }

    private void queuePendingCrashes() {
        for (File logFile : ErrorLogHelper.getStoredErrorLogFiles()) {
            String logfileContents = StorageHelper.InternalStorage.read(logFile);
            try {
                JavaErrorLog log = (JavaErrorLog) mLogSerializer.deserializeLog(logfileContents);
                if (log != null) {
                    if (!mErrorReportingListener.shouldProcess(buildErrorReport(log))) {
                        UUID id = log.getId();
                        mErrorReportMap.remove(id);
                        ErrorLogHelper.removeStoredErrorLogFile(id);
                        ErrorLogHelper.removeStoredThrowableFile(id);
                    }
                }
            } catch (JSONException e) {
                AvalancheLog.error("Error parsing error log", e);
            }
        }

        if (mErrorReportMap.size() > 0) {
            if (StorageHelper.PreferencesStorage.getBoolean(PREF_KEY_ALWAYS_SEND, false)) {
                AvalancheLog.info("ALWAYS_SEND flag is true. Bypass getting a user confirmation.");
                handleUserConfirmation(ALWAYS_SEND);
            } else if (!mErrorReportingListener.shouldAwaitUserConfirmation()) {
                handleUserConfirmation(SEND);
            }
        }
    }

    @VisibleForTesting
    synchronized void setInstanceListener(ErrorReportingListener listener) {
        if (listener == null) {
            listener = DEFAULT_ERROR_REPORTING_LISTENER;
        }
        mErrorReportingListener = listener;
    }

    @VisibleForTesting
    synchronized void handleUserConfirmation(@ConfirmationDef int userConfirmation) {
        if (userConfirmation == DONT_SEND) {
            /* Clean up all pending error log and throwable files. */
            for (UUID id : getInstance().mErrorReportMap.keySet()) {
                ErrorLogHelper.removeStoredErrorLogFile(id);
                ErrorLogHelper.removeStoredThrowableFile(id);
            }
            return;
        }

        if (userConfirmation == ALWAYS_SEND) {
            StorageHelper.PreferencesStorage.putBoolean(PREF_KEY_ALWAYS_SEND, true);
        }

        for (UUID id : getInstance().mErrorReportMap.keySet()) {
            ErrorLogReportPair pair = getInstance().mErrorReportMap.get(id);

            /* TODO (jaelim): Attach the return value to the log. */
            getInstance().mErrorReportingListener.getErrorAttachment(pair.report);
            getInstance().mChannel.enqueue(pair.log, ERROR_GROUP);

            /* Clean up an error log file. */
            ErrorLogHelper.removeStoredErrorLogFile(id);
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
     * ErrorLogReportPair class for error log and error report.
     */
    private static class ErrorLogReportPair {
        private final JavaErrorLog log;
        private final ErrorReport report;

        private ErrorLogReportPair(JavaErrorLog log, ErrorReport report) {
            this.log = log;
            this.report = report;
        }
    }
}
