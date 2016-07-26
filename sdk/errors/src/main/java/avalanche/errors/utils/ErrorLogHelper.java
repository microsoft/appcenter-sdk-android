package avalanche.errors.utils;

import android.os.Process;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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

    public static void serializeErrorLog(@NonNull ErrorLog errorLog) {

        File errorLogDirectory = getErrorStorageDirectory();
        if (!StorageHelper.InternalStorage.mkdir(errorLogDirectory.getAbsolutePath())) {
            // Could not create crashes temporary directory, can't write error log
            return;
        }
        File logFile = new File(errorLogDirectory, errorLog.getId().toString() + ".json");

        //noinspection TryWithIdenticalCatches
        try {
            writeErrorLog(errorLog, logFile.getAbsolutePath());
        } catch (JSONException e) { // TODO error handling
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NonNull
    private static File getErrorStorageDirectory() {
        return new File(Constants.FILES_PATH, ERROR_DIRECTORY);
    }

    @NonNull
    public static String[] getStoredErrorLogs() {
        return StorageHelper.InternalStorage.getFilenames(getErrorStorageDirectory().getAbsolutePath(), new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });
    }

    @Nullable
    public static ErrorLog getLastErrorLog() {
        File logfile = StorageHelper.InternalStorage.lastModifiedFile(getErrorStorageDirectory().getAbsolutePath(), new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".json");
            }
        });

        if (logfile == null) {
            return null;
        }

        return deserializeErrorLog(logfile.getAbsolutePath());
    }

    @Nullable
    public static ErrorLog deserializeErrorLog(@NonNull String logfile) {
        String logfileContents = StorageHelper.InternalStorage.read(logfile);
        if (TextUtils.isEmpty(logfileContents)) {
            return null;
        }

        ErrorLog errorLog = new ErrorLog();
        try {
            errorLog.read(new JSONObject(logfileContents));
        } catch (JSONException e) {
            e.printStackTrace(); // TODO Error handling
        }
        return errorLog;
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

    private static void writeErrorLog(@NonNull ErrorLog log, @NonNull String logfile) throws JSONException, IOException {
        JSONStringer writer = new JSONStringer();
        writer.object();
        log.write(writer);
        try {
            log.validate();
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
        writer.endObject();


        StorageHelper.InternalStorage.write(logfile, writer.toString());
    }

}
