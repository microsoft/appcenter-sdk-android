package avalanche.crash;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.base.Avalanche;
import avalanche.base.Constants;
import avalanche.base.DefaultAvalancheFeature;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.HttpURLConnectionBuilder;
import avalanche.base.utils.Util;
import avalanche.crash.model.CrashMetaData;
import avalanche.crash.model.CrashReport;

import static android.text.TextUtils.isEmpty;
import static avalanche.base.Constants.BASE_URL;
import static avalanche.base.utils.StorageHelper.InternalStorage;
import static avalanche.base.utils.StorageHelper.PreferencesStorage;


public class Crashes extends DefaultAvalancheFeature {

    private static final String ALWAYS_SEND_KEY = "always_send_crash_reports";

    private static final int STACK_TRACES_FOUND_NONE = 0;
    private static final int STACK_TRACES_FOUND_NEW = 1;
    private static final int STACK_TRACES_FOUND_CONFIRMED = 2;
    private static Crashes sharedInstance = null;
    private CrashesListener mListener;
    private static final String mEndpointUrl = BASE_URL;
    private WeakReference<Context> mContextWeakReference;
    private boolean mLazyExecution;
    private boolean mIsSubmitting = false;
    private long mInitializeTimestamp;
    private boolean mDidCrashInLastSession = false;

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

