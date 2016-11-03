package com.microsoft.azure.mobile;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.channel.DefaultChannel;
import com.microsoft.azure.mobile.ingestion.models.WrapperSdk;
import com.microsoft.azure.mobile.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.IdHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.util.Log.ASSERT;
import static android.util.Log.VERBOSE;
import static com.microsoft.azure.mobile.utils.MobileCenterLog.NONE;

public final class MobileCenter {

    /**
     * TAG used in logging.
     */
    public static final String LOG_TAG = MobileCenterLog.LOG_TAG;

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static MobileCenter sInstance;

    /**
     * Remember if log level was configured using this class.
     */
    private boolean mLogLevelConfigured;

    /**
     * Custom server Url if any.
     */
    private String mServerUrl;

    /**
     * Application context.
     */
    private Application mApplication;

    /**
     * Configured features.
     */
    private Set<MobileCenterFeature> mFeatures;

    /**
     * Log serializer.
     */
    private LogSerializer mLogSerializer;

    /**
     * Channel.
     */
    private Channel mChannel;

    @VisibleForTesting
    static synchronized MobileCenter getInstance() {
        if (sInstance == null)
            sInstance = new MobileCenter();
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * A wrapper SDK can use this method to pass extra information to device properties.
     *
     * @param wrapperSdk wrapper SDK information.
     */
    @SuppressWarnings("WeakerAccess")
    public static void setWrapperSdk(WrapperSdk wrapperSdk) {
        getInstance().setInstanceWrapperSdk(wrapperSdk);
    }

    /**
     * Return log level filter for logs coming from this SDK.
     *
     * @return log level as defined by {@link android.util.Log}.
     */
    @IntRange(from = VERBOSE, to = ASSERT)
    public static int getLogLevel() {
        return MobileCenterLog.getLogLevel();
    }

    /**
     * Set a log level for logs coming from Mobile Center SDK.
     *
     * @param logLevel A log level as defined by {@link android.util.Log}.
     * @see android.util.Log#VERBOSE
     * @see android.util.Log#DEBUG
     * @see android.util.Log#INFO
     * @see android.util.Log#WARN
     * @see android.util.Log#ERROR
     * @see android.util.Log#ASSERT
     * @see MobileCenterLog#NONE
     */
    public static void setLogLevel(@IntRange(from = VERBOSE, to = NONE) int logLevel) {
        getInstance().setInstanceLogLevel(logLevel);
    }

    /**
     * Change the base URL (scheme + authority + port only) used to communicate with the backend.
     *
     * @param serverUrl base URL to use for server communication.
     */
    public static void setServerUrl(String serverUrl) {
        getInstance().setInstanceServerUrl(serverUrl);
    }

    /**
     * Check whether SDK has already been initialized.
     *
     * @return true if initialized, false otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isInitialized() {
        return getInstance().isInstanceInitialized();
    }

    /**
     * Initialize the SDK.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param appSecret   A unique and secret key used to identify the application.
     */
    @SuppressWarnings("SameParameterValue")
    public static void initialize(Application application, String appSecret) {
        getInstance().instanceInitialize(application, appSecret);
    }

    /**
     * Start features.
     * This may be called only once per feature per application process lifetime.
     *
     * @param features List of features to use.
     */
    @SafeVarargs
    public static void start(Class<? extends MobileCenterFeature>... features) {
        getInstance().startFeatures(features);
    }

    /**
     * Initialize the SDK with the list of features to start.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param appSecret   A unique and secret key used to identify the application.
     * @param features    List of features to use.
     */
    @SafeVarargs
    public static void start(Application application, String appSecret, Class<? extends MobileCenterFeature>... features) {
        getInstance().initializeAndStartFeatures(application, appSecret, features);
    }

    /**
     * Check whether the SDK is enabled or not as a whole.
     *
     * @return true if enabled, false otherwise.
     */
    public static boolean isEnabled() {
        return checkPrecondition("isEnabled") && getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable the SDK as a whole. In addition to the MobileCenter resources,
     * it will also enable or disable
     * all features registered via {@link #start(Application, String, Class[])}.
     *
     * @param enabled true to enable, false to disable.
     */
    public static void setEnabled(boolean enabled) {
        if (checkPrecondition("setEnabled"))
            getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Get a unique installation identifier.
     * The identifier is persisted until the application is uninstalled and installed again.
     *
     * @return A unique installation identifier.
     */
    public static UUID getInstallId() {
        if (checkPrecondition("getInstallId"))
            return IdHelper.getInstallId();
        return null;
    }

    /**
     * Check whether the SDK is ready for use or not.
     *
     * @param methodName A method name that is being called by a host application.
     * @return <code>true</code> if the SDK is ready, <code>false</code> otherwise.
     */
    private static boolean checkPrecondition(String methodName) {
        if (getInstance().isInstanceInitialized())
            return true;

        MobileCenterLog.error(LOG_TAG, "Mobile Center has not been initialized and is not ready for " + methodName);
        return false;
    }

    /**
     * {@link #setWrapperSdk(WrapperSdk)} implementation at instance level.
     *
     * @param wrapperSdk wrapper SDK information.
     */
    private synchronized void setInstanceWrapperSdk(WrapperSdk wrapperSdk) {
        DeviceInfoHelper.setWrapperSdk(wrapperSdk);
        if (mChannel != null)
            mChannel.invalidateDeviceCache();
    }

    /**
     * {@link #setLogLevel(int)} implementation at instance level.
     *
     * @param logLevel log level.
     */
    private synchronized void setInstanceLogLevel(int logLevel) {
        mLogLevelConfigured = true;
        MobileCenterLog.setLogLevel(logLevel);
    }

    /**
     * {@link #setServerUrl(String)} implementation at instance level.
     *
     * @param serverUrl server URL.
     */
    private synchronized void setInstanceServerUrl(String serverUrl) {
        mServerUrl = serverUrl;
        if (mChannel != null)
            mChannel.setServerUrl(serverUrl);
    }

    /**
     * {@link #isInitialized()} implementation at instance level.
     */
    private synchronized boolean isInstanceInitialized() {
        return mApplication != null;
    }

    /**
     * Internal SDK initialization.
     *
     * @param application application context.
     * @param appSecret   a unique and secret key used to identify the application.
     * @return true if init was successful, false otherwise.
     */
    private synchronized boolean instanceInitialize(Application application, String appSecret) {

        /* Load some global constants. */
        Constants.loadFromContext(application);

        /* Enable a default log level for debuggable applications. */
        if (!mLogLevelConfigured && Constants.APPLICATION_DEBUGGABLE) {
            MobileCenterLog.setLogLevel(Log.WARN);
        }

        /* Parse and store parameters. */
        if (mApplication != null) {
            MobileCenterLog.warn(LOG_TAG, "Mobile Center may only be initialized once");
            return false;
        }
        if (application == null) {
            MobileCenterLog.error(LOG_TAG, "application may not be null");
            return false;
        }
        if (appSecret == null) {
            MobileCenterLog.error(LOG_TAG, "appSecret may not be null");
            return false;
        }
        UUID appSecretUUID;
        try {
            appSecretUUID = UUID.fromString(appSecret);
        } catch (IllegalArgumentException e) {
            MobileCenterLog.error(LOG_TAG, "appSecret is invalid", e);
            return false;
        }
        mApplication = application;

        /* If parameters are valid, init context related resources. */
        StorageHelper.initialize(application);
        mFeatures = new HashSet<>();

        /* Init channel. */
        mLogSerializer = new DefaultLogSerializer();
        mChannel = new DefaultChannel(application, appSecretUUID, mLogSerializer);
        mChannel.setEnabled(isInstanceEnabled());
        if (mServerUrl != null)
            mChannel.setServerUrl(mServerUrl);
        return true;
    }

    @SafeVarargs
    private final synchronized void startFeatures(Class<? extends MobileCenterFeature>... features) {
        if (features == null) {
            MobileCenterLog.error(LOG_TAG, "Start feature array is null");
            return;
        }
        if (mApplication == null) {
            MobileCenterLog.error(LOG_TAG, "Cannot start features, Mobile Center has not been initialized");
            return;
        }
        for (Class<? extends MobileCenterFeature> feature : features) {
            if (feature == null) {
                MobileCenterLog.warn(LOG_TAG, "Skipping null feature, please check your varargs/array does not contain any null reference.");
            } else {
                try {
                    startFeature((MobileCenterFeature) feature.getMethod("getInstance").invoke(null));
                } catch (Exception e) {
                    MobileCenterLog.error(LOG_TAG, "Failed to get feature instance '" + feature.getName() + "', skipping it.", e);
                }
            }
        }
    }

    /**
     * Start a feature.
     *
     * @param feature feature to start.
     */
    private synchronized void startFeature(@NonNull MobileCenterFeature feature) {
        if (mFeatures.contains(feature)) {
            MobileCenterLog.warn(LOG_TAG, "Mobile Center has already started the feature with class name: " + feature.getClass().getName());
            return;
        }
        Map<String, LogFactory> logFactories = feature.getLogFactories();
        if (logFactories != null) {
            for (Map.Entry<String, LogFactory> logFactory : logFactories.entrySet())
                mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
        }
        mFeatures.add(feature);
        feature.onChannelReady(mApplication, mChannel);
        if (isInstanceEnabled())
            mApplication.registerActivityLifecycleCallbacks(feature);
    }

    @SafeVarargs
    private final synchronized void initializeAndStartFeatures(Application application, String appSecret, Class<? extends MobileCenterFeature>... features) {
        boolean initializedSuccessfully = instanceInitialize(application, appSecret);
        if (initializedSuccessfully) {
            startFeatures(features);
        }
    }

    /**
     * Implements {@link #isEnabled()}.
     */
    private synchronized boolean isInstanceEnabled() {
        return StorageHelper.PreferencesStorage.getBoolean(PrefStorageConstants.KEY_ENABLED, true);
    }

    /**
     * Implements {@link #setEnabled(boolean)}}.
     */
    private synchronized void setInstanceEnabled(boolean enabled) {

        /* Update channel state. */
        mChannel.setEnabled(enabled);

        /* Un-subscribe app callbacks if we were enabled and now disabled. */
        boolean previouslyEnabled = isInstanceEnabled();
        boolean switchToDisabled = previouslyEnabled && !enabled;
        boolean switchToEnabled = !previouslyEnabled && enabled;
        if (switchToDisabled) {
            MobileCenterLog.info(LOG_TAG, "Mobile Center disabled");
        } else if (switchToEnabled) {
            MobileCenterLog.info(LOG_TAG, "Mobile Center enabled");
        }

        /* Update state. */
        StorageHelper.PreferencesStorage.putBoolean(PrefStorageConstants.KEY_ENABLED, enabled);

        /* Apply change to features. */
        for (MobileCenterFeature feature : mFeatures) {

            /* Add or remove callbacks depending on state change. */
            if (switchToDisabled)
                mApplication.unregisterActivityLifecycleCallbacks(feature);
            else if (switchToEnabled)
                mApplication.registerActivityLifecycleCallbacks(feature);

            /* Forward status change. */
            if (feature.isInstanceEnabled() != enabled)
                feature.setInstanceEnabled(enabled);
        }
    }

    @VisibleForTesting
    Set<MobileCenterFeature> getFeatures() {
        return mFeatures;
    }

    @VisibleForTesting
    Application getApplication() {
        return mApplication;
    }

    @VisibleForTesting
    void setChannel(Channel channel) {
        mChannel = channel;
    }
}
