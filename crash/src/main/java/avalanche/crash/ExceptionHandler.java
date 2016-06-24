package avalanche.crash;

import android.text.TextUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.UUID;

import avalanche.base.Channel;
import avalanche.base.Constants;
import avalanche.base.utils.AvalancheLog;
import avalanche.crash.model.CrashReport;

/**
 * <h3>Description</h3>
 * Helper class to catch exceptions. Saves the stack trace
 * as a file and executes callback methods to ask the app for
 * additional information and meta data (see CrashesListener).
 **/
public class ExceptionHandler implements UncaughtExceptionHandler {
    private final Crashes mCrashes;
    private boolean mIgnoreDefaultHandler = false;
    private CrashesListener mCrashesListener;
    private UncaughtExceptionHandler mDefaultExceptionHandler;

    public ExceptionHandler(Crashes crashes, UncaughtExceptionHandler defaultExceptionHandler, CrashesListener listener, boolean ignoreDefaultHandler) {
        mCrashes = crashes;
        mDefaultExceptionHandler = defaultExceptionHandler;
        mIgnoreDefaultHandler = ignoreDefaultHandler;
        mCrashesListener = listener;
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

    public void setListener(CrashesListener listener) {
        mCrashesListener = listener;
    }

    /**
     * Save a caught exception to disk.
     *
     * @param exception Exception to save.
     * @param thread    Thread that crashed.
     * @param listener  Custom Crashes listener instance.
     */
    public void saveException(Throwable exception, Thread thread, CrashesListener listener) {
        final Date now = new Date();
        final Date startDate = new Date(mCrashes.getInitializeTimestamp());
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        exception.printStackTrace(printWriter);

        String filename = UUID.randomUUID().toString();

        CrashReport crashReport = new CrashReport(filename, exception);
        crashReport.setAppPackage(Constants.APP_PACKAGE);
        crashReport.setAppVersionCode(Constants.APP_VERSION);
        crashReport.setAppVersionName(Constants.APP_VERSION_NAME);
        crashReport.setAppStartDate(startDate);
        crashReport.setAppCrashDate(now);

        if ((listener == null) || (listener.includeDeviceData())) {
            crashReport.setOsVersion(Constants.ANDROID_VERSION);
            crashReport.setOsBuild(Constants.ANDROID_BUILD);
            crashReport.setDeviceManufacturer(Constants.PHONE_MANUFACTURER);
            crashReport.setDeviceModel(Constants.PHONE_MODEL);
        }

        if (thread != null && ((listener == null) || (listener.includeThreadDetails()))) {
            crashReport.setThreadName(thread.getName() + "-" + thread.getId());
        }

        if (Constants.CRASH_IDENTIFIER != null && (listener == null || listener.includeDeviceIdentifier())) {
            crashReport.setReporterKey(Constants.CRASH_IDENTIFIER);
        }

        crashReport.writeCrashReport();

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
     * @param listener               Custom Crashes listener instance.
     */
    @SuppressWarnings("unused")
    public void saveNativeException(Throwable exception, String managedExceptionString, Thread thread, CrashesListener listener) {
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
     * @param exception The managed exception to save.
     * @param thread    Thread that crashed.
     * @param listener  Custom Crashes listener instance.
     */
    @SuppressWarnings("unused")
    public void saveManagedException(Throwable exception, Thread thread, CrashesListener listener) {
        saveXamarinException(exception, thread, null, true, listener);
    }

    //TODO refacture so we don't have duplicate code
    private void saveXamarinException(Throwable exception, Thread thread, String additionalManagedException, Boolean isManagedException, CrashesListener listener) {
        final Date startDate = new Date(mCrashes.getInitializeTimestamp());
        String filename = UUID.randomUUID().toString();
        final Date now = new Date();

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        if (exception != null) {
            exception.printStackTrace(printWriter);
        }


        //TODO move this to a Factory class
        CrashReport crashReport = new CrashReport(filename, exception, additionalManagedException, isManagedException);
        crashReport.setAppPackage(Constants.APP_PACKAGE);
        crashReport.setAppVersionCode(Constants.APP_VERSION);
        crashReport.setAppVersionName(Constants.APP_VERSION_NAME);
        crashReport.setAppStartDate(startDate);
        crashReport.setAppCrashDate(now);

        if ((listener == null) || (listener.includeDeviceData())) {
            crashReport.setOsVersion(Constants.ANDROID_VERSION);
            crashReport.setOsBuild(Constants.ANDROID_BUILD);
            crashReport.setDeviceManufacturer(Constants.PHONE_MANUFACTURER);
            crashReport.setDeviceModel(Constants.PHONE_MODEL);
        }

        if (thread != null && ((listener == null) || (listener.includeThreadDetails()))) {
            crashReport.setThreadName(thread.getName() + "-" + thread.getId());
        }

        if (Constants.CRASH_IDENTIFIER != null && (listener == null || listener.includeDeviceIdentifier())) {
            crashReport.setReporterKey(Constants.CRASH_IDENTIFIER);
        }

        crashReport.writeCrashReport();

        Channel.getInstance().handle(crashReport);

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

    //TODO: this should be the only method here, no persisting logic in here.
    public void uncaughtException(Thread thread, Throwable exception) {
        if (Constants.FILES_PATH == null) {
            // If the files path is null, the exception can't be stored
            // Always call the default handler instead
            mDefaultExceptionHandler.uncaughtException(thread, exception);
        } else {
            saveException(exception, thread, mCrashesListener);

            if (!mIgnoreDefaultHandler) {
                mDefaultExceptionHandler.uncaughtException(thread, exception);
            } else {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(10);
            }
        }
    }
}
