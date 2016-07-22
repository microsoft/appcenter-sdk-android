package avalanche.errors;

import android.os.Process;

import java.util.Set;

import avalanche.core.Constants;
import avalanche.core.utils.UUIDUtils;
import avalanche.errors.ingestion.models.ErrorLog;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private boolean mIgnoreDefaultExceptionHandler = false;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public UncaughtExceptionHandler() {
        register();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (Constants.FILES_PATH == null && mDefaultUncaughtExceptionHandler != null) {
            mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
        } else {
            saveException(thread, exception);

            if (!mIgnoreDefaultExceptionHandler && mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
        }
    }

    private void saveException(Thread thread, Throwable exception) {
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

        Set<Thread> allLiveThreads = Thread.getAllStackTraces().keySet();
        for (Thread t : allLiveThreads) {

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
