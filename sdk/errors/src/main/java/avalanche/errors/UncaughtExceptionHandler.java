package avalanche.errors;

import android.os.Process;

import avalanche.errors.ingestion.models.ErrorLog;
import avalanche.errors.utils.ErrorLogHelper;

class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private boolean mIgnoreDefaultExceptionHandler = false;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    UncaughtExceptionHandler() {
        register();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (!ErrorReporting.isEnabled() && mDefaultUncaughtExceptionHandler != null) {
            mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
        } else {
            ErrorLog errorLog = ErrorLogHelper.createErrorLog(thread, exception, Thread.getAllStackTraces(), ErrorReporting.getInstance().getInitializeTimestamp());
            ErrorLogHelper.serializeErrorLog(errorLog);

            if (!mIgnoreDefaultExceptionHandler && mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }
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
