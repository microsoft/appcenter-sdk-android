package avalanche.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import avalanche.base.AvalancheHub;
import avalanche.base.Constants;
import avalanche.base.DefaultAvalancheFeature;



import avalanche.crash.model.CrashReport;
import avalanche.crash.model.CrashesUserInput;
import avalanche.crash.model.CrashMetaData;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.HttpURLConnectionBuilder;
import avalanche.base.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.isEmpty;


public class Crashes extends DefaultAvalancheFeature {

    private static final String ALWAYS_SEND_KEY = "always_send_crash_reports";

    private static final int STACK_TRACES_FOUND_NONE = 0;
    private static final int STACK_TRACES_FOUND_NEW = 1;
    private static final int STACK_TRACES_FOUND_CONFIRMED = 2;
    private static Crashes sharedInstance = null;
    private CrashesListener mListener;
    private String mEndpointUrl;
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

    private static String getAlertTitle(Context context) {
        String appTitle = Util.getAppName(context);

        String message = context.getString(R.string.avalanche_crash_dialog_title);
        return String.format(message, appTitle);
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
        return register(context, Constants.BASE_URL, listener);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * AvalancheHub app identifier is read from configuration values in manifest.
     *
     * @param context     The context to use. Usually your Activity object. If
     *                    context is not an instance of Activity (or a subclass of it),
     *                    crashes will be sent automatically.
     * @param endpointUrl URL of the AvalancheHub endpoint to use.
     * @param listener    Implement this for callback functions.
     * @return The configured crash manager for method chaining.
     */
    public Crashes register(Context context, String endpointUrl, CrashesListener listener) {
        return register(context, endpointUrl, listener, false);
    }

    /**
     * Registers the crash manager and handles existing crash logs.
     * AvalancheHub app identifier is read from configuration values in manifest.
     *
     * @param context       The context to use. Usually your Activity object. If
     *                      context is not an instance of Activity (or a subclass of it),
     *                      crashes will be sent automatically.
     * @param endpointUrl   URL of the AvalancheHub endpoint to use.
     * @param listener      Implement this for callback functions.
     * @param lazyExecution Whether the manager should execute lazily, e.g. not check for crashes right away.
     * @return
     */
    public Crashes register(Context context, String endpointUrl, CrashesListener listener, boolean lazyExecution) {
        mContextWeakReference = new WeakReference<>(context);
        mListener = listener;
        mEndpointUrl = endpointUrl;
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

        int foundOrSend = hasStackTraces(context);

        if (foundOrSend == STACK_TRACES_FOUND_NEW) {
            mDidCrashInLastSession = true;
            Boolean autoSend = !(context instanceof Activity);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            autoSend |= prefs.getBoolean(ALWAYS_SEND_KEY, false);

            if (mListener != null) {
                autoSend |= mListener.shouldAutoUploadCrashes();

                mListener.onNewCrashesFound();
            }

            if (!autoSend) {
                showDialog(mContextWeakReference);
            } else {
                sendCrashes();
            }
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
     * Submits all stack traces in the files dir to AvalancheHub.
     *
     * @param crashMetaData The crashMetaData, provided by the user.
     */
    public void submitStackTraces(CrashMetaData crashMetaData) {
        String[] list = searchForStackTraces();
        Boolean successful = false;

        Context context = mContextWeakReference.get();

        if ((list != null) && (list.length > 0)) {
            AvalancheLog.debug("Found " + list.length + " stacktrace(s).");

            for (int index = 0; index < list.length; index++) {
                HttpURLConnection urlConnection = null;
                try {
                    // Read contents of stack trace
                    String filename = list[index];
                    String stacktrace = Util.contentsOfFile(context, filename);
                    if (stacktrace.length() > 0) {
                        // Transmit stack trace with POST request

                        AvalancheLog.debug("Transmitting crash data: \n" + stacktrace);

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

                        Map<String, String> parameters = new HashMap<String, String>();

                        parameters.put("raw", stacktrace);
                        parameters.put("userID", userID);
                        parameters.put("contact", contact);
                        parameters.put("description", description);
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
                            deleteRetryCounter(list[index], mListener.getMaxRetryAttempts());
                        }
                    } else {
                        AvalancheLog.debug("Transmission failed, will retry on next register() call");
                        if (mListener != null) {
                            mListener.onCrashesNotSent();
                            updateRetryCounter(list[index], mListener.getMaxRetryAttempts());
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows a dialog to ask the user whether he wants to send crash reports to
     * AvalancheHub or delete them.
     */
    private void showDialog(final WeakReference<Context> weakContext) {
        Context context = null;
        if (weakContext != null) {
            context = weakContext.get();
        }

        if (context == null) {
            return;
        }

        if (mListener != null && mListener.onHandleAlertView()) {
            return;
        }

        final boolean ignoreDefaultHandler = isIgnoreDefaultHandler();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        String alertTitle = getAlertTitle(context);
        builder.setTitle(alertTitle);
        builder.setMessage(R.string.avalanche_crash_dialog_message);

        builder.setNegativeButton(R.string.avalanche_crash_dialog_negative_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputDontSend, null, mListener, weakContext, ignoreDefaultHandler);
            }
        });

        builder.setNeutralButton(R.string.avalanche_crash_dialog_neutral_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputAlwaysSend, null, mListener, weakContext, ignoreDefaultHandler);
            }
        });

        builder.setPositiveButton(R.string.avalanche_crash_dialog_positive_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                handleUserInput(CrashesUserInput.CrashManagerUserInputSend, null, mListener,
                        weakContext, ignoreDefaultHandler);
            }
        });

        builder.create().show();
    }

