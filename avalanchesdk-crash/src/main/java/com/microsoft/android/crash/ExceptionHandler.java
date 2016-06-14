package com.microsoft.android.crash;

import android.text.TextUtils;

import com.microsoft.android.Constants;
import com.microsoft.android.crash.model.CrashDetails;
import com.microsoft.android.utils.AvalancheLog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.UUID;

/**
 * <h3>Description</h3>
 * Helper class to catch exceptions. Saves the stack trace
 * as a file and executes callback methods to ask the app for
 * additional information and meta data (see CrashManagerListener).
 *
 **/
public class ExceptionHandler implements UncaughtExceptionHandler {
    private final CrashManager mCrashManager;
    private boolean mIgnoreDefaultHandler = false;
    private CrashManagerListener mCrashManagerListener;
    private UncaughtExceptionHandler mDefaultExceptionHandler;

    public ExceptionHandler(CrashManager crashManager, UncaughtExceptionHandler defaultExceptionHandler, CrashManagerListener listener, boolean ignoreDefaultHandler) {
        mCrashManager = crashManager;
        mDefaultExceptionHandler = defaultExceptionHandler;
        mIgnoreDefaultHandler = ignoreDefaultHandler;
        mCrashManagerListener = listener;
    }

    public void setListener(CrashManagerListener listener) {
        mCrashManagerListener = listener;
    }

    /**
     * Save a caught exception to disk.
     *
     * @param exception Exception to save.
     * @param thread    Thread that crashed.
     * @param listener  Custom CrashManager listener instance.
     */
    public void saveException(Throwable exception, Thread thread, CrashManagerListener listener) {
        final Date now = new Date();
        final Date startDate = new Date(mCrashManager.getInitializeTimestamp());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);

        String filename = UUID.randomUUID().toString();

        CrashDetails crashDetails = new CrashDetails(filename, exception);
        crashDetails.setAppPackage(Constants.APP_PACKAGE);
        crashDetails.setAppVersionCode(Constants.APP_VERSION);
        crashDetails.setAppVersionName(Constants.APP_VERSION_NAME);
        crashDetails.setAppStartDate(startDate);
        crashDetails.setAppCrashDate(now);

        if ((listener == null) || (listener.includeDeviceData())) {
            crashDetails.setOsVersion(Constants.ANDROID_VERSION);
            crashDetails.setOsBuild(Constants.ANDROID_BUILD);
            crashDetails.setDeviceManufacturer(Constants.PHONE_MANUFACTURER);
            crashDetails.setDeviceModel(Constants.PHONE_MODEL);
        }

        if (thread != null && ((listener == null) || (listener.includeThreadDetails()))) {
            crashDetails.setThreadName(thread.getName() + "-" + thread.getId());
        }

        if (Constants.CRASH_IDENTIFIER != null && (listener == null || listener.includeDeviceIdentifier())) {
            crashDetails.setReporterKey(Constants.CRASH_IDENTIFIER);
        }

        crashDetails.writeCrashReport();

