package avalanche.errors.utils;

import android.os.Process;
import android.support.annotation.NonNull;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import avalanche.core.Constants;
import avalanche.core.utils.StorageHelper;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ingestion.models.ErrorLog;
import avalanche.errors.ingestion.models.Exception;
import avalanche.errors.ingestion.models.ThreadFrame;

/**
 * ErrorLogHelper to help constructing, serializing, and de-serializing locally stored error logs.
 */
public final class ErrorLogHelper {

    public static final String ERROR_DIRECTORY = "error";

    @NonNull
    public static ErrorLog createErrorLog(@NonNull final Thread thread, @NonNull final Throwable exception, @NonNull final Map<Thread, StackTraceElement[]> allStackTraces, final long initializeTimestamp) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        /*
            - Parent process information intentionally left blank
            - applicationPath, exceptionType, exceptionCode, exceptionAddress intentionally left blank
            (Android does not provide any of this information)
         */

        errorLog.setProcessId(Process.myPid());
        errorLog.setCrashThread((int) thread.getId()); // TODO maybe redefine model value to be of type long
        errorLog.setAppLaunchTOffset(System.currentTimeMillis() - initializeTimestamp);
        errorLog.setExceptionType(exception.getClass().getName());
        errorLog.setExceptionReason(exception.getMessage());
        errorLog.setFatal(true);

        for (Map.Entry<Thread, StackTraceElement[]> entry : allStackTraces.entrySet()) {
            avalanche.errors.ingestion.models.Thread t = new avalanche.errors.ingestion.models.Thread();
            t.setId((int) entry.getKey().getId());
            t.setFrames(getThreadFramesFromStackTrace(entry.getValue()));
        }

        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(getExceptionFromThrowable(exception));
        errorLog.setExceptions(exceptions);

        return errorLog;
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
    private static Exception getExceptionFromThrowable(@NonNull Throwable t) {
        return getExceptionFromThrowable(t, 0);
    }

    @NonNull
    private static Exception getExceptionFromThrowable(@NonNull Throwable t, int id) {
        Exception result = new Exception();
        result.setId(id);
        result.setFrames(getThreadFramesFromStackTrace(t.getStackTrace()));
        result.setLanguage("Java");
        result.setReason(t.getMessage());

        if (t.getCause() != null) {
            List<Exception> innerExceptions = new ArrayList<>();
            innerExceptions.add(getExceptionFromThrowable(t.getCause(), id + 1));
            result.setInnerExceptions(innerExceptions);
        }

        return result;
    }

    @NonNull
    private static List<ThreadFrame> getThreadFramesFromStackTrace(@NonNull StackTraceElement[] stackTrace) {
        List<ThreadFrame> threadFrames = new ArrayList<>();
        for (StackTraceElement e : stackTrace) {
            ThreadFrame frame = new ThreadFrame();
            frame.setSymbol(e.toString());
            threadFrames.add(frame);
        }
        return threadFrames;
    }

}