    /**
     * Provides an interface to pass user input from a custom alert to a crash report
     *
     * @param userInput            Defines the users action whether to send, always send, or not to send the crash report.
     * @param userProvidedMetaData The content of this optional CrashMetaData instance will be attached to the crash report
     *                             and allows to ask the user for e.g. additional comments or info.
     * @param listener             an optional crash manager listener to use.
     * @param weakContext          The context to use. Usually your Activity object.
     * @param ignoreDefaultHandler whether to ignore the default exception handler.
     * @return true if the input is a valid option and successfully triggered further processing of the crash report.
     * @see CrashesUserInput
     * @see CrashMetaData
     * @see CrashesListener
     */
    public boolean handleUserInput(final CrashesUserInput userInput,
                                   final CrashMetaData userProvidedMetaData, final CrashesListener listener,
                                   final WeakReference<Context> weakContext, final boolean ignoreDefaultHandler) {
        switch (userInput) {
            case CrashManagerUserInputDontSend:
                if (listener != null) {
                    listener.onUserDeniedCrashes();
                }

                deleteStackTraces();
                registerExceptionHandler();
                return true;
            case CrashManagerUserInputAlwaysSend:
                Context context = null;
                if (weakContext != null) {
                    context = weakContext.get();
                }

                if (context == null) {
                    return false;
                }

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                prefs.edit().putBoolean(ALWAYS_SEND_KEY, true).apply();

                sendCrashes(userProvidedMetaData);
                return true;
            case CrashManagerUserInputSend:
                sendCrashes(userProvidedMetaData);
                return true;
            default:
                return false;
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

    public CrashReport getLastCrashDetails() {
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

    public void deleteStackTraces() {
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

    /**
     * Update the retry attempts count for this crash stacktrace.
     */
    private void updateRetryCounter(String filename, int maxRetryAttempts) {
        if (maxRetryAttempts == -1) {
            return;
        }

        Context context = mContextWeakReference.get();
        SharedPreferences preferences = context.getSharedPreferences("AvalancheSDK", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        int retryCounter = preferences.getInt("RETRY_COUNT: " + filename, 0);
        if (retryCounter >= maxRetryAttempts) {
            deleteStackTrace(filename);
            deleteRetryCounter(filename, maxRetryAttempts);
        } else {
            editor.putInt("RETRY_COUNT: " + filename, retryCounter + 1);
            editor.apply();
        }
    }

    /**
     * Delete the retry counter if stacktrace is uploaded or retry limit is
     * reached.
     */
    private void deleteRetryCounter(String filename, int maxRetryAttempts) {
        Context context = mContextWeakReference.get();
        SharedPreferences preferences = context.getSharedPreferences("AvalancheSDK", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove("RETRY_COUNT: " + filename);
        editor.apply();
    }

    private boolean isIgnoreDefaultHandler() {
        return mListener != null && mListener.ignoreDefaultHandler();
    }

    private String getURLString() {
        return mEndpointUrl + "api/2/apps/" + AvalancheHub.getSharedInstance().getAppIdentifier() + "/crashes/";
    }
}
