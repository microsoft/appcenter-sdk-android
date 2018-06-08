package com.microsoft.appcenter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.channel.OneCollectorChannelListener;
import com.microsoft.appcenter.ingestion.models.CustomPropertiesLog;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.ingestion.models.json.CustomPropertiesLogFactory;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.json.StartServiceLogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.ShutdownHelper;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_COUNT;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
import static com.microsoft.appcenter.utils.AppCenterLog.NONE;

public class AppCenter {

    /**
     * TAG used in logging.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG;

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String CORE_GROUP = "group_core";

    /**
     * Name of the variable used to indicate services that should be disabled (typically for test
     * cloud).
     */
    @VisibleForTesting
    static final String DISABLE_SERVICES = "APP_CENTER_DISABLE";

    /**
     * Value to indicate that all services should be disabled.
     */
    @VisibleForTesting
    static final String DISABLE_ALL_SERVICES = "All";

    /**
     * Delimiter between two key value pairs.
     */
    @VisibleForTesting
    static final String PAIR_DELIMITER = ";";

    /**
     * Delimiter between key and its value.
     */
    @VisibleForTesting
    static final String KEY_VALUE_DELIMITER = "=";

    /**
     * Application secret key.
     */
    @VisibleForTesting
    static final String APP_SECRET_KEY = "appsecret";

    /**
     * Transmission target token key.
     */
    @VisibleForTesting
    static final String TRANSMISSION_TARGET_TOKEN_KEY = "target";

    /**
     * Shutdown timeout in millis.
     */
    private static final int SHUTDOWN_TIMEOUT = 5000;

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static AppCenter sInstance;

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

    /**
     * Transmission target token.
     */
    private String mTransmissionTargetToken;

    /**
     * Handler for uncaught exceptions.
     */
    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * Configured services.
     */
    private Set<AppCenterService> mServices;

    /**
     * Started services for which the log isn't sent yet.
     */
    private List<String> mStartedServicesNamesToLog;

    /**
     * Log serializer.
     */
    private LogSerializer mLogSerializer;

    /**
     * Channel.
     */
    private Channel mChannel;

    /**
     * Background handler thread.
     */
    private HandlerThread mHandlerThread;

    /**
     * Background thread handler.
     */
    private Handler mHandler;

    /**
     * Background thread handler abstraction to shared with services.
     */
    private AppCenterHandler mAppCenterHandler;

    @VisibleForTesting
    static synchronized AppCenter getInstance() {
        if (sInstance == null) {
            sInstance = new AppCenter();
        }
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
        return AppCenterLog.getLogLevel();
    }

    /**
     * Set a log level for logs coming from App Center SDK.
     *
     * @param logLevel A log level as defined by {@link android.util.Log}.
     * @see android.util.Log#VERBOSE
     * @see android.util.Log#DEBUG
     * @see android.util.Log#INFO
     * @see android.util.Log#WARN
     * @see android.util.Log#ERROR
     * @see android.util.Log#ASSERT
     * @see AppCenterLog#NONE
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

    /**
     * Get the current version of App Center SDK.
     *
     * @return The current version of App Center SDK.
     */
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    public static String getSdkVersion() {
        return com.microsoft.appcenter.BuildConfig.VERSION_NAME;
    }

