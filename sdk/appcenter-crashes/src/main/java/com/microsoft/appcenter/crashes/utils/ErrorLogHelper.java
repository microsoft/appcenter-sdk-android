package com.microsoft.appcenter.crashes.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.crashes.ingestion.models.Exception;
import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.crashes.ingestion.models.StackFrame;
import com.microsoft.appcenter.crashes.ingestion.models.Thread;
import com.microsoft.appcenter.crashes.model.ErrorReport;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.MobileCenterLog;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 */
public class ErrorLogHelper {

    /**
     * Error log file extension for the JSON schema.
     */
    public static final String ERROR_LOG_FILE_EXTENSION = ".json";

    /**
     * Error log file extension for the serialized throwable for client side inspection.
     */
    public static final String THROWABLE_FILE_EXTENSION = ".throwable";

    /**
     * For huge stack traces such as giant StackOverflowError, we keep only beginning and end of frames according to this limit.
     */
    @VisibleForTesting
    public static final int FRAME_LIMIT = 256;

    /**
     * Error log directory within application files.
     */
    @VisibleForTesting
    static final String ERROR_DIRECTORY = "error";

    /**
     * We keep the first half of the limit of frames from the beginning and the second half from end.
     */
    private static final int FRAME_LIMIT_HALF = FRAME_LIMIT / 2;

    /**
     * Root directory for error log and throwable files.
     */
    private static File sErrorLogDirectory;

    @NonNull
    public static ManagedErrorLog createErrorLog(@NonNull Context context, @NonNull final java.lang.Thread thread, @NonNull final Throwable throwable, @NonNull final Map<java.lang.Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp, boolean fatal) {
        return createErrorLog(context, thread, getModelExceptionFromThrowable(throwable), allStackTraces, initializeTimestamp, fatal);
    }

    @NonNull
    public static ManagedErrorLog createErrorLog(@NonNull Context context, @NonNull final java.lang.Thread thread, @NonNull final Exception exception, @NonNull final Map<java.lang.Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp, boolean fatal) {

        /* Build error log with a unique identifier. */
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());

        /* Set current time. Will be correlated to session after restart. */
        errorLog.setTimestamp(new Date());

        /* Snapshot device properties. */
        try {
            errorLog.setDevice(DeviceInfoHelper.getDeviceInfo(context));
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            MobileCenterLog.error(Crashes.LOG_TAG, "Could not attach device properties snapshot to error log, will attach at sending time", e);
        }

