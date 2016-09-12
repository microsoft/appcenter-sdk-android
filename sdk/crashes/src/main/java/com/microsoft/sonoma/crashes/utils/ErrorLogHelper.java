package com.microsoft.sonoma.crashes.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.sonoma.core.Constants;
import com.microsoft.sonoma.core.utils.DeviceInfoHelper;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.StorageHelper;
import com.microsoft.sonoma.core.utils.UUIDUtils;
import com.microsoft.sonoma.crashes.Crashes;
import com.microsoft.sonoma.crashes.ingestion.models.Exception;
import com.microsoft.sonoma.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.sonoma.crashes.ingestion.models.StackFrame;
import com.microsoft.sonoma.crashes.ingestion.models.Thread;
import com.microsoft.sonoma.crashes.model.ErrorReport;

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

    public static final String ERROR_LOG_FILE_EXTENSION = ".json";

    public static final String THROWABLE_FILE_EXTENSION = ".throwable";

    @VisibleForTesting
    static final String ERROR_DIRECTORY = "error";

    /**
     * Root directory for error log and throwable files.
     */
    private static File sErrorLogDirectory;

    @NonNull
    public static ManagedErrorLog createErrorLog(@NonNull Context context, @NonNull final java.lang.Thread thread, @NonNull final Throwable exception, @NonNull final Map<java.lang.Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp) {

        /* Build error log with a unique identifier. */
        ManagedErrorLog errorLog = new ManagedErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());

        /* Set absolute current time. Will be correlated to session and converted to relative later. */
        errorLog.setToffset(System.currentTimeMillis());

        /* Snapshot device properties. */
        try {
            errorLog.setDevice(DeviceInfoHelper.getDeviceInfo(context));
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            SonomaLog.error(Crashes.LOG_TAG, "Could not attach device properties snapshot to error log, will attach at sending time", e);
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

        /* For now we monitor only uncaught exceptions: a crash, fatal. */
        errorLog.setFatal(true);

        /* Relative application launch time to error time. */
        errorLog.setAppLaunchTOffset(SystemClock.elapsedRealtime() - initializeTimestamp);

        /* Attach exceptions. */
        errorLog.setException(getModelExceptionFromThrowable(exception));

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
    public static File getErrorStorageDirectory() {
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
            SonomaLog.info(Crashes.LOG_TAG, "Deleting throwable file " + file.getName());
            StorageHelper.InternalStorage.delete(file);
        }
    }

    @Nullable
    public static File getStoredErrorLogFile(@NonNull UUID id) {
        return getStoredFile(id, ERROR_LOG_FILE_EXTENSION);
    }

    public static void removeStoredErrorLogFile(@NonNull UUID id) {
        File file = getStoredErrorLogFile(id);
        if (file != null) {
            SonomaLog.info(Crashes.LOG_TAG, "Deleting error log file " + file.getName());
            StorageHelper.InternalStorage.delete(file);
        }
    }

    @NonNull
    public static ErrorReport getErrorReportFromErrorLog(@NonNull ManagedErrorLog log, Throwable throwable) {
        ErrorReport report = new ErrorReport();
        report.setId(log.getId().toString());
        report.setThreadName(log.getErrorThreadName());
        report.setThrowable(throwable);
        report.setAppStartTime(new Date(log.getToffset() - log.getAppLaunchTOffset()));
        report.setAppErrorTime(new Date(log.getToffset()));
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
    private static Exception getModelExceptionFromThrowable(@NonNull Throwable t) {
        Exception topException = null;
        Exception parentException = null;
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            Exception exception = new Exception();
            exception.setType(cause.getClass().getName());
            exception.setMessage(cause.getMessage());
            exception.setFrames(getModelFramesFromStackTrace(cause.getStackTrace()));
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
    private static List<StackFrame> getModelFramesFromStackTrace(@NonNull StackTraceElement[] stackTrace) {
        List<StackFrame> stackFrames = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            StackFrame stackFrame = new StackFrame();
            stackFrame.setClassName(stackTraceElement.getClassName());
            stackFrame.setMethodName(stackTraceElement.getMethodName());
            stackFrame.setLineNumber(stackTraceElement.getLineNumber());
            stackFrame.setFileName(stackTraceElement.getFileName());
            stackFrames.add(stackFrame);
        }
        return stackFrames;
    }
}