    /**
     * Set the custom properties.
     *
     * @param customProperties custom properties object.
     */
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
    public static void start(Class<? extends AppCenterService>... services) {
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
    public static void start(Application application, String appSecret, Class<? extends AppCenterService>... services) {
        getInstance().configureAndStartServices(application, appSecret, services);
    }

    /**
     * Check whether the SDK is enabled or not as a whole.
     * This operation is performed in background as it accesses SharedPreferences.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable the SDK as a whole. In addition to the App Center resources,
     * it will also enable or disable
     * all services registered via {@link #start(Application, String, Class[])}.
     *
     * @param enabled true to enable, false to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Get a unique installation identifier.
     * The identifier is persisted until the application is uninstalled and installed again.
     * This operation is performed in background as it accesses SharedPreferences and UUID.
     *
     * @return future with result being the installation identifier.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<UUID> getInstallId() {
        return getInstance().getInstanceInstallId();
    }

    /**
     * Check whether the SDK is ready for use or not.
     *
     * @return <code>true</code> if the SDK is ready, <code>false</code> otherwise.
     */
    private synchronized boolean checkPrecondition() {
        if (isInstanceConfigured()) {
            return true;
        }
        AppCenterLog.error(LOG_TAG, "App Center hasn't been configured. You need to call AppCenter.start with appSecret or AppCenter.configure first.");
        return false;
    }

    /**
     * {@link #setWrapperSdk(WrapperSdk)} implementation at instance level.
     *
     * @param wrapperSdk wrapper SDK information.
     */
    private synchronized void setInstanceWrapperSdk(WrapperSdk wrapperSdk) {
        DeviceInfoHelper.setWrapperSdk(wrapperSdk);

        /* If SDK already configured, reset device info cache. */
        if (mHandler != null) {

            /* Every channel operation must be in background since it uses locks and accesses disks. */
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mChannel.invalidateDeviceCache();
                }
            });
        }
    }

    /**
     * {@link #setLogLevel(int)} implementation at instance level.
     *
     * @param logLevel log level.
     */
    private synchronized void setInstanceLogLevel(int logLevel) {
        mLogLevelConfigured = true;
        AppCenterLog.setLogLevel(logLevel);
    }

    /**
     * {@link #setLogUrl(String)} implementation at instance level.
     *
     * @param logUrl log URL.
     */
    private synchronized void setInstanceLogUrl(final String logUrl) {
        mLogUrl = logUrl;

        /* If SDK already configured, set log url. */
        if (mHandler != null) {

            /* Every channel operation must be in background since it uses locks and accesses disks. */
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mChannel.setLogUrl(logUrl);
                }
            });
        }
    }

    /**
     * {@link #setCustomProperties(CustomProperties)} implementation at instance level.
     *
     * @param customProperties custom properties object.
     */
    private synchronized void setInstanceCustomProperties(CustomProperties customProperties) {
        if (customProperties == null) {
            AppCenterLog.error(LOG_TAG, "Custom properties may not be null.");
            return;
        }
        final Map<String, Object> properties = customProperties.getProperties();
        if (properties.size() == 0) {
            AppCenterLog.error(LOG_TAG, "Custom properties may not be empty.");
            return;
        }
        handlerAppCenterOperation(new Runnable() {

            @Override
            public void run() {
                queueCustomProperties(properties);
            }
        }, null);
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
     *                    It can be null since a transmission target token can be set later.
     * @return true if configuration was successful, false otherwise.
     */
    /* UncaughtExceptionHandler is used by PowerMock but lint does not detect it. */
    @SuppressLint("VisibleForTests")
    private synchronized boolean instanceConfigure(Application application, String appSecret) {

        /* Check parameters. */
        if (application == null) {
            AppCenterLog.error(LOG_TAG, "application may not be null");
            return false;
        }

        /* Ignore call if already configured. */
        if (mHandler != null) {
            AppCenterLog.warn(LOG_TAG, "App Center may only be configured once.");
            return false;
        }

        /* Enable a default log level for debuggable applications. */
        if (!mLogLevelConfigured && (application.getApplicationInfo().flags & FLAG_DEBUGGABLE) == FLAG_DEBUGGABLE) {
            AppCenterLog.setLogLevel(Log.WARN);
        }

        /* Store state. */
        mApplication = application;

        /* A null secret is still valid since transmission target token can be set later. */
        if (appSecret != null && !appSecret.isEmpty()) {

            /* Init parsing, the app secret string can contain other secrets.  */
            String[] pairs = appSecret.split(PAIR_DELIMITER);

            /* Split by pairs. */
            for (String pair : pairs) {

                /* Split key and value. */
                String[] keyValue = pair.split(KEY_VALUE_DELIMITER, -1);
                String key = keyValue[0];

                /* A value with no key is default to the app secret. */
                if (keyValue.length == 1) {
                    if (!key.isEmpty()) {
                        mAppSecret = key;
                    }
                } else if (!keyValue[1].isEmpty()) {
                    String value = keyValue[1];

                    /* Ignore unknown keys. */
                    if (APP_SECRET_KEY.equals(key)) {
                        mAppSecret = value;
                    } else if (TRANSMISSION_TARGET_TOKEN_KEY.equals(key)) {
                        mTransmissionTargetToken = value;
                    }
                }
            }
        }

        /* Start looper. */
        mHandlerThread = new HandlerThread("AppCenter.Looper");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper());
        mAppCenterHandler = new AppCenterHandler() {

            @Override
            public void post(@NonNull Runnable runnable, Runnable disabledRunnable) {
                handlerAppCenterOperation(runnable, disabledRunnable);
            }
        };

        /* The rest of initialization is done in background as we need storage. */
        mServices = new HashSet<>();
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                finishConfiguration();
            }
        });
        AppCenterLog.info(LOG_TAG, "App Center SDK configured successfully.");
        return true;
    }

    private synchronized void handlerAppCenterOperation(final Runnable runnable, final Runnable disabledRunnable) {
        if (checkPrecondition()) {
            Runnable wrapperRunnable = new Runnable() {

                @Override
                public void run() {
                    if (isInstanceEnabled()) {
                        runnable.run();
                    } else {
                        if (disabledRunnable != null) {
                            disabledRunnable.run();
                        } else {
                            AppCenterLog.error(LOG_TAG, "App Center SDK is disabled.");
                        }
                    }
                }
            };

            /*
             * We can already be in the background thread in case of callbacks,
             * run now to avoid dead locks with getters.
             */
            if (Thread.currentThread() == mHandlerThread) {
                runnable.run();
            } else {
                mHandler.post(wrapperRunnable);
            }
        }
    }

    @WorkerThread
    private void finishConfiguration() {

        /* Load some global constants. */
        Constants.loadFromContext(mApplication);

        /* If parameters are valid, init context related resources. */
        StorageHelper.initialize(mApplication);

        /* Initialize session storage. */
        SessionContext.getInstance();

        /* Get enabled state. */
        boolean enabled = isInstanceEnabled();

        /* Init uncaught exception handler. */
        mUncaughtExceptionHandler = new UncaughtExceptionHandler();
        if (enabled) {
            mUncaughtExceptionHandler.register();
        }

        /* Init channel. */
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        mLogSerializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        mChannel = new DefaultChannel(mApplication, mAppSecret, mLogSerializer, mHandler);
        mChannel.setEnabled(enabled);
        mChannel.addGroup(CORE_GROUP, DEFAULT_TRIGGER_COUNT, DEFAULT_TRIGGER_INTERVAL, DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS, null, null);
        if (mLogUrl != null) {
            mChannel.setLogUrl(mLogUrl);
        }
        mChannel.addListener(new OneCollectorChannelListener(mApplication, mChannel, mLogSerializer, IdHelper.getInstallId()));
        if (!enabled) {
            NetworkStateHelper.getSharedInstance(mApplication).close();
        }
        AppCenterLog.debug(LOG_TAG, "App Center storage initialized.");
    }

    @SafeVarargs
    private final synchronized void startServices(Class<? extends AppCenterService>... services) {
        if (services == null) {
            AppCenterLog.error(LOG_TAG, "Cannot start services, services array is null. Failed to start services.");
            return;
        }
        if (mApplication == null) {
            StringBuilder serviceNames = new StringBuilder();
            for (Class<? extends AppCenterService> service : services) {
                serviceNames.append("\t").append(service.getName()).append("\n");
            }
            AppCenterLog.error(LOG_TAG, "Cannot start services, App Center has not been configured. Failed to start the following services:\n" + serviceNames);
            return;
        }

        /* Start each service and collect info for send start service log. */
        final Collection<AppCenterService> startedServices = new ArrayList<>();
        for (Class<? extends AppCenterService> service : services) {
            if (service == null) {
                AppCenterLog.warn(LOG_TAG, "Skipping null service, please check your varargs/array does not contain any null reference.");
            } else {
                try {
                    AppCenterService serviceInstance = (AppCenterService) service.getMethod("getInstance").invoke(null);
                    if (mServices.contains(serviceInstance)) {
                        AppCenterLog.warn(LOG_TAG, "App Center has already started the service with class name: " + service.getName());
                    } else if (shouldDisable(serviceInstance.getServiceName())) {
                        AppCenterLog.debug(LOG_TAG, "Instrumentation variable to disable service has been set; not starting service " + service.getName() + ".");
                    } else if (mAppSecret == null && serviceInstance.isAppSecretRequired()) {
                        AppCenterLog.warn(LOG_TAG, "App Center was started without app secret, but the service requires it; not starting service " + service.getName() + ".");
                    } else {

                        /* Share handler now with service while starting. */
                        serviceInstance.onStarting(mAppCenterHandler);
                        mApplication.registerActivityLifecycleCallbacks(serviceInstance);
                        mServices.add(serviceInstance);
                        startedServices.add(serviceInstance);
                    }
                } catch (Exception e) {
                    AppCenterLog.error(LOG_TAG, "Failed to get service instance '" + service.getName() + "', skipping it.", e);
                }
            }
        }

        /* Finish starting in background. */
        if (startedServices.size() > 0) {

            /* Post to ensure service started after storage initialized. */
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    finishStartServices(startedServices);
                }
            });
        }
    }

    @WorkerThread
    private void finishStartServices(Iterable<AppCenterService> services) {
        boolean enabled = isInstanceEnabled();
        List<String> serviceNames = new ArrayList<>();
        for (AppCenterService service : services) {
            Map<String, LogFactory> logFactories = service.getLogFactories();
            if (logFactories != null) {
                for (Map.Entry<String, LogFactory> logFactory : logFactories.entrySet()) {
                    mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
                }
            }
            if (!enabled && service.isInstanceEnabled()) {
                service.setInstanceEnabled(false);
            }
            service.onStarted(mApplication, mAppSecret, mTransmissionTargetToken, mChannel);
            AppCenterLog.info(LOG_TAG, service.getClass().getSimpleName() + " service started.");
            serviceNames.add(service.getServiceName());
        }
        sendStartServiceLog(serviceNames);
    }

    /**
     * Queue start service log.
     *
     * @param serviceNames the services to send.
     */
    @WorkerThread
    private void sendStartServiceLog(List<String> serviceNames) {
        if (isInstanceEnabled()) {
            StartServiceLog startServiceLog = new StartServiceLog();
            startServiceLog.setServices(serviceNames);
            mChannel.enqueue(startServiceLog, CORE_GROUP);
        } else {
            if (mStartedServicesNamesToLog == null) {
                mStartedServicesNamesToLog = new ArrayList<>();
            }
            mStartedServicesNamesToLog.addAll(serviceNames);
        }
    }

    @SafeVarargs
    private final synchronized void configureAndStartServices(Application application, String appSecret, Class<? extends AppCenterService>... services) {
        boolean configuredSuccessfully = instanceConfigure(application, appSecret);
        if (configuredSuccessfully) {
            startServices(services);
        }
    }

    /**
     * Send custom properties.
     * Unit test requires top level methods when PowerMock.whenNew.
     *
     * @param properties properties to send.
     */
    @WorkerThread
    private void queueCustomProperties(@NonNull Map<String, Object> properties) {
        CustomPropertiesLog customPropertiesLog = new CustomPropertiesLog();
        customPropertiesLog.setProperties(properties);
        mChannel.enqueue(customPropertiesLog, CORE_GROUP);
    }

    /**
     * Implements {@link #isEnabled()} at instance level.
     */
    private synchronized AppCenterFuture<Boolean> isInstanceEnabledAsync() {
        final DefaultAppCenterFuture<Boolean> future = new DefaultAppCenterFuture<>();
        if (checkPrecondition()) {
            mAppCenterHandler.post(new Runnable() {

                @Override
                public void run() {
                    future.complete(true);
                }
            }, new Runnable() {

                @Override
                public void run() {
                    future.complete(false);
                }
            });
        } else {
            future.complete(false);
        }
        return future;
    }

    /**
     * This can be called only after storage has been initialized in background.
     * However after that it can be used from U.I. thread without breaking strict mode.
     */
    private boolean isInstanceEnabled() {
        return StorageHelper.PreferencesStorage.getBoolean(PrefStorageConstants.KEY_ENABLED, true);
    }

    /**
     * Implements {@link #setInstanceEnabledAsync(boolean)}} after it's posted in background loop.
     */
    @WorkerThread
    private void setInstanceEnabled(boolean enabled) {

        /* Update channel state. */
        mChannel.setEnabled(enabled);

        /* Un-subscribe app callbacks if we were enabled and now disabled. */
        boolean previouslyEnabled = isInstanceEnabled();
        boolean switchToDisabled = previouslyEnabled && !enabled;
        boolean switchToEnabled = !previouslyEnabled && enabled;

        /* Update uncaught exception subscription. */
        if (switchToEnabled) {
            mUncaughtExceptionHandler.register();
            NetworkStateHelper.getSharedInstance(mApplication).reopen();
        } else if (switchToDisabled) {
            mUncaughtExceptionHandler.unregister();
            NetworkStateHelper.getSharedInstance(mApplication).close();
        }

        /* Update state now if true, services are checking this. */
        if (enabled) {
            StorageHelper.PreferencesStorage.putBoolean(PrefStorageConstants.KEY_ENABLED, true);
        }

        /* Send started services. */
        if (mStartedServicesNamesToLog != null && switchToEnabled) {
            sendStartServiceLog(mStartedServicesNamesToLog);
            mStartedServicesNamesToLog = null;
        }

        /* Apply change to services. */
        for (AppCenterService service : mServices) {

            /* Forward status change. */
            if (service.isInstanceEnabled() != enabled) {
                service.setInstanceEnabled(enabled);
            }
        }

        /* Update state now if false, services are checking if enabled while disabling. */
        if (!enabled) {
            StorageHelper.PreferencesStorage.putBoolean(PrefStorageConstants.KEY_ENABLED, false);
        }

        /* Log current state. */
        if (switchToDisabled) {
            AppCenterLog.info(LOG_TAG, "App Center has been disabled.");
        } else if (switchToEnabled) {
            AppCenterLog.info(LOG_TAG, "App Center has been enabled.");
        } else {
            AppCenterLog.info(LOG_TAG, "App Center has already been " + (enabled ? "enabled" : "disabled") + ".");
        }
    }

    /**
     * Implements {@link #setEnabled(boolean)}}.
     */
    private synchronized AppCenterFuture<Void> setInstanceEnabledAsync(final boolean enabled) {
        final DefaultAppCenterFuture<Void> future = new DefaultAppCenterFuture<>();
        if (checkPrecondition()) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    setInstanceEnabled(enabled);
                    future.complete(null);
                }
            });
        } else {
            future.complete(null);
        }
        return future;
    }

    /**
     * Implements {@link #getInstallId()}.
     */
    private synchronized AppCenterFuture<UUID> getInstanceInstallId() {
        final DefaultAppCenterFuture<UUID> future = new DefaultAppCenterFuture<>();
        if (checkPrecondition()) {
            mAppCenterHandler.post(new Runnable() {

                @Override
                public void run() {
                    future.complete(IdHelper.getInstallId());
                }
            }, new Runnable() {

                @Override
                public void run() {
                    future.complete(null);
                }
            });
        } else {
            future.complete(null);
        }
        return future;
    }

    @VisibleForTesting
    Set<AppCenterService> getServices() {
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

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    void setChannel(Channel channel) {
        mChannel = channel;
    }

    private Boolean shouldDisable(String serviceName) {
        try {
            Bundle arguments = InstrumentationRegistryHelper.getArguments();
            String disableServices = arguments.getString(DISABLE_SERVICES);
            if (disableServices == null) {
                return false;
            }
            String[] disableServicesList = disableServices.split(",");
            for (String service : disableServicesList) {
                service = service.trim();
                if (service.equals(DISABLE_ALL_SERVICES) || service.equals(serviceName)) {
                    return true;
                }
            }
            return false;
        } catch (LinkageError | IllegalStateException e) {
            AppCenterLog.debug(LOG_TAG, "Cannot read instrumentation variables in a non-test environment.");
            return false;
        }
    }

    @VisibleForTesting
    class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;

        @Override
        public void uncaughtException(Thread thread, Throwable exception) {
            if (isInstanceEnabled()) {

                /* Wait channel to finish saving other logs in background. */
                final Semaphore semaphore = new Semaphore(0);
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        if (mChannel != null) {
                            mChannel.shutdown();
                        }
                        AppCenterLog.debug(LOG_TAG, "Channel completed shutdown.");
                        semaphore.release();
                    }
                });
                try {
                    if (!semaphore.tryAcquire(SHUTDOWN_TIMEOUT, TimeUnit.MILLISECONDS)) {
                        AppCenterLog.error(LOG_TAG, "Timeout waiting for looper tasks to complete.");
                    }
                } catch (InterruptedException e) {
                    AppCenterLog.warn(LOG_TAG, "Interrupted while waiting looper to flush.", e);
                }
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