        /* Process information. Parent one is not available on Android. */
        errorLog.setProcessId(Process.myPid());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningAppProcessInfo info : activityManager.getRunningAppProcesses()) {
                if (info.pid == Process.myPid()) {
                    errorLog.setProcessName(info.processName);
                }
            }
        }

        /* CPU architecture. */
        errorLog.setArchitecture(getArchitecture());

        /* Thread in error information. */
        errorLog.setErrorThreadId(thread.getId());
        errorLog.setErrorThreadName(thread.getName());

        /* Uncaught exception or managed exception. */
        errorLog.setFatal(fatal);

        /* Application launch time. */
        errorLog.setAppLaunchTimestamp(new Date(initializeTimestamp));

        /* Attach exceptions. */
        errorLog.setException(exception);

        /* Attach thread states. */
        List<Thread> threads = new ArrayList<>(allStackTraces.size());
        for (Map.Entry<java.lang.Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            Thread javaThread = new Thread();
            javaThread.setId(entry.getKey().getId());
            javaThread.setName(entry.getKey().getName());
            javaThread.setFrames(getModelFramesFromStackTrace(entry.getValue()));
            threads.add(javaThread);
        }
        errorLog.setThreads(threads);
        return errorLog;
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getArchitecture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    @NonNull
    public static synchronized File getErrorStorageDirectory() {
        if (sErrorLogDirectory == null) {
            sErrorLogDirectory = new File(Constants.FILES_PATH, ERROR_DIRECTORY);
            StorageHelper.InternalStorage.mkdir(sErrorLogDirectory.getAbsolutePath());
        }
        return sErrorLogDirectory;
    }

    @NonNull
    public static File[] getStoredErrorLogFiles() {
        File[] files = getErrorStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(ERROR_LOG_FILE_EXTENSION);
            }
        });

        return files != null && files.length > 0 ? files : new File[0];
    }

    @Nullable
    public static File getLastErrorLogFile() {
        return StorageHelper.InternalStorage.lastModifiedFile(getErrorStorageDirectory(), new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(ERROR_LOG_FILE_EXTENSION);
            }
        });
    }

    @Nullable
    public static File getStoredThrowableFile(@NonNull UUID id) {
        return getStoredFile(id, THROWABLE_FILE_EXTENSION);
    }

    public static void removeStoredThrowableFile(@NonNull UUID id) {
        File file = getStoredThrowableFile(id);
        if (file != null) {
            MobileCenterLog.info(Crashes.LOG_TAG, "Deleting throwable file " + file.getName());
            StorageHelper.InternalStorage.delete(file);
        }
    }

    @Nullable
    static File getStoredErrorLogFile(@NonNull UUID id) {
        return getStoredFile(id, ERROR_LOG_FILE_EXTENSION);
    }

    public static void removeStoredErrorLogFile(@NonNull UUID id) {
        File file = getStoredErrorLogFile(id);
        if (file != null) {
            MobileCenterLog.info(Crashes.LOG_TAG, "Deleting error log file " + file.getName());
            StorageHelper.InternalStorage.delete(file);
        }
    }

    @NonNull
    public static ErrorReport getErrorReportFromErrorLog(@NonNull ManagedErrorLog log, Throwable throwable) {
        ErrorReport report = new ErrorReport();
        report.setId(log.getId().toString());
        report.setThreadName(log.getErrorThreadName());
        report.setThrowable(throwable);
        report.setAppStartTime(log.getAppLaunchTimestamp());
        report.setAppErrorTime(log.getTimestamp());
        report.setDevice(log.getDevice());
        return report;
    }

    @VisibleForTesting
    static void setErrorLogDirectory(File file) {
        sErrorLogDirectory = file;
    }

    @Nullable
    private static File getStoredFile(@NonNull final UUID id, @NonNull final String extension) {
        File[] files = getErrorStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith(id.toString()) && filename.endsWith(extension);
            }
        });

        return files != null && files.length > 0 ? files[0] : null;
    }

    @NonNull
    public static Exception getModelExceptionFromThrowable(@NonNull Throwable t) {
        Exception topException = null;
        Exception parentException = null;
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            Exception exception = new Exception();
            exception.setType(cause.getClass().getName());
            exception.setMessage(cause.getMessage());
            exception.setFrames(getModelFramesFromStackTrace(cause));
            if (topException == null) {
                topException = exception;
            } else {
                parentException.setInnerExceptions(Collections.singletonList(exception));
            }
            parentException = exception;
        }
        return topException;
    }

    @NonNull
    private static List<StackFrame> getModelFramesFromStackTrace(@NonNull Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length > FRAME_LIMIT) {
            StackTraceElement[] stackTraceTruncated = new StackTraceElement[FRAME_LIMIT];
            System.arraycopy(stackTrace, 0, stackTraceTruncated, 0, FRAME_LIMIT_HALF);
            System.arraycopy(stackTrace, stackTrace.length - FRAME_LIMIT_HALF, stackTraceTruncated, FRAME_LIMIT_HALF, FRAME_LIMIT_HALF);
            throwable.setStackTrace(stackTraceTruncated);
            MobileCenterLog.warn(Crashes.LOG_TAG, "Crash frames truncated from " + stackTrace.length + " to " + stackTraceTruncated.length + " frames.");
            stackTrace = stackTraceTruncated;
        }
        return getModelFramesFromStackTrace(stackTrace);
    }

    @NonNull
    private static List<StackFrame> getModelFramesFromStackTrace(@NonNull StackTraceElement[] stackTrace) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stackFrames.add(getModelStackFrame(stackTraceElement));
        }
        return stackFrames;
    }

    @NonNull
    private static StackFrame getModelStackFrame(StackTraceElement stackTraceElement) {
        StackFrame stackFrame = new StackFrame();
        stackFrame.setClassName(stackTraceElement.getClassName());
        stackFrame.setMethodName(stackTraceElement.getMethodName());
        stackFrame.setLineNumber(stackTraceElement.getLineNumber());
        stackFrame.setFileName(stackTraceElement.getFileName());
        return stackFrame;
    }
}
