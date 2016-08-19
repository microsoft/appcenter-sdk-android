package avalanche.errors.utils;

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import avalanche.core.Constants;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.JavaException;
import avalanche.errors.ingestion.models.JavaStackFrame;
import avalanche.errors.ingestion.models.JavaThread;
import avalanche.errors.model.ErrorReport;

/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 */
public final class ErrorLogHelper {

    public static final String ERROR_LOG_FILE_EXTENSION = ".json";

    public static final String THROWABLE_FILE_EXTENSION = ".throwable";

    @VisibleForTesting
    static final String ERROR_DIRECTORY = "error";

    /**
     * Root directory for error log and throwable files.
     */
    private static File sErrorLogDirectory;

    @NonNull
    public static JavaErrorLog createErrorLog(@NonNull Context context, @NonNull final Thread thread, @NonNull final Throwable exception, @NonNull final Map<Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp) {

        /* Build error log with a unique identifier. */
        JavaErrorLog errorLog = new JavaErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());

        /* Set absolute current time. Will be correlated to session and converted to relative later. */
        errorLog.setToffset(System.currentTimeMillis());

        /* Snapshot device properties. */
        try {
            errorLog.setDevice(DeviceInfoHelper.getDeviceInfo(context));
        } catch (DeviceInfoHelper.DeviceInfoException e) {
            AvalancheLog.error("Could not attach device properties snapshot to error log, will attach at sending time", e);
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
        errorLog.setExceptions(getJavaExceptionsFromThrowable(exception));

        /* Attach thread states. */
        List<JavaThread> threads = new ArrayList<>(allStackTraces.size());
        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            JavaThread javaThread = new JavaThread();
            javaThread.setId(entry.getKey().getId());
            javaThread.setName(entry.getKey().getName());
            javaThread.setFrames(getJavaStackFramesFromStackTrace(entry.getValue()));
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
            AvalancheLog.info("Deleting throwable file " + file.getName());
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
            AvalancheLog.info("Deleting error log file " + file.getName());
            StorageHelper.InternalStorage.delete(file);
        }
    }

    @NonNull
    public static ErrorReport getErrorReportFromErrorLog(@NonNull JavaErrorLog log, Throwable throwable) {
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
    private static List<JavaException> getJavaExceptionsFromThrowable(@NonNull Throwable t) {
        List<JavaException> javaExceptions = new ArrayList<>();
        for (Throwable cause = t; cause != null; cause = cause.getCause()) {
            JavaException javaException = new JavaException();
            javaException.setType(cause.getClass().getName());
            javaException.setMessage(cause.getMessage());
            javaException.setFrames(getJavaStackFramesFromStackTrace(cause.getStackTrace()));
            javaExceptions.add(javaException);
        }
        return javaExceptions;
    }

    @NonNull
    private static List<JavaStackFrame> getJavaStackFramesFromStackTrace(@NonNull StackTraceElement[] stackTrace) {
        List<JavaStackFrame> javaStackFrames = new ArrayList<>();
        for (StackTraceElement stackTraceElement : stackTrace) {
            JavaStackFrame javaStackFrame = new JavaStackFrame();
            javaStackFrame.setClassName(stackTraceElement.getClassName());
            javaStackFrame.setMethodName(stackTraceElement.getMethodName());
            javaStackFrame.setLineNumber(stackTraceElement.getLineNumber());
            javaStackFrame.setFileName(stackTraceElement.getFileName());
            javaStackFrames.add(javaStackFrame);
        }
        return javaStackFrames;
    }
}
