package com.microsoft.azure.mobile.crashes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.Constants;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.crashes.ingestion.models.ErrorAttachmentLog;
import com.microsoft.azure.mobile.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.crashes.ingestion.models.json.ErrorAttachmentLogFactory;
import com.microsoft.azure.mobile.crashes.ingestion.models.json.HandledErrorLogFactory;
import com.microsoft.azure.mobile.crashes.ingestion.models.json.ManagedErrorLogFactory;
import com.microsoft.azure.mobile.crashes.model.ErrorReport;
import com.microsoft.azure.mobile.crashes.model.TestCrashException;
import com.microsoft.azure.mobile.crashes.utils.ErrorLogHelper;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.DefaultMobileCenterFuture;
import com.microsoft.azure.mobile.utils.async.MobileCenterFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Crashes service.
 */
public class Crashes extends AbstractMobileCenterService {

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
    @VisibleForTesting
    public static final String PREF_KEY_ALWAYS_SEND = "com.microsoft.azure.mobile.crashes.always.send";

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String ERROR_GROUP = "group_errors";

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Crashes";

    /**
     * TAG used in logging for Crashes.
     */
    public static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Max allowed attachments per crash.
     */
    private static final int MAX_ATTACHMENT_PER_CRASH = 2;

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
     * Log factories managed by this service.
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
     * ErrorReport for the last session.
     */
    private ErrorReport mLastSessionErrorReport;

    /**
     * Flag to remember whether we already saved uncaught exception or not.
     */
    private boolean mSavedUncaughtException;

    /**
     * Automatic processing flag (automatic is the default).
     */
    private boolean mAutomaticProcessing = true;

