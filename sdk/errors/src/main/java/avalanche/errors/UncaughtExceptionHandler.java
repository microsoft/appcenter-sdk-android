package avalanche.errors;

import android.content.Context;
import android.os.Process;
import android.support.annotation.VisibleForTesting;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;

import avalanche.core.ingestion.models.json.DefaultLogSerializer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.StorageHelper;
import avalanche.errors.ingestion.models.JavaErrorLog;
import avalanche.errors.ingestion.models.json.JavaErrorLogFactory;
import avalanche.errors.utils.ErrorLogHelper;

class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Context mContext;

    private final LogSerializer mLogSerializer;

    private boolean mIgnoreDefaultExceptionHandler = false;

    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

    UncaughtExceptionHandler(Context context) {
        mContext = context;
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(JavaErrorLog.TYPE, JavaErrorLogFactory.getInstance());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable exception) {
        if (!ErrorReporting.isEnabled()) {
            if (mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            }
        } else {
            JavaErrorLog errorLog = ErrorLogHelper.createErrorLog(mContext, thread, exception, Thread.getAllStackTraces(), ErrorReporting.getInstance().getInitializeTimestamp());
            try {
                File errorStorageDirectory = ErrorLogHelper.getErrorStorageDirectory();
                String filename = errorLog.getId().toString();
                File errorLogFile = new File(errorStorageDirectory, filename + ErrorLogHelper.ERROR_LOG_FILE_EXTENSION);
                File throwableFile = new File(errorStorageDirectory, filename + ErrorLogHelper.THROWABLE_FILE_EXTENSION);
                String errorLogString = mLogSerializer.serializeLog(errorLog);
                AvalancheLog.debug("Saving uncaught exception:", exception);
                StorageHelper.InternalStorage.write(errorLogFile, errorLogString);
                AvalancheLog.debug("Saved JSON content for ingestion into " + errorLogFile);
                StorageHelper.InternalStorage.writeObject(throwableFile, exception);
                AvalancheLog.debug("Saved Throwable as is for client side inspection in " + throwableFile);
            } catch (JSONException e) {
                AvalancheLog.error("Error serializing error log to JSON", e);
            } catch (IOException e) {
                AvalancheLog.error("Error writing error log to file", e);
            }
            if (mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                ShutdownHelper.shutdown();
            }
        }
    }

    @VisibleForTesting
    void setIgnoreDefaultExceptionHandler(boolean ignoreDefaultExceptionHandler) {
        mIgnoreDefaultExceptionHandler = ignoreDefaultExceptionHandler;
        if (ignoreDefaultExceptionHandler) {
            mDefaultUncaughtExceptionHandler = null;
        }
    }

    @VisibleForTesting
    Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return mDefaultUncaughtExceptionHandler;
    }


    public void register() {
        if (!mIgnoreDefaultExceptionHandler) {
            mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        } else {
            mDefaultUncaughtExceptionHandler = null;
        }
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    public void unregister() {
        Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
    }

    @VisibleForTesting
    final static class ShutdownHelper {

        static void shutdown() {
            Process.killProcess(Process.myPid());
            System.exit(10);
        }
    }
}