            // Filter for ".stacktrace" files
            FilenameFilter filter = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".stacktrace");
                }
            };
            return InternalStorage.getFilenames(Constants.FILES_PATH, filter);
        } else {
            AvalancheLog.debug("Can't search for exception as file path is null.");
            return null;
        }
    }

    private static List<String> getConfirmedFilenames() {
        return Arrays.asList(PreferencesStorage.getString("ConfirmedFilenames", "").split("\\|"));
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
     * Avalanche app identifier is read from configuration values in manifest.
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
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context  The context to use. Usually your Activity object. If
     *                 context is not an instance of Activity (or a subclass of it),
     *                 crashes will be sent automatically.
     * @param listener Implement this for callback functions.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context, CrashesListener listener) {
        return register(context, listener, false);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * Avalanche app identifier is read from configuration values in manifest.
     *
     * @param context       The context to use. Usually your Activity object. If
     *                      context is not an instance of Activity (or a subclass of it),
     *                      crashes will be sent automatically.
     * @param listener      Implement this for callback functions.
     * @param lazyExecution Whether the manager should execute lazily, e.g. not check for crashes right away.
     * @return
     */
    public Crashes register(Context context, CrashesListener listener, boolean lazyExecution) {
        mContextWeakReference = new WeakReference<>(context);
        mListener = listener;
        mLazyExecution = lazyExecution;

        initialize();

        return this;
    }

    private void initialize() {
        Context context = mContextWeakReference.get();

        if (context != null) {
            if (mInitializeTimestamp == 0) {
                mInitializeTimestamp = System.currentTimeMillis();
            }

            Constants.loadFromContext(context);

            if (!mLazyExecution) {
                execute();
            }
        }
    }

    /**
     * Allows you to execute the crash manager later on-demand.
     */
    public void execute() {
        Context context = mContextWeakReference.get();
        if (context == null) {
            return;
        }

        int foundOrSend = hasStackTraces();

        if (foundOrSend == STACK_TRACES_FOUND_NEW) {
            mDidCrashInLastSession = true;
            Boolean autoSend = !(context instanceof Activity);
            autoSend |= PreferencesStorage.getBoolean(ALWAYS_SEND_KEY, false);

            if (mListener != null) {
                autoSend |= mListener.shouldAutoUploadCrashes();

                mListener.onNewCrashesFound();
            }

            sendCrashes();
        } else if (foundOrSend == STACK_TRACES_FOUND_CONFIRMED) {
            if (mListener != null) {
                mListener.onConfirmedCrashesFound();
            }

            sendCrashes();
        } else {
            registerExceptionHandler();
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

    private void sendCrashes() {
        sendCrashes(null);
    }

    private void sendCrashes(final CrashMetaData crashMetaData) {
        saveConfirmedStackTraces();
        registerExceptionHandler();

        Context context = mContextWeakReference.get();
        if (context != null && !Util.isConnectedToNetwork(context)) {
            // Not connected to network, not trying to submit stack traces
            return;
        }

        if (!mIsSubmitting) {
            mIsSubmitting = true;

            new Thread() {
                @Override
                public void run() {
                    submitStackTraces(crashMetaData);
                    mIsSubmitting = false;
                }
            }.start();
        }
    }

    /**
     * Submits all stack traces in the files dir to Avalanche.
     *
     * @param crashMetaData The crashMetaData, provided by the user.
     */
    public void submitStackTraces(CrashMetaData crashMetaData) {
        String[] list = searchForStackTraces();
        Boolean successful = false;

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (int index = 0; index < list.length; index++) {
                HttpURLConnection urlConnection = null;
                try {
                    // Read contents of stack trace
                    String filename = Constants.FILES_PATH + "/" + list[index];
                    String stacktrace = InternalStorage.read(filename);
                    if (stacktrace.length() > 0) {
                        // Transmit stack trace with POST request

                        AvalancheLog.debug("Transmitting crash data: \n" + stacktrace);

                        Map<String, String> parameters = new HashMap<String, String>();

                        parameters.put("raw", stacktrace);
                        parameters.put("userID", "");
                        parameters.put("contact", "");
                        parameters.put("description", "");
                        parameters.put("sdk", Constants.SDK_NAME);
                        parameters.put("sdk_version", BuildConfig.VERSION_NAME);

                        urlConnection = new HttpURLConnectionBuilder(getURLString())
                                .setRequestMethod("POST")
                                .writeFormFields(parameters)
                                .build();

                        int responseCode = urlConnection.getResponseCode();

                        successful = (responseCode == HttpURLConnection.HTTP_ACCEPTED || responseCode == HttpURLConnection.HTTP_CREATED);

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (successful) {
                        AvalancheLog.debug("Transmission succeeded");
                        deleteStackTrace(list[index]);

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
            }
        }
    }


    long getInitializeTimestamp() {
        return mInitializeTimestamp;
    }

    /**
     * Checks if there are any saved stack traces in the files dir.
     *
     * @return STACK_TRACES_FOUND_NONE if there are no stack traces,
     * STACK_TRACES_FOUND_NEW if there are any new stack traces,
     * STACK_TRACES_FOUND_CONFIRMED if there only are confirmed stack traces.
     */
    public int hasStackTraces() {
        String[] filenames = searchForStackTraces();
        List<String> confirmedFilenames = null;
        int result = STACK_TRACES_FOUND_NONE;
        if ((filenames != null) && (filenames.length > 0)) {
            try {
                confirmedFilenames = getConfirmedFilenames();

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

    public CrashReport getLastCrashDetails() {
        if (Constants.FILES_PATH == null || !didCrashInLastSession()) {
            return null;
        }

        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".stacktrace");
            }
        };

        File lastModifiedFile = InternalStorage.lastModifiedFile(Constants.FILES_PATH, filter);
        CrashReport result = null;

        if (lastModifiedFile != null && lastModifiedFile.exists()) {
            try {
                result = CrashReport.fromFile(lastModifiedFile);
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        return result;
    }

    private void saveConfirmedStackTraces() {
        try {
            String[] filenames = searchForStackTraces();
            PreferencesStorage.putString("ConfirmedFilenames", Util.joinArray(filenames, "|"));
        } catch (Exception e) {
            // Just in case, we catch all exceptions here
        }
    }

    public void deleteStackTraces() {
        String[] list = searchForStackTraces();

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (int index = 0; index < list.length; index++) {
                try {
                    AvalancheLog.debug("Delete stacktrace " + list[index] + ".");
                    deleteStackTrace(list[index]);
                    InternalStorage.delete(Constants.FILES_PATH + "/" + list[index]);
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
        filename = Constants.FILES_PATH + "/" + filename;
        InternalStorage.delete(filename);

        String user = filename.replace(".stacktrace", ".user");
        InternalStorage.delete(user);

        String contact = filename.replace(".stacktrace", ".contact");
        InternalStorage.delete(contact);

        String description = filename.replace(".stacktrace", ".description");
        InternalStorage.delete(description);
    }

    private boolean isIgnoreDefaultHandler() {
        return mListener != null && mListener.ignoreDefaultHandler();
    }

    private String getURLString() {
        return mEndpointUrl + "api/2/apps/" + Avalanche.getSharedInstance().getAppIdentifier() + "/crashes/";
    }
}