    /**
     * Init.
     */
    private Crashes() {
        mFactories = new HashMap<>();
        mFactories.put(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());
        mFactories.put(HandledErrorLog.TYPE, HandledErrorLogFactory.getInstance());
        mFactories.put(ErrorAttachmentLog.TYPE, ErrorAttachmentLogFactory.getInstance());
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(ManagedErrorLog.TYPE, ManagedErrorLogFactory.getInstance());
        mLogSerializer.addLogFactory(ErrorAttachmentLog.TYPE, ErrorAttachmentLogFactory.getInstance());
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
     * Check whether Crashes service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see MobileCenterFuture
     */
    public static MobileCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Crashes service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static MobileCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Track an exception.
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param throwable An exception.
     */
    @SuppressWarnings("SameParameterValue")
    static void trackException(@NonNull Throwable throwable) {
        getInstance().queueException(throwable);
    }

    /**
     * Generates crash for test purpose.
     */
    public static void generateTestCrash() {
        if (Constants.APPLICATION_DEBUGGABLE) {
            throw new TestCrashException();
        } else {
            MobileCenterLog.warn(LOG_TAG, "The application is not debuggable so SDK won't generate test crash");
        }
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
     * Check whether the app crashed in its last session.
     *
     * @return future with result being <code>true</code> if there was a crash in the last session, <code>false</code> otherwise.
     * @see MobileCenterFuture
     */
    public static MobileCenterFuture<Boolean> hasCrashedInLastSession() {
        return getInstance().hasInstanceCrashedInLastSession();
    }

    /**
     * Provides information about any available crash report from the last session, if it crashed.
     *
     * @return future with result being the crash report from last session or null if there wasn't one.
     * @see MobileCenterFuture
     */
    public static MobileCenterFuture<ErrorReport> getLastSessionCrashReport() {
        return getInstance().getInstanceLastSessionCrashReport();
    }

    /**
     * Implements {@link #hasCrashedInLastSession()} at instance level.
     */
    private synchronized MobileCenterFuture<Boolean> hasInstanceCrashedInLastSession() {
        final DefaultMobileCenterFuture<Boolean> future = new DefaultMobileCenterFuture<>();
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                future.complete(mLastSessionErrorReport != null);
            }
        }, future, false);
        return future;
    }

    /**
     * Implements {@link #getLastSessionCrashReport()} at instance level.
     */
    private synchronized MobileCenterFuture<ErrorReport> getInstanceLastSessionCrashReport() {
        final DefaultMobileCenterFuture<ErrorReport> future = new DefaultMobileCenterFuture<>();
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                future.complete(mLastSessionErrorReport);
            }
        }, future, null);
        return future;
    }


    @Override
    protected synchronized void applyEnabledState(boolean enabled) {
        initialize();
        if (!enabled) {
            for (File file : ErrorLogHelper.getErrorStorageDirectory().listFiles()) {
                MobileCenterLog.debug(LOG_TAG, "Deleting file " + file);
                if (!file.delete()) {
                    MobileCenterLog.warn(LOG_TAG, "Failed to delete file " + file);
                }
            }
            MobileCenterLog.info(LOG_TAG, "Deleted crashes local files");
        }
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        mContext = context;
        if (isInstanceEnabled()) {
            processPendingErrors();
        } else {
            initialize();
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
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    protected int getTriggerCount() {
        return 1;
    }

    @Override
    protected Channel.GroupListener getChannelListener() {
        return new Channel.GroupListener() {

            /** Process callback (template method) */
            private void processCallback(final Log log, final CallbackProcessor callbackProcessor) {
                post(new Runnable() {

                    @Override
                    public void run() {
                        if (log instanceof ManagedErrorLog) {
                            ManagedErrorLog errorLog = (ManagedErrorLog) log;
                            final ErrorReport report = buildErrorReport(errorLog);
                            UUID id = errorLog.getId();
                            if (report != null) {

                                /* Clean up before calling callbacks if requested. */
                                if (callbackProcessor.shouldDeleteThrowable()) {
                                    removeStoredThrowable(id);
                                }

                                /* Call back. */
                                HandlerUtils.runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        callbackProcessor.onCallBack(report);
                                    }
                                });
                            } else {
                                MobileCenterLog.warn(LOG_TAG, "Cannot find crash report for the error log: " + id);
                            }
                        } else if (!(log instanceof ErrorAttachmentLog) && !(log instanceof HandledErrorLog)) {
                            MobileCenterLog.warn(LOG_TAG, "A different type of log comes to crashes: " + log.getClass().getName());
                        }
                    }
                });
            }

            @Override
            public void onBeforeSending(Log log) {
                processCallback(log, new CallbackProcessor() {

                    @Override
                    public boolean shouldDeleteThrowable() {
                        return false;
                    }

                    @Override
                    public void onCallBack(ErrorReport report) {
                        mCrashesListener.onBeforeSending(report);
                    }
                });
            }

            @Override
            public void onSuccess(Log log) {
                processCallback(log, new CallbackProcessor() {

                    @Override
                    public boolean shouldDeleteThrowable() {
                        return true;
                    }

                    @Override
                    public void onCallBack(ErrorReport report) {
                        mCrashesListener.onSendingSucceeded(report);
                    }
                });
            }

            @Override
            public void onFailure(Log log, final Exception e) {
                processCallback(log, new CallbackProcessor() {

                    @Override
                    public boolean shouldDeleteThrowable() {
                        return true;
                    }

                    @Override
                    public void onCallBack(ErrorReport report) {
                        mCrashesListener.onSendingFailed(report, e);
                    }
                });
            }
        };
    }

    /**
     * Get initialization timestamp.
     *
     * @return initialization timestamp expressed using {@link System#currentTimeMillis()}.
     */
    @VisibleForTesting
    synchronized long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    /**
     * Send an handled exception.
     *
     * @param throwable An handled exception.
     */
    private synchronized void queueException(@NonNull final Throwable throwable) {
        queueException(new ExceptionModelBuilder() {

            @Override
            public com.microsoft.azure.mobile.crashes.ingestion.models.Exception buildExceptionModel() {
                return ErrorLogHelper.getModelExceptionFromThrowable(throwable);
            }
        });
    }

    /**
     * Send an handled exception (used by wrapper SDKs).
     *
     * @param modelException An handled exception already in JSON model form.
     */
    synchronized void queueException(@NonNull final com.microsoft.azure.mobile.crashes.ingestion.models.Exception modelException) {
        queueException(new ExceptionModelBuilder() {

            @Override
            public com.microsoft.azure.mobile.crashes.ingestion.models.Exception buildExceptionModel() {
                return modelException;
            }
        });
    }

    private synchronized void queueException(@NonNull final ExceptionModelBuilder exceptionModelBuilder) {
        post(new Runnable() {

            @Override
            public void run() {
                HandledErrorLog errorLog = new HandledErrorLog();
                errorLog.setId(UUID.randomUUID());
                errorLog.setException(exceptionModelBuilder.buildExceptionModel());
                mChannel.enqueue(errorLog, ERROR_GROUP);
            }
        });
    }

    private void initialize() {
        boolean enabled = isInstanceEnabled();
        mInitializeTimestamp = enabled ? System.currentTimeMillis() : -1;
        if (!enabled) {
            if (mUncaughtExceptionHandler != null) {
                mUncaughtExceptionHandler.unregister();
                mUncaughtExceptionHandler = null;
            }
        } else {
            mUncaughtExceptionHandler = new UncaughtExceptionHandler();
            mUncaughtExceptionHandler.register();
            final File logFile = ErrorLogHelper.getLastErrorLogFile();
            if (logFile != null) {
                MobileCenterLog.debug(LOG_TAG, "Processing crash report for the last session.");
                String logFileContents = StorageHelper.InternalStorage.read(logFile);
                if (logFileContents == null) {
                    MobileCenterLog.error(LOG_TAG, "Error reading last session error log.");
                } else {
                    try {
                        ManagedErrorLog log = (ManagedErrorLog) mLogSerializer.deserializeLog(logFileContents);
                        mLastSessionErrorReport = buildErrorReport(log);
                        MobileCenterLog.debug(LOG_TAG, "Processed crash report for the last session.");
                    } catch (JSONException e) {
                        MobileCenterLog.error(LOG_TAG, "Error parsing last session error log.", e);
                    }
                }
            }
        }
    }

    private void processPendingErrors() {
        for (File logFile : ErrorLogHelper.getStoredErrorLogFiles()) {
            MobileCenterLog.debug(LOG_TAG, "Process pending error file: " + logFile);
            String logfileContents = StorageHelper.InternalStorage.read(logFile);
            if (logfileContents != null) {
                try {
                    ManagedErrorLog log = (ManagedErrorLog) mLogSerializer.deserializeLog(logfileContents);
                    UUID id = log.getId();
                    ErrorReport report = buildErrorReport(log);
                    if (report == null) {
                        removeAllStoredErrorLogFiles(id);
                    } else if (!mAutomaticProcessing || mCrashesListener.shouldProcess(report)) {
                        if (!mAutomaticProcessing) {
                            MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned true, continue processing log: " + id.toString());
                        }
                        mUnprocessedErrorReports.put(id, mErrorReportCache.get(id));
                    } else {
                        MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned false, clean up and ignore log: " + id.toString());
                        removeAllStoredErrorLogFiles(id);
                    }
                } catch (JSONException e) {
                    MobileCenterLog.error(LOG_TAG, "Error parsing error log", e);
                }
            }
        }

        /* If automatic processing is enabled. */
        if (mAutomaticProcessing) {

            /* Proceed to check if user confirmation is needed. */
            sendCrashReportsOrAwaitUserConfirmation();
        }

    }

    /**
     * Send crashes or wait for user confirmation (either via callback or explicit call in manual processing).
     *
     * @return true if always send was persisted, false otherwise.
     */
    private boolean sendCrashReportsOrAwaitUserConfirmation() {

        /* Handle user confirmation in UI thread. */
        final boolean alwaysSend = StorageHelper.PreferencesStorage.getBoolean(PREF_KEY_ALWAYS_SEND, false);
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {

                /* If we still have crashes to send after filtering. */
                if (mUnprocessedErrorReports.size() > 0) {

                    /* Check for always send: this bypasses user confirmation callback. */
                    if (alwaysSend) {
                        MobileCenterLog.debug(LOG_TAG, "The flag for user confirmation is set to ALWAYS_SEND, will send logs.");
                        handleUserConfirmation(SEND);
                        return;
                    }

                    /* For disabled automatic processing, we don't call listener. */
                    if (!mAutomaticProcessing) {
                        MobileCenterLog.debug(LOG_TAG, "Automatic processing disabled, will wait for explicit user confirmation.");
                        return;
                    }

                    /* Check via listener if should wait for user confirmation. */
                    if (!mCrashesListener.shouldAwaitUserConfirmation()) {
                        MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldAwaitUserConfirmation returned false, will send logs.");
                        handleUserConfirmation(SEND);
                    } else {
                        MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldAwaitUserConfirmation returned true, wait sending logs.");
                    }
                }
            }
        });
        return alwaysSend;
    }

    private void removeAllStoredErrorLogFiles(UUID id) {
        ErrorLogHelper.removeStoredErrorLogFile(id);
        removeStoredThrowable(id);
    }

    private void removeStoredThrowable(UUID id) {
        mErrorReportCache.remove(id);
        WrapperSdkExceptionManager.deleteWrapperExceptionData(id);
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
                    Throwable throwable = null;
                    if (file.length() > 0) {
                        throwable = StorageHelper.InternalStorage.readObject(file);
                    }
                    ErrorReport report = ErrorLogHelper.getErrorReportFromErrorLog(log, throwable);
                    mErrorReportCache.put(id, new ErrorLogReport(log, report));
                    return report;
                } catch (ClassNotFoundException ignored) {
                    MobileCenterLog.error(LOG_TAG, "Cannot read throwable file " + file.getName(), ignored);
                } catch (IOException ignored) {
                    MobileCenterLog.error(LOG_TAG, "Cannot access serialized throwable file " + file.getName(), ignored);
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

    @VisibleForTesting
    private synchronized void handleUserConfirmation(@UserConfirmationDef final int userConfirmation) {
        post(new Runnable() {

            @Override
            public void run() {

                /* If we don't send. */
                if (userConfirmation == DONT_SEND) {

                    /* Clean up all pending error log and throwable files. */
                    for (Iterator<UUID> iterator = mUnprocessedErrorReports.keySet().iterator(); iterator.hasNext(); ) {
                        UUID id = iterator.next();
                        iterator.remove();
                        removeAllStoredErrorLogFiles(id);
                    }
                }

                /* We send the crash. */
                else {

                    /* Always send: we remember. */
                    if (userConfirmation == ALWAYS_SEND) {
                        StorageHelper.PreferencesStorage.putBoolean(PREF_KEY_ALWAYS_SEND, true);
                    }

                    /* Send every pending report. */
                    Iterator<Map.Entry<UUID, ErrorLogReport>> unprocessedIterator = mUnprocessedErrorReports.entrySet().iterator();
                    while (unprocessedIterator.hasNext()) {

                        /* Send report. */
                        Map.Entry<UUID, ErrorLogReport> unprocessedEntry = unprocessedIterator.next();
                        ErrorLogReport errorLogReport = unprocessedEntry.getValue();
                        mChannel.enqueue(errorLogReport.log, ERROR_GROUP);

                        /* Get attachments from callback in automatic processing. */
                        if (mAutomaticProcessing) {
                            Iterable<ErrorAttachmentLog> attachments = mCrashesListener.getErrorAttachments(errorLogReport.report);
                            sendErrorAttachment(errorLogReport.log.getId(), attachments);
                        }

                        /* Clean up an error log file and map entry. */
                        unprocessedIterator.remove();
                        ErrorLogHelper.removeStoredErrorLogFile(unprocessedEntry.getKey());
                    }
                }
            }
        });
    }

    /**
     * Send error attachment logs through channel.
     */
    private void sendErrorAttachment(UUID errorId, Iterable<ErrorAttachmentLog> attachments) {
        if (attachments == null) {
            MobileCenterLog.debug(LOG_TAG, "CrashesListener.getErrorAttachments returned null, no additional information will be attached to log: " + errorId.toString());
        } else {
            int totalErrorAttachments = 0;
            for (ErrorAttachmentLog attachment : attachments) {
                if (attachment != null) {
                    attachment.setId(UUID.randomUUID());
                    attachment.setErrorId(errorId);
                    if (attachment.isValid()) {
                        ++totalErrorAttachments;
                        mChannel.enqueue(attachment, ERROR_GROUP);
                    } else {
                        MobileCenterLog.error(LOG_TAG, "Not all required fields are present in ErrorAttachmentLog.");
                    }
                } else {
                    MobileCenterLog.warn(LOG_TAG, "Skipping null ErrorAttachmentLog in CrashesListener.getErrorAttachments.");
                }
            }
            if (totalErrorAttachments > MAX_ATTACHMENT_PER_CRASH) {
                MobileCenterLog.warn(LOG_TAG, "A limit of " + MAX_ATTACHMENT_PER_CRASH + " attachments per error report might be enforced by server.");
            }
        }
    }

    @VisibleForTesting
    void setLogSerializer(LogSerializer logSerializer) {
        mLogSerializer = logSerializer;
    }

    /**
     * Save a crash.
     *
     * @param thread    thread where crash occurred.
     * @param throwable uncaught exception or error.
     */
    void saveUncaughtException(Thread thread, Throwable throwable) {
        try {
            saveUncaughtException(thread, throwable, ErrorLogHelper.getModelExceptionFromThrowable(throwable));
        } catch (JSONException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error serializing error log to JSON", e);
        } catch (IOException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Error writing error log to file", e);
        }
    }

    /**
     * Save uncaught exception to disk.
     *
     * @param thread         thread where exception occurred.
     * @param throwable      Java exception as is, can be null for non Java exceptions.
     * @param modelException model exception, supports any language.
     * @return error log identifier.
     * @throws JSONException if an error occurred during JSON serialization of modelException.
     * @throws IOException   if an error occurred while accessing the file system.
     */
    UUID saveUncaughtException(Thread thread, Throwable throwable, com.microsoft.azure.mobile.crashes.ingestion.models.Exception modelException) throws JSONException, IOException {

        /* Ignore call if Crash is disabled. */
        if (!Crashes.isEnabled().get()) {
            return null;
        }

        /*
         * Save only 1 crash. This is needed for example in Xamarin
         * where we save as a Xamarin crash before Java handler is called.
         */
        if (mSavedUncaughtException) {
            return null;
        }
        mSavedUncaughtException = true;

        /* Save error log. */
        ManagedErrorLog errorLog = ErrorLogHelper.createErrorLog(mContext, thread, modelException, Thread.getAllStackTraces(), mInitializeTimestamp, true);
        File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
        UUID errorLogId = errorLog.getId();
        String filename = errorLogId.toString();
        MobileCenterLog.debug(Crashes.LOG_TAG, "Saving uncaught exception.");
        File errorLogFile = new File(errorStorageDirectory, filename + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);
        String errorLogString = mLogSerializer.serializeLog(errorLog);
        StorageHelper.InternalStorage.write(errorLogFile, errorLogString);
        MobileCenterLog.debug(Crashes.LOG_TAG, "Saved JSON content for ingestion into " + errorLogFile);
        File throwableFile = new File(errorStorageDirectory, filename + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
        if (throwable != null) {
            StorageHelper.InternalStorage.writeObject(throwableFile, throwable);
            MobileCenterLog.debug(Crashes.LOG_TAG, "Saved Throwable as is for client side inspection in " + throwableFile);
        } else {

            /*
             * If there is no Java Throwable to save as is (typical in wrapper SDKs),
             * use file placeholder as we also use this file to manage state.
             */
            if (!throwableFile.createNewFile()) {
                throw new IOException(throwableFile.getName());
            }
            MobileCenterLog.debug(Crashes.LOG_TAG, "Saved empty Throwable file in " + throwableFile);
        }
        return errorLogId;
    }

    /**
     * Implementation of {@link WrapperSdkExceptionManager#setAutomaticProcessing(boolean)}.
     */
    void setAutomaticProcessing(boolean automaticProcessing) {
        mAutomaticProcessing = automaticProcessing;
    }

    /**
     * Implementation of {@link WrapperSdkExceptionManager#getUnprocessedErrorReports()}.
     */
    MobileCenterFuture<Collection<ErrorReport>> getUnprocessedErrorReports() {
        final DefaultMobileCenterFuture<Collection<ErrorReport>> future = new DefaultMobileCenterFuture<>();
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                Collection<ErrorReport> reports = new ArrayList<>(mUnprocessedErrorReports.size());
                for (ErrorLogReport entry : mUnprocessedErrorReports.values()) {
                    reports.add(entry.report);
                }
                future.complete(reports);
            }
        }, future, Collections.<ErrorReport>emptyList());
        return future;
    }

    /**
     * Implementation of {@link WrapperSdkExceptionManager#sendCrashReportsOrAwaitUserConfirmation(Collection)}.
     */
    MobileCenterFuture<Boolean> sendCrashReportsOrAwaitUserConfirmation(final Collection<String> filteredReportIds) {
        final DefaultMobileCenterFuture<Boolean> future = new DefaultMobileCenterFuture<>();
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {

                /* Apply the filtering. */
                Iterator<Map.Entry<UUID, ErrorLogReport>> iterator = mUnprocessedErrorReports.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<UUID, ErrorLogReport> entry = iterator.next();
                    UUID id = entry.getKey();
                    String idString = entry.getValue().report.getId();
                    if (filteredReportIds != null && filteredReportIds.contains(idString)) {
                        MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned true, continue processing log: " + idString);
                    } else {
                        MobileCenterLog.debug(LOG_TAG, "CrashesListener.shouldProcess returned false, clean up and ignore log: " + idString);
                        removeAllStoredErrorLogFiles(id);
                        iterator.remove();
                    }
                }

                /* Proceed to check if user confirmation is needed. */
                future.complete(sendCrashReportsOrAwaitUserConfirmation());
            }
        }, future, false);
        return future;
    }

    /**
     * Implementation of {@link WrapperSdkExceptionManager#sendErrorAttachments(String, Iterable)}.
     */
    void sendErrorAttachments(final String errorReportId, final Iterable<ErrorAttachmentLog> attachments) {
        post(new Runnable() {

            @Override
            public void run() {

                /* Check error identifier format. */
                UUID errorId;
                try {
                    errorId = UUID.fromString(errorReportId);
                } catch (RuntimeException e) {
                    MobileCenterLog.error(LOG_TAG, "Error report identifier has an invalid format for sending attachments.");
                    return;
                }

                /* Send them. */
                sendErrorAttachment(errorId, attachments);
            }
        });
    }

    /**
     * Interface to use a template method to build exception model since it's a complex operation
     * and should be called only after all enabled checks have been done.
     */
    private interface ExceptionModelBuilder {

        /**
         * Get model exception.
         *
         * @return model exception.
         */
        com.microsoft.azure.mobile.crashes.ingestion.models.Exception buildExceptionModel();
    }

    /**
     * Callback template method.
     */
    private interface CallbackProcessor {

        /**
         * @return true to delete the stored serialized throwable file.
         */
        boolean shouldDeleteThrowable();

        /**
         * Execute call back.
         *
         * @param report error report related to the callback.
         */
        void onCallBack(ErrorReport report);
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
