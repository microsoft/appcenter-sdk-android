package avalanche.errors;

import android.os.Process;

import org.json.JSONException;
import org.json.JSONStringer;

import java.io.File;
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

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private boolean mIgnoreDefaultExceptionHandler = false;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public UncaughtExceptionHandler() {
        register();
    }

    private static Exception getExceptionFromThrowable(Throwable t) {
        return getExceptionFromThrowable(t, 0);
    }

    private static Exception getExceptionFromThrowable(Throwable t, int id) {
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

    private static List<ThreadFrame> getThreadFramesFromStackTrace(StackTraceElement[] stackTrace) {
        List<ThreadFrame> threadFrames = new ArrayList<>();
        for (StackTraceElement e : stackTrace) {
            ThreadFrame frame = new ThreadFrame();
            frame.setSymbol(e.toString());
            threadFrames.add(frame);
        }
        return threadFrames;
    }

    private static void writeErrorLog(ErrorLog log) throws JSONException, IOException {
        JSONStringer writer = new JSONStringer();
        writer.object();
        log.write(writer);
        try {
            log.validate();
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
        writer.endObject();

        File crashesTemp = new File(Constants.FILES_PATH, "crash");
        if (!StorageHelper.InternalStorage.mkdir(crashesTemp.getAbsolutePath())) {
            // Could not create crashes temporary directory, can't write error log
            return;
        }
        File logFile = new File(crashesTemp, log.getId().toString() + ".json");
        StorageHelper.InternalStorage.write(logFile, writer.toString());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (Constants.FILES_PATH == null && mDefaultUncaughtExceptionHandler != null) {
            mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
        } else {
            saveException(thread, exception, Thread.getAllStackTraces());

            if (!mIgnoreDefaultExceptionHandler && mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }
    }

    public void saveException(final Thread thread, final Throwable exception, final Map<Thread, StackTraceElement[]> allStackTraces) {
        ErrorLog errorLog = new ErrorLog();
        errorLog.setId(UUIDUtils.randomUUID());
        /*
            - Parent process information intentionally left blank
            - applicationPath, exceptionType, exceptionCode, exceptionAddress intentionally left blank
            (Android does not provide any of this information)
         */

        // TODO Provide process name, probably hacky, maybe as per http://stackoverflow.com/questions/8542326/android-how-to-get-the-processname-or-packagename-by-using-pid

        errorLog.setProcessId(Process.myPid());
        errorLog.setCrashThread((int) thread.getId()); // TODO maybe redefine model value to be of type long
        errorLog.setAppLaunchTOffset(System.currentTimeMillis() - ErrorReporting.getInstance().getInitializeTimestamp());
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

        //noinspection TryWithIdenticalCatches
        try {
            writeErrorLog(errorLog);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void register() {
        if (!mIgnoreDefaultExceptionHandler) {
            mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void unregister() {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
    }
}
