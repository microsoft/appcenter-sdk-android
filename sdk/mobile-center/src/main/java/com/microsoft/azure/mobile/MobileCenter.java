package com.microsoft.azure.mobile;

import android.annotation.SuppressLint;
import android.app.Application;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.channel.DefaultChannel;
import com.microsoft.azure.mobile.ingestion.models.CustomPropertiesLog;
import com.microsoft.azure.mobile.ingestion.models.StartServiceLog;
import com.microsoft.azure.mobile.ingestion.models.WrapperSdk;
import com.microsoft.azure.mobile.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.StartServiceLogFactory;
import com.microsoft.azure.mobile.utils.DeviceInfoHelper;
import com.microsoft.azure.mobile.utils.IdHelper;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.PrefStorageConstants;
import com.microsoft.azure.mobile.utils.ShutdownHelper;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.util.Log.VERBOSE;
import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_COUNT;
import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_INTERVAL;
import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
import static com.microsoft.azure.mobile.utils.MobileCenterLog.NONE;

public class MobileCenter {

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String CORE_GROUP = "group_core";

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
     * Custom log URL if any.
     */
    private String mLogUrl;

    /**
     * Application context.
     */
    private Application mApplication;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /*
     * Handler for uncaught exceptions.
     */
    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * Configured services.
     */
    private Set<MobileCenterService> mServices;

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
    @IntRange(from = VERBOSE, to = NONE)
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
     * Change the base URL (scheme + authority + port only) used to send logs.
     *
     * @param logUrl base log URL.
     */
    public static void setLogUrl(String logUrl) {
        getInstance().setInstanceLogUrl(logUrl);
    }


    public static void setCustomProperties(CustomProperties customProperties) {
        getInstance().setInstanceCustomProperties(customProperties);
    }

    /**
     * Check whether SDK has already been configured.
     *
     * @return true if configured, false otherwise.
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isConfigured() {
        return getInstance().isInstanceConfigured();
    }

    /**
     * Configure the SDK.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param appSecret   A unique and secret key used to identify the application.
     */
    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static void configure(Application application, String appSecret) {
        getInstance().instanceConfigure(application, appSecret);
    }

    /**
     * Start services.
     * This may be called only once per service per application process lifetime.
     *
     * @param services List of services to use.
     */
    @SafeVarargs
    public static void start(Class<? extends MobileCenterService>... services) {
        getInstance().startServices(services);
    }

