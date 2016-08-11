package avalanche.errors.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import avalanche.core.Constants;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.DeviceInfoHelper;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.JavaException;
import avalanche.errors.ingestion.models.JavaStackFrame;
import avalanche.errors.ingestion.models.JavaThread;

/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 */
public final class ErrorLogHelper {

    private static final String ERROR_DIRECTORY = "error";

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
        for (ActivityManager.RunningAppProcessInfo info : activityManager.getRunningAppProcesses())
            if (info.pid == Process.myPid())
                errorLog.setProcessName(info.processName);

        /* CPU architecture. */
        errorLog.setArchitecture(getArchitecture());

        /* Thread in error information. */
        errorLog.setErrorThreadId(thread.getId());
        errorLog.setErrorThreadName(thread.getName());

        /* For now we monitor only uncaught exceptions: a crash, fatal. */
        errorLog.setFatal(true);

        /* Relative application launch time to error time. */
        errorLog.setAppLaunchTOffset(System.currentTimeMillis() - initializeTimestamp);

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
    private static String getArchitecture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    @NonNull
    public static File getErrorStorageDirectory() {
        File errorLogDirectory = new File(Constants.FILES_PATH, ERROR_DIRECTORY);
        StorageHelper.InternalStorage.mkdir(errorLogDirectory.getAbsolutePath());
        return errorLogDirectory;
    }

    @NonNull
    public static File[] getStoredErrorLogFiles() {
        File[] files = getErrorStorageDirectory().listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });

        return files != null ? files : new File[0];
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
