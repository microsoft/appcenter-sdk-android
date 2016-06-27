package avalanche.crash;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import avalanche.base.Constants;
import avalanche.base.DefaultAvalancheFeature;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.Persistence;
import avalanche.base.utils.PersistenceListener;
import avalanche.base.utils.Util;
import avalanche.crash.ingestion.models.CrashLog;
import avalanche.crash.ingestion.models.Thread;
import avalanche.crash.model.CrashMetaData;
import avalanche.crash.model.CrashReport;

import static android.text.TextUtils.isEmpty;


public class Crashes extends DefaultAvalancheFeature {

    private static final String ALWAYS_SEND_KEY = "always_send_crash_reports";

    private static final int STACK_TRACES_FOUND_NONE = 0;
    private static final int STACK_TRACES_FOUND_NEW = 1;
    private static final int STACK_TRACES_FOUND_CONFIRMED = 2;

    private static Crashes sharedInstance = null;
    private CrashesListener mListener;
    private WeakReference<Context> mContextWeakReference;
    private long mInitializeTimestamp;
    private boolean mDidCrashInLastSession = false;

    private boolean mCrashReportingEnabled;

    protected Crashes() {
    }

    public static Crashes getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Crashes();
        }
        return sharedInstance;
    }

    /**
     * Searches .stacktrace files and returns them as array.
     */
    private static String[] searchForStackTraces() {
        if (Constants.FILES_PATH != null) {
            AvalancheLog.debug("Looking for exceptions in: " + Constants.FILES_PATH);

            // Try to create the files folder if it doesn't exist
            File dir = new File(Constants.FILES_PATH + "/");
            boolean created = dir.mkdir();
            if (!created && !dir.exists()) {
                return new String[0];
            }

            // Filter for ".stacktrace" files
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".stacktrace");
                }
            };
            return dir.list(filter);
        } else {
            AvalancheLog.debug("Can't search for exception as file path is null.");
            return null;
        }
    }

    private static List<String> getConfirmedFilenames(Context context) {
        List<String> result = null;
        if (context != null) {
            SharedPreferences preferences = context.getSharedPreferences("AvalancheSDK", Context.MODE_PRIVATE);
            result = Arrays.asList(preferences.getString("ConfirmedFilenames", "").split("\\|"));
        }
        return result;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        super.onActivityResumed(activity);
        if (mContextWeakReference == null && Util.isMainActivity(activity)) {
            // Opinionated approach -> per default we will want to activate the crash reporting with the very first of your activities.
            register(activity);
        }
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * AvalancheHub app identifier is read from configuration values in manifest.
     *
     * @param context The context to use. Usually your Activity object. If
     *                context is not an instance of Activity (or a subclass of it),
     *                crashes will be sent automatically.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context) {
        return register(context, null);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * AvalancheHub app identifier is read from configuration values in manifest.
     *
     * @param context  The context to use. Usually your Activity object. If
     *                 context is not an instance of Activity (or a subclass of it),
     *                 crashes will be sent automatically.
     * @param listener Implement this for callback functions.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context, CrashesListener listener) {
        mContextWeakReference = new WeakReference<>(context);
        mListener = listener;

        initialize();

        return this;
    }

    private void initialize() {
        if (mInitializeTimestamp == 0) {
            mInitializeTimestamp = System.currentTimeMillis();
        }

        //Write ALWAYS_SEND_KEY to preferences to send all crashes immediatelly
        Context context = mContextWeakReference.get();
        if (context != null) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putBoolean(ALWAYS_SEND_KEY, true).apply();
        }

        mCrashReportingEnabled = true;

        checkForCrashes();
        registerExceptionHandler();
    }

    public void enableCrashReporting() {
        mCrashReportingEnabled = true;
        registerExceptionHandler();
        checkForCrashes();
    }

    public void disableCrashReporting() {
        mCrashReportingEnabled = false;
        //TODO check mCrashReportingEnabled where apropriate and make sure we don't clutter the device with crashlogs.
        deleteStackTraces();
    }


    /**
     * Allows you to checkForCrashes the crash manager later on-demand.
     */
    private void checkForCrashes() {
        Context context = mContextWeakReference.get();
        if (context == null) {
            return;
        }

        int foundOrSend = hasStackTraces(context);

        if (foundOrSend == STACK_TRACES_FOUND_NEW) {
            mDidCrashInLastSession = true;
            if (mListener != null) {
                mListener.onNewCrashesFound();
            }
            sendCrashes(null);
        } else if (foundOrSend == STACK_TRACES_FOUND_CONFIRMED) {
            if (mListener != null) {
                mListener.onConfirmedCrashesFound();
            }

            sendCrashes(null);
        }
    }


    private void registerExceptionHandler() {
        if (!isEmpty(Constants.APP_VERSION) && !isEmpty(Constants.APP_PACKAGE)) {
            // Get current handler
            Thread.UncaughtExceptionHandler currentHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (currentHandler != null) {
                AvalancheLog.debug("Current handler class = " + currentHandler.getClass().getName());
            }

            // Update listener if already registered, otherwise set new handler
            if (currentHandler instanceof ExceptionHandler) {
                ((ExceptionHandler) currentHandler).setListener(mListener);
            } else {
                Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this, currentHandler, mListener, isIgnoreDefaultHandler()));
            }
        } else {
            AvalancheLog.warn("Exception handler not set because app version or package is null.");
        }
    }

    private void sendCrashes(final CrashMetaData crashMetaData) {
        saveConfirmedStackTraces();
        registerExceptionHandler();
        submitStackTraces(crashMetaData);
    }

    /**
     * Submits all stack traces in the files dir to AvalancheHub.
     *
     * @param crashMetaData The crashMetaData, provided by the user.
     */
    private void submitStackTraces(CrashMetaData crashMetaData) {
        String[] list = searchForStackTraces();
        Boolean successful = false;

        Context context = mContextWeakReference.get();

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (int index = 0; index < list.length; index++) {
                // Read contents of stack trace
                final String filename = list[index];
                String stacktrace = Util.contentsOfFile(context, filename);
                if (stacktrace.length() > 0) {
                    AvalancheLog.debug("Forwarding crash data: \n" + stacktrace);

                    // Retrieve user ID and contact information if given
                    String userID = Util.contentsOfFile(context, filename.replace(".stacktrace", ".user"));
                    String contact = Util.contentsOfFile(context, filename.replace(".stacktrace", ".contact"));

                    if (crashMetaData != null) {
                        final String crashMetaDataUserID = crashMetaData.getUserID();
                        if (!isEmpty(crashMetaDataUserID)) {
                            userID = crashMetaDataUserID;
                        }
                        final String crashMetaDataContact = crashMetaData.getUserEmail();
                        if (!isEmpty(crashMetaDataContact)) {
                            contact = crashMetaDataContact;
                        }
                    }

                    //TODO Crash Object Generation here
                    CrashLog crashLog = new CrashLog();


                    // Append application log to user provided description if present, if not, just send application log
                    final String applicationLog = Util.contentsOfFile(context, filename.replace(".stacktrace", ".description"));
                    String description = crashMetaData != null ? crashMetaData.getUserDescription() : "";
                    if (!isEmpty(applicationLog)) {
                        if (!isEmpty(description)) {
                            description = String.format("%s\n\nLog:\n%s", description, applicationLog);
                        } else {
                            description = String.format("Log:\n%s", applicationLog);
                        }
                    }

                    //We'll forward the final crash object to Persistence for processing
                    Persistence.getInstance().setListener(new PersistenceListener() {
                        @Override
                        public void storingSuccessful(boolean success) {
                            if (success) {
                                AvalancheLog.debug("Transmission succeeded");
                                deleteStackTrace(filename);

                                if (mListener != null) {
                                    mListener.onCrashesSent();
                                }
                            } else {
                                AvalancheLog.debug("Transmission failed, will retry on next register() call");
                                if (mListener != null) {
                                    mListener.onCrashesNotSent();
                                }
                            }

                        }
                    });
                    Persistence.getInstance().storeCrash(crashLog);
                }
            }
        }
    }


    long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    /**
     * Checks if there are any saved stack traces in the files dir.
     *
     * @param context The context to use. Usually your Activity object.
     * @return STACK_TRACES_FOUND_NONE if there are no stack traces,
     * STACK_TRACES_FOUND_NEW if there are any new stack traces,
     * STACK_TRACES_FOUND_CONFIRMED if there only are confirmed stack traces.
     */
    public int hasStackTraces(Context context) {
        String[] filenames = searchForStackTraces();
        List<String> confirmedFilenames = null;
        int result = STACK_TRACES_FOUND_NONE;
        if ((filenames != null) && (filenames.length > 0)) {
            try {
                confirmedFilenames = getConfirmedFilenames(context);

            } catch (Exception e) {
                // Just in case, we catch all exceptions here
            }

            if (confirmedFilenames != null) {
                result = STACK_TRACES_FOUND_CONFIRMED;

                for (String filename : filenames) {
                    if (!confirmedFilenames.contains(filename)) {
                        result = STACK_TRACES_FOUND_NEW;
                        break;
                    }
                }
            } else {
                result = STACK_TRACES_FOUND_NEW;
            }
        }

        return result;
    }

    public boolean didCrashInLastSession() {
        return mDidCrashInLastSession;
    }

    public CrashReport getLastCrashReport() {
        if (Constants.FILES_PATH == null || !didCrashInLastSession()) {
            return null;
        }

        File dir = new File(Constants.FILES_PATH + "/");
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".stacktrace");
            }
        });

        long lastModification = 0;
        File lastModifiedFile = null;
        CrashReport result = null;
        for (File file : files) {
            if (file.lastModified() > lastModification) {
                lastModification = file.lastModified();
                lastModifiedFile = file;
            }
        }

        if (lastModifiedFile != null && lastModifiedFile.exists()) {
            try {
                result = CrashReport.fromFile(lastModifiedFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private void saveConfirmedStackTraces() {
        Context context = mContextWeakReference.get();
        if (context != null) {
            try {
                String[] filenames = searchForStackTraces();
                SharedPreferences preferences = context.getSharedPreferences("AvalancheSDK", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("ConfirmedFilenames", Util.joinArray(filenames, "|"));
                editor.apply();
            } catch (Exception e) {
                // Just in case, we catch all exceptions here
            }
        }
    }

    //TODO Private needs to be called once crashes have been forwarded to the queue
    private void deleteStackTraces() {
        String[] list = searchForStackTraces();

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (int index = 0; index < list.length; index++) {
                try {
                    Context context = mContextWeakReference.get();
                    AvalancheLog.debug("Delete stacktrace " + list[index] + ".");
                    deleteStackTrace(list[index]);

                    if (context != null) {
                        context.deleteFile(list[index]);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Deletes the given filename and all corresponding files (same name,
     * different extension).
     */
    private void deleteStackTrace(String filename) {
        Context context = mContextWeakReference.get();
        context.deleteFile(filename);

        String user = filename.replace(".stacktrace", ".user");
        context.deleteFile(user);

        String contact = filename.replace(".stacktrace", ".contact");
        context.deleteFile(contact);

        String description = filename.replace(".stacktrace", ".description");
        context.deleteFile(description);
    }

    private boolean isIgnoreDefaultHandler() {
        return mListener != null && mListener.ignoreDefaultHandler();
    }
}
