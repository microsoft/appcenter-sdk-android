package avalanche.errors;

import android.os.Process;

import avalanche.core.Constants;

public class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private boolean mIgnoreDefaultExceptionHandler = false;
    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    public UncaughtExceptionHandler() {

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