        if (listener != null) {
            try {
                writeValueToFile(limitedString(listener.getUserID()), filename + ".user");
                writeValueToFile(limitedString(listener.getContact()), filename + ".contact");
                writeValueToFile(listener.getDescription(), filename + ".description");
            } catch (IOException e) {
                AvalancheLog.error("Error saving crash meta data!", e);
            }

        }
    }

    /**
     * Save java exception(s) caught by XamarinSDK to disk.
     *
     * @param exception              The native java exception to save.
     * @param managedExceptionString String representation of the full exception including the managed exception.
     * @param thread                 Thread that crashed.
     * @param listener               Custom CrashManager listener instance.
     */
    @SuppressWarnings("unused")
    public void saveNativeException(Throwable exception, String managedExceptionString, Thread thread, CrashManagerListener listener) {
        // the throwable will a "native" Java exception. In this case managedExceptionString contains the full, "unconverted" exception
        // which contains information about the managed exception, too. We don't want to loose that part. Sadly, passing a managed
        // exception as an additional throwable strips that info, so we pass in the full managed exception as a string
        // and extract the first part that contains the info about the managed code that was calling the java code.
        // In case there is no managedExceptionString, we just forward the java exception
        if (!TextUtils.isEmpty(managedExceptionString)) {
            String[] splits = managedExceptionString.split("--- End of managed exception stack trace ---", 2);
            if (splits != null && splits.length > 0) {
                managedExceptionString = splits[0];
            }
        }

        saveXamarinException(exception, thread, managedExceptionString, false, listener);
    }

    /**
     * Save managed exception(s) caught by XamarinSDK to disk.
     *
     * @param exception              The managed exception to save.
     * @param thread                 Thread that crashed.
     * @param listener               Custom CrashManager listener instance.
     */
    @SuppressWarnings("unused")
    public void saveManagedException(Throwable exception, Thread thread, CrashManagerListener listener) {
        saveXamarinException(exception, thread, null, true, listener);
    }

    private void saveXamarinException(Throwable exception, Thread thread, String additionalManagedException, Boolean isManagedException, CrashManagerListener listener) {
        final Date startDate = new Date(mCrashManager.getInitializeTimestamp());
        String filename = UUID.randomUUID().toString();
        final Date now = new Date();

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        if (exception != null) {
            exception.printStackTrace(printWriter);
        }

        CrashDetails crashDetails = new CrashDetails(filename, exception, additionalManagedException, isManagedException);
        crashDetails.setAppPackage(Constants.APP_PACKAGE);
        crashDetails.setAppVersionCode(Constants.APP_VERSION);
        crashDetails.setAppVersionName(Constants.APP_VERSION_NAME);
        crashDetails.setAppStartDate(startDate);
        crashDetails.setAppCrashDate(now);

        if ((listener == null) || (listener.includeDeviceData())) {
            crashDetails.setOsVersion(Constants.ANDROID_VERSION);
            crashDetails.setOsBuild(Constants.ANDROID_BUILD);
            crashDetails.setDeviceManufacturer(Constants.PHONE_MANUFACTURER);
            crashDetails.setDeviceModel(Constants.PHONE_MODEL);
        }

        if (thread != null && ((listener == null) || (listener.includeThreadDetails()))) {
            crashDetails.setThreadName(thread.getName() + "-" + thread.getId());
        }

        if (Constants.CRASH_IDENTIFIER != null && (listener == null || listener.includeDeviceIdentifier())) {
            crashDetails.setReporterKey(Constants.CRASH_IDENTIFIER);
        }

        crashDetails.writeCrashReport();

        if (listener != null) {
            try {
                writeValueToFile(limitedString(listener.getUserID()), filename + ".user");
                writeValueToFile(limitedString(listener.getContact()), filename + ".contact");
                writeValueToFile(listener.getDescription(), filename + ".description");
            } catch (IOException e) {
                AvalancheLog.error("Error saving crash meta data!", e);
            }

        }
    }

    public void uncaughtException(Thread thread, Throwable exception) {
        if (Constants.FILES_PATH == null) {
            // If the files path is null, the exception can't be stored
            // Always call the default handler instead
            mDefaultExceptionHandler.uncaughtException(thread, exception);
        } else {
            saveException(exception, thread, mCrashManagerListener);

            if (!mIgnoreDefaultHandler) {
                mDefaultExceptionHandler.uncaughtException(thread, exception);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        }
    }

    private static void writeValueToFile(String value, String filename) throws IOException {
        if (TextUtils.isEmpty(value)) {
            return;
        }
        BufferedWriter writer = null;
        try {
            String path = Constants.FILES_PATH + "/" + filename;
            if (!TextUtils.isEmpty(value) && TextUtils.getTrimmedLength(value) > 0) {
                writer = new BufferedWriter(new FileWriter(path));
                writer.write(value);
                writer.flush();
            }
        } catch (IOException e) {
            // TODO: Handle exception here
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static String limitedString(String string) {
        if (!TextUtils.isEmpty(string) && string.length() > 255) {
            string = string.substring(0, 255);
        }
        return string;
    }
}