    /**
     * Configure the SDK with the list of services to start.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param appSecret   A unique and secret key used to identify the application.
     * @param services    List of services to use.
     */
    @SafeVarargs
    public static void start(Application application, String appSecret, Class<? extends MobileCenterService>... services) {
        getInstance().configureAndStartServices(application, appSecret, services);
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
     * all services registered via {@link #start(Application, String, Class[])}.
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
        if (getInstance().isInstanceConfigured())
            return true;

        MobileCenterLog.error(LOG_TAG, "Mobile Center has not been configured and is not ready for " + methodName);
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
     * {@link #setLogUrl(String)} implementation at instance level.
     *
     * @param logUrl log URL.
     */
    private synchronized void setInstanceLogUrl(String logUrl) {
        mLogUrl = logUrl;
        if (mChannel != null)
            mChannel.setLogUrl(logUrl);
    }

    private synchronized void setInstanceCustomProperties(CustomProperties customProperties) {
        if (customProperties == null)
            return;
        Map<String, Object> properties = customProperties.getProperties();
        if (properties.size() == 0) {
            MobileCenterLog.error(LOG_TAG, "Properties cannot be empty");
            return;
        }
        queueCustomProperties(properties);
    }

     /**
     * {@link #isConfigured()} implementation at instance level.
     */
    private synchronized boolean isInstanceConfigured() {
        return mApplication != null;
    }

    /**
     * Internal SDK configuration.
     *
     * @param application application context.
     * @param appSecret   a unique and secret key used to identify the application.
     * @return true if configuration was successful, false otherwise.
     */
    /* UncaughtExceptionHandler is used by PowerMock but lint does not detect it. */
    @SuppressLint("VisibleForTests")
    private synchronized boolean instanceConfigure(Application application, String appSecret) {

        /* Load some global constants. */
        Constants.loadFromContext(application);

        /* Enable a default log level for debuggable applications. */
        if (!mLogLevelConfigured && Constants.APPLICATION_DEBUGGABLE) {
            MobileCenterLog.setLogLevel(Log.WARN);
        }

        /* Parse and store parameters. */
        if (mApplication != null) {
            MobileCenterLog.warn(LOG_TAG, "Mobile Center may only be configured once");
            return false;
        } else if (application == null) {
            MobileCenterLog.error(LOG_TAG, "application may not be null");
        } else if (appSecret == null || appSecret.isEmpty()) {
            MobileCenterLog.error(LOG_TAG, "appSecret may not be null or empty");
        } else {

            /* Store state. */
            mApplication = application;
            mAppSecret = appSecret;

            /* If parameters are valid, init context related resources. */
            StorageHelper.initialize(application);

            /* Remember state to avoid double call PreferencesStorage. */
            boolean enabled = isInstanceEnabled();

            /* Init uncaught exception handler. */
            mUncaughtExceptionHandler = new UncaughtExceptionHandler();
            if (enabled)
                mUncaughtExceptionHandler.register();
            mServices = new HashSet<>();

            /* Init channel. */
            mLogSerializer = new DefaultLogSerializer();
            mLogSerializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
            mChannel = new DefaultChannel(application, appSecret, mLogSerializer);
            mChannel.setEnabled(enabled);
            mChannel.addGroup(CORE_GROUP, DEFAULT_TRIGGER_COUNT, DEFAULT_TRIGGER_INTERVAL, DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS, null);
            if (mLogUrl != null)
                mChannel.setLogUrl(mLogUrl);
            MobileCenterLog.logAssert(LOG_TAG, "Mobile Center SDK configured successfully.");
            return true;
        }

        MobileCenterLog.logAssert(LOG_TAG, "Mobile Center SDK configuration failed.");
        return false;
    }

    @SafeVarargs
    private final synchronized void startServices(Class<? extends MobileCenterService>... services) {
        if (services == null) {
            MobileCenterLog.error(LOG_TAG, "Cannot start services, services array is null. Failed to start services.");
            return;
        }
        if (mApplication == null) {
            String serviceNames = "";
            for (Class<? extends MobileCenterService> service : services) {
                serviceNames += "\t" + service.getName() + "\n";
            }
            MobileCenterLog.error(LOG_TAG, "Cannot start services, Mobile Center has not been configured. Failed to start the following services:\n" + serviceNames);
            return;
        }

        /* Start each service and collect info for send start service log. */
        List<String> startedServices = new ArrayList<>();
        for (Class<? extends MobileCenterService> service : services) {
            if (service == null) {
                MobileCenterLog.warn(LOG_TAG, "Skipping null service, please check your varargs/array does not contain any null reference.");
            } else {
                try {
                    MobileCenterService serviceInstance = (MobileCenterService) service.getMethod("getInstance").invoke(null);
                    if (startService(serviceInstance)) {
                        startedServices.add(serviceInstance.getServiceName());
                    }
                } catch (Exception e) {
                    MobileCenterLog.error(LOG_TAG, "Failed to get service instance '" + service.getName() + "', skipping it.", e);
                }
            }
        }
        if (startedServices.size() > 0)
            queueStartService(startedServices);
    }

    /**
     * Start a service.
     *
     * @param service service to start.
     */
    private synchronized boolean startService(@NonNull MobileCenterService service) {
        if (mServices.contains(service)) {
            MobileCenterLog.warn(LOG_TAG, "Mobile Center has already started the service with class name: " + service.getClass().getName());
            return false;
        }
        Map<String, LogFactory> logFactories = service.getLogFactories();
        if (logFactories != null) {
            for (Map.Entry<String, LogFactory> logFactory : logFactories.entrySet())
                mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
        }
        mServices.add(service);
        service.onStarted(mApplication, mAppSecret, mChannel);
        if (isInstanceEnabled())
            mApplication.registerActivityLifecycleCallbacks(service);
        MobileCenterLog.info(LOG_TAG, service.getClass().getSimpleName() + " service started.");
        return true;
    }

    @SafeVarargs
    private final synchronized void configureAndStartServices(Application application, String appSecret, Class<? extends MobileCenterService>... services) {
        boolean configuredSuccessfully = instanceConfigure(application, appSecret);
        if (configuredSuccessfully)
            startServices(services);
    }

    /**
     * Send started services.
     *
     * @param services started services.
     */
    private synchronized void queueStartService(@NonNull List<String> services) {
        StartServiceLog startServiceLog = new StartServiceLog();
        startServiceLog.setServices(services);
        mChannel.enqueue(startServiceLog, CORE_GROUP);
    }

    private synchronized void queueCustomProperties(@NonNull Map<String, Object> properties) {
        CustomPropertiesLog customPropertiesLog = new CustomPropertiesLog();
        customPropertiesLog.setProperties(properties);
        mChannel.enqueue(customPropertiesLog, CORE_GROUP);
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

        /* Update uncaught exception subscription. */
        if (switchToEnabled) {
            mUncaughtExceptionHandler.register();
        } else if (switchToDisabled) {
            mUncaughtExceptionHandler.unregister();
        }

        /* Update state. */
        StorageHelper.PreferencesStorage.putBoolean(PrefStorageConstants.KEY_ENABLED, enabled);

        /* Apply change to services. */
        for (MobileCenterService service : mServices) {

            /* Add or remove callbacks depending on state change. */
            if (switchToDisabled)
                mApplication.unregisterActivityLifecycleCallbacks(service);
            else if (switchToEnabled)
                mApplication.registerActivityLifecycleCallbacks(service);

            /* Forward status change. */
            if (service.isInstanceEnabled() != enabled)
                service.setInstanceEnabled(enabled);
        }

        /* Log current state. */
        if (switchToDisabled) {
            MobileCenterLog.info(LOG_TAG, "Mobile Center has been disabled.");
        } else if (switchToEnabled) {
            MobileCenterLog.info(LOG_TAG, "Mobile Center has been enabled.");
        } else {
            MobileCenterLog.info(LOG_TAG, "Mobile Center has already been " + (enabled ? "enabled" : "disabled") + ".");
        }
    }

    @VisibleForTesting
    Set<MobileCenterService> getServices() {
        return mServices;
    }

    @VisibleForTesting
    Application getApplication() {
        return mApplication;
    }

    @VisibleForTesting
    UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return mUncaughtExceptionHandler;
    }

    @VisibleForTesting
    void setChannel(Channel channel) {
        mChannel = channel;
    }

    @VisibleForTesting
    class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            if (isEnabled()) {

                /* Wait channel to finish saving other logs in background. */
                if (mChannel != null)
                    mChannel.shutdown();
            }
            if (mDefaultUncaughtExceptionHandler != null) {
                mDefaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            } else {
                ShutdownHelper.shutdown(10);
            }
        }

        @VisibleForTesting
        Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
            return mDefaultUncaughtExceptionHandler;
        }

        void register() {
            mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

        void unregister() {
            Thread.setDefaultUncaughtExceptionHandler(mDefaultUncaughtExceptionHandler);
        }
    }
}
