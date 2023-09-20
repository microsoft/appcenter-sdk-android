/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import android.util.Log;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.channel.DefaultChannel;
import com.microsoft.appcenter.channel.OneCollectorChannelListener;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.ingestion.models.WrapperSdk;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.ingestion.models.json.StartServiceLogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.ApplicationLifecycleListener;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.IdHelper;
import com.microsoft.appcenter.utils.InstrumentationRegistryHelper;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.PrefStorageConstants;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.SessionContext;
import com.microsoft.appcenter.utils.context.UserIdContext;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static android.util.Log.VERBOSE;
import static com.microsoft.appcenter.ApplicationContextUtils.getApplicationContext;
import static com.microsoft.appcenter.ApplicationContextUtils.isDeviceProtectedStorage;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_COUNT;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
import static com.microsoft.appcenter.http.HttpUtils.createHttpClient;
import static com.microsoft.appcenter.utils.AppCenterLog.NONE;

public class AppCenter {

    /**
     * TAG used in logging.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG;

    /**
     * Default maximum storage size for SQLite database.
     */
    @VisibleForTesting
    static final long DEFAULT_MAX_STORAGE_SIZE_IN_BYTES = 10 * 1024 * 1024;

    /**
     * Minimum size allowed for set maximum size (SQL limitation).
     */
    @VisibleForTesting
    static final long MINIMUM_STORAGE_SIZE = 24 * 1024;

    /**
     * Group for sending logs.
     */
    @VisibleForTesting
    static final String CORE_GROUP = "group_core";

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
     * Environment variable name for test to see if we're running in App Center Test.
     */
    @VisibleForTesting
    static final String RUNNING_IN_APP_CENTER = "RUNNING_IN_APP_CENTER";

    /**
     * A string value for environment variables denoting `true`.
     */
    private static final String TRUE_ENVIRONMENT_STRING = "1";

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
     * Android application object that was passed during configuration.
     * It shouldn't be used directly, but only for getting right context.
     * It's null until SDK is configured.
     */
    private Application mApplication;

    /**
     * Application context. Might be special context with device-protected storage.
     * See {@link ApplicationContextUtils#getApplicationContext(Application)}.
     */
    private Context mContext;

    /**
     * Application lifecycle listener.
     */
    private ApplicationLifecycleListener mApplicationLifecycleListener;

    /**
     * Application secret.
     */
    private String mAppSecret;

    /**
     * Transmission target token.
     */
    private String mTransmissionTargetToken;

    /**
     * Flag to now App Center configured at app level.
     * If configuring with neither app secret or transmission target, then we need this flag
     * to know App Center already configured/started at application level.
     */
    private boolean mConfiguredFromApp;

    /**
     * Handler for uncaught exceptions.
     */
    private UncaughtExceptionHandler mUncaughtExceptionHandler;

    /**
     * Started services for which the log isn't sent yet.
     */
    private final List<String> mStartedServicesNamesToLog = new ArrayList<>();

    /**
     * All started services.
     */
    private Set<AppCenterService> mServices;

    /**
     * All started services from library without an app secret.
     */
    private Set<AppCenterService> mServicesStartedFromLibrary;

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

    /**
     * Max storage size in bytes.
     */
    private long mMaxStorageSizeInBytes = DEFAULT_MAX_STORAGE_SIZE_IN_BYTES;

    /**
     * AppCenterFuture of set maximum storage size.
     */
    private DefaultAppCenterFuture<Boolean> mSetMaxStorageSizeFuture;

    /**
     * Redirect selected traffic to One Collector.
     */
    private OneCollectorChannelListener mOneCollectorChannelListener;

    /**
     * Contains a value about allowing/disallowing network requests. Null by default if the value was not set before the start of App Center.
     */
    private Boolean mAllowedNetworkRequests;

    /**
     * Country code or any other string to identify residency region..
     */
    private @Nullable String mDataResidencyRegion;

    /**
     * Get unique instance.
     *
     * @return unique instance.
     */
    public static synchronized AppCenter getInstance() {
        if (sInstance == null) {
            sInstance = new AppCenter();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
        NetworkStateHelper.unsetInstance();
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
     * Set the two-letter ISO country code.
     *
     * @param countryCode the two-letter ISO country code. See <code>https://www.iso.org/obp/ui/#search</code> for more information.
     */
    public static void setCountryCode(String countryCode) {
        DeviceInfoHelper.setCountryCode(countryCode);
    }

    /**
     * Set the country code or any other string to identify residency region.
     *
     * @param dataResidencyRegion residency region code.
     */
    public static void setDataResidencyRegion(@Nullable String dataResidencyRegion) {
        getInstance().mDataResidencyRegion = dataResidencyRegion;
    }

    /**
     * Set the country code or any other string to identify residency region.
     *
     * @return dataResidencyRegion residency region code if defined.
     */
    @Nullable
    public static String getDataResidencyRegion() {
        return getInstance().mDataResidencyRegion;
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
     * Check whether SDK has already been configured.
     *
     * @return true if configured, false otherwise.
     */
    public static boolean isConfigured() {
        return getInstance().isInstanceConfigured();
    }

    /**
     * Check whether app is running in App Center Test.
     *
     * @return true if running in App Center Test, false otherwise
     * (and where no test dependencies in release).
     */
    public static boolean isRunningInAppCenterTestCloud() {
        try {
            Bundle arguments = InstrumentationRegistryHelper.getArguments();
            String runningValue = arguments.getString(RUNNING_IN_APP_CENTER);
            return TRUE_ENVIRONMENT_STRING.equals(runningValue);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Configure the SDK with an app secret.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param appSecret   A unique and secret key used to identify the application.
     */
    public static void configure(Application application, String appSecret) {
        getInstance().configureInstanceWithRequiredAppSecret(application, appSecret);
    }

    /**
     * Configure the SDK without an app secret.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     */
    public static void configure(Application application) {
        getInstance().configureInstance(application, null, true);
    }

    /**
     * Start services.
     * This may be called only once per service per application process lifetime.
     *
     * @param services List of services to use.
     */
    @SafeVarargs
    public static void start(Class<? extends AppCenterService>... services) {
        getInstance().startServices(true, services);
    }

    /**
     * Configure the SDK with the list of services to start with an app secret parameter.
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
     * Configure the SDK with the list of services to start without an app secret.
     * This may be called only once per application process lifetime.
     *
     * @param application Your application object.
     * @param services    List of services to use.
     */
    @SafeVarargs
    public static void start(Application application, Class<? extends AppCenterService>... services) {
        getInstance().configureAndStartServices(application, null, true, services);
    }

    /**
     * Start services from a library. This does not configure app secret and can be called multiple
     * times without side effect.
     * <p>
     * This will not start the service at app level, it will enable the service only for the library.
     * <p>
     * Please note that not all services support this start mode,
     * you can refer to the documentation of each service to know if this is supported in libraries.
     *
     * @param context  Context.
     * @param services List of services to use.
     */
    @SafeVarargs
    public static void startFromLibrary(Context context, Class<? extends AppCenterService>... services) {
        getInstance().startInstanceFromLibrary(context, services);
    }

    /**
     * Set custom logger.
     *
     * @param logger custom logger.
     */
    public static void setLogger(Logger logger) {
        AppCenterLog.setLogger(logger);
    }

    /**
     * Allow or disallow network requests.
     * If network requests is disallowed then SDK continue to collect data but they will be sent only when network requests will be allowed.
     * This value is stored in SharedPreferences.
     *
     * @param isAllowed true to allow, false to disallow.
     */
    public static void setNetworkRequestsAllowed(boolean isAllowed) {
        getInstance().setInstanceNetworkRequestsAllowed(isAllowed);
    }

    /**
     * Check whether network requests are allowed or disallowed.
     * Due to this value is taken from SharedPreferences, the method before the start of App Center returns the last value set or true if the value wasn't changed before App Center start.
     *
     * @return true if network requests are allowed, false otherwise.
     */
    public static boolean isNetworkRequestsAllowed() {
        return getInstance().isInstanceNetworkRequestsAllowed();
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
     * Enable or disable the SDK as a whole. In addition to the App Center resources, it will also
     * enable or disable all services registered via {@link #start(Application, String, Class[])}.
     * <p>
     * The state is persisted in the device's storage across application launches.
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
        return getInstance().getInstanceInstallIdAsync();
    }

    /**
     * Set the SQLite database storage size. Returns true if the operation succeeded. If the new size
     * is smaller than the previous size (database is shrinking) and the capacity is greater than
     * the new size, then the operation will fail and a warning will be emitted. Can only be called
     * once per app lifetime and only before AppCenter.start(...).
     * <p>
     * If the size is not a multiple of database page size (default is 4096 bytes), the next multiple
     * of page size is used as the new maximum size.
     *
     * @param storageSizeInBytes New size for the SQLite db in bytes.
     * @return Future with true result if succeeded, otherwise future with false result.
     */
    public static AppCenterFuture<Boolean> setMaxStorageSize(long storageSizeInBytes) {
        return getInstance().setInstanceMaxStorageSizeAsync(storageSizeInBytes);
    }

    /**
     * {@link #setUserId(String)} implementation at instance level.
     */
    private synchronized void setInstanceUserId(String userId) {
        if (!mConfiguredFromApp) {
            AppCenterLog.error(LOG_TAG, "AppCenter must be configured from application, libraries cannot use call setUserId.");
            return;
        }
        if (mAppSecret == null && mTransmissionTargetToken == null) {
            AppCenterLog.error(LOG_TAG, "AppCenter must be configured with a secret from application to call setUserId.");
            return;
        }
        if (userId != null) {
            if (mAppSecret != null && !UserIdContext.checkUserIdValidForAppCenter(userId)) {
                return;
            }
            if (mTransmissionTargetToken != null && !UserIdContext.checkUserIdValidForOneCollector(userId)) {
                return;
            }
        }
        UserIdContext.getInstance().setUserId(userId);
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
                    if (mAppSecret != null) {
                        AppCenterLog.info(LOG_TAG, "The log url of App Center endpoint has been changed to " + logUrl);
                        mChannel.setLogUrl(logUrl);
                    } else {
                        AppCenterLog.info(LOG_TAG, "The log url of One Collector endpoint has been changed to " + logUrl);
                        mOneCollectorChannelListener.setLogUrl(logUrl);
                    }
                }
            });
        }
    }

    /**
     * {@link #setNetworkRequestsAllowed(boolean)} implementation at instance level.
     *
     * @param isAllowed true to allow, false to disallow.
     */
    private synchronized void setInstanceNetworkRequestsAllowed(final boolean isAllowed) {
        if (!AppCenter.isConfigured()) {
            mAllowedNetworkRequests = isAllowed;
            return;
        }
        if (isInstanceNetworkRequestsAllowed() == isAllowed) {
            AppCenterLog.info(LOG_TAG, "Network requests are already " + (isAllowed ? "allowed" : "forbidden"));
            return;
        }
        SharedPreferencesManager.putBoolean(PrefStorageConstants.ALLOWED_NETWORK_REQUEST, isAllowed);
        if (mChannel != null) {
            mChannel.setNetworkRequests(isAllowed);
        }
        AppCenterLog.info(LOG_TAG, "Set network requests " + (isAllowed ? "allowed" : "forbidden"));
    }

    /**
     * {@link #isNetworkRequestsAllowed()} implementation at instance level.
     *
     * @return true if network requests are allowed, false otherwise.
     */
    private synchronized boolean isInstanceNetworkRequestsAllowed() {
        boolean defaultValue = mAllowedNetworkRequests == null ? true : mAllowedNetworkRequests;
        if (!AppCenter.isConfigured()) {
            return defaultValue;
        }
        return SharedPreferencesManager.getBoolean(PrefStorageConstants.ALLOWED_NETWORK_REQUEST, defaultValue);
    }

    /**
     * {@link #setMaxStorageSize(long)} implementation at instance level.
     *
     * @param storageSizeInBytes size to set SQLite database to in bytes.
     * @return future of result of set maximum storage size.
     */
    private synchronized AppCenterFuture<Boolean> setInstanceMaxStorageSizeAsync(long storageSizeInBytes) {
        DefaultAppCenterFuture<Boolean> setMaxStorageSizeFuture = new DefaultAppCenterFuture<>();
        if (mConfiguredFromApp) {
            AppCenterLog.error(LOG_TAG, "setMaxStorageSize may not be called after App Center has been configured.");
            setMaxStorageSizeFuture.complete(false);
            return setMaxStorageSizeFuture;
        }
        if (storageSizeInBytes < MINIMUM_STORAGE_SIZE) {
            AppCenterLog.error(LOG_TAG, "Maximum storage size must be at least " + MINIMUM_STORAGE_SIZE + " bytes.");
            setMaxStorageSizeFuture.complete(false);
            return setMaxStorageSizeFuture;
        }
        if (mSetMaxStorageSizeFuture != null) {
            AppCenterLog.error(LOG_TAG, "setMaxStorageSize may only be called once per app launch.");
            setMaxStorageSizeFuture.complete(false);
            return setMaxStorageSizeFuture;
        }
        mMaxStorageSizeInBytes = storageSizeInBytes;
        mSetMaxStorageSizeFuture = setMaxStorageSizeFuture;
        return setMaxStorageSizeFuture;
    }

    /**
     * {@link #isConfigured()} implementation at instance level.
     */
    private synchronized boolean isInstanceConfigured() {
        return mApplication != null;
    }

    /**
     * Configure SDK without services with app secret internal function.
     */
    private void configureInstanceWithRequiredAppSecret(Application application, String appSecret) {
        if (appSecret == null || appSecret.isEmpty()) {
            AppCenterLog.error(LOG_TAG, "appSecret may not be null or empty.");
        } else {
            configureInstance(application, appSecret, true);
        }
    }

    /**
     * Internal SDK configuration.
     *
     * @param application      application context.
     * @param secretString     a unique and secret key used to identify the application.
     *                         It can be null since a transmission target token can be set later.
     * @param configureFromApp true if configuring from app, false if called from a library.
     * @return true if configuration was successful, false otherwise.
     */
    private synchronized boolean configureInstance(Application application, String secretString, final boolean configureFromApp) {

        /* Check parameters. */
        if (application == null) {
            AppCenterLog.error(LOG_TAG, "Application context may not be null.");
            return false;
        }

        /* Enable a default log level for debuggable applications. */
        if (!mLogLevelConfigured && (application.getApplicationInfo().flags & FLAG_DEBUGGABLE) == FLAG_DEBUGGABLE) {
            AppCenterLog.setLogLevel(Log.WARN);
        }

        /* Configure app secret and/or transmission target. */
        String previousAppSecret = mAppSecret;
        if (configureFromApp && !configureSecretString(secretString)) {
            return false;
        }

        /* Skip configuration of global states if already done. */
        if (mHandler != null) {

            /* If app started after library with an app secret, set app secret on channel now. */
            if (mAppSecret != null && !mAppSecret.equals(previousAppSecret)) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mChannel.setAppSecret(mAppSecret);
                        applyStorageMaxSize();
                    }
                });
            }
            return true;
        }

        /* Store application to use it later for registering services as lifecycle callbacks. */
        mApplication = application;
        mContext = getApplicationContext(application);
        if (isDeviceProtectedStorage(mContext)) {

            /*
             * In this mode storing sensitive is strongly discouraged, but App Center considers regular storage as
             * not secure as well, so all tokens are encrypted separately anyway. Just warn about it.
             */
            AppCenterLog.warn(LOG_TAG,
                    "A user is locked, credential-protected private app data storage is not available.\n" +
                    "App Center will use device-protected data storage that available without user authentication.\n" +
                    "Please note that it's a separate storage, all settings and pending logs won't be shared with regular storage.");
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
        mApplicationLifecycleListener = new ApplicationLifecycleListener(mHandler);
        mApplication.registerActivityLifecycleCallbacks(mApplicationLifecycleListener);

        /* The rest of initialization is done in background as we need storage. */
        mServices = new HashSet<>();
        mServicesStartedFromLibrary = new HashSet<>();
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                finishConfiguration(configureFromApp);
            }
        });
        AppCenterLog.info(LOG_TAG, "App Center SDK configured successfully.");
        return true;
    }


    /**
     * Configure app secret.
     *
     * @param secretString a unique and secret key used to identify the application.
     *                     It can be null since a transmission target token can be set later.
     * @return false if app secret already configured or invalid.
     */
    private boolean configureSecretString(String secretString) {

        /* We don't support overriding the app secret. */
        if (mConfiguredFromApp) {
            AppCenterLog.warn(LOG_TAG, "App Center may only be configured once.");
            return false;
        }
        mConfiguredFromApp = true;

        /* A null secret is still valid since some services don't require it. */
        if (secretString != null) {

            /* Init parsing, the app secret string can contain other secrets.  */
            String[] pairs = secretString.split(PAIR_DELIMITER);

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
    private void finishConfiguration(boolean configureFromApp) {

        /* Load some global constants. */
        Constants.loadFromContext(mContext);

        /* If parameters are valid, init context related resources. */
        FileManager.initialize(mContext);
        SharedPreferencesManager.initialize(mContext);

        /* Set network requests allowed. */
        if (mAllowedNetworkRequests != null) {
            SharedPreferencesManager.putBoolean(PrefStorageConstants.ALLOWED_NETWORK_REQUEST, mAllowedNetworkRequests);
        }

        /* Initialize session storage. */
        SessionContext.getInstance();

        /* Get enabled state. */
        boolean enabled = isInstanceEnabled();

        /* Instantiate HTTP client if it doesn't exist as a dependency. */
        HttpClient httpClient = DependencyConfiguration.getHttpClient();
        if (httpClient == null) {
            httpClient = createHttpClient(mContext);
        }

        /* Init channel. */
        mLogSerializer = new DefaultLogSerializer();
        mLogSerializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        mChannel = new DefaultChannel(mContext, mAppSecret, mLogSerializer, httpClient, mHandler);

        /* Complete set maximum storage size future if starting from app. */
        if (configureFromApp) {
            applyStorageMaxSize();
        } else {

            /* If from library, we apply storage size only later, we have to try using the default value in the mean time. */
            mChannel.setMaxStorageSize(DEFAULT_MAX_STORAGE_SIZE_IN_BYTES);
        }
        mChannel.setEnabled(enabled);
        mChannel.addGroup(CORE_GROUP, DEFAULT_TRIGGER_COUNT, DEFAULT_TRIGGER_INTERVAL, DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS, null, null);
        mOneCollectorChannelListener = new OneCollectorChannelListener(mChannel, mLogSerializer, httpClient, IdHelper.getInstallId());
        if (mLogUrl != null) {
            if (mAppSecret != null) {
                AppCenterLog.info(LOG_TAG, "The log url of App Center endpoint has been changed to " + mLogUrl);
                mChannel.setLogUrl(mLogUrl);
            } else {
                AppCenterLog.info(LOG_TAG, "The log url of One Collector endpoint has been changed to " + mLogUrl);
                mOneCollectorChannelListener.setLogUrl(mLogUrl);
            }
        }
        mChannel.addListener(mOneCollectorChannelListener);

        /* Disable listening network if we start while being disabled. */
        if (!enabled) {
            NetworkStateHelper.getSharedInstance(mContext).close();
        }

        /* Init uncaught exception handler. */
        mUncaughtExceptionHandler = new UncaughtExceptionHandler(mHandler, mChannel);
        if (enabled) {
            mUncaughtExceptionHandler.register();
        }
        AppCenterLog.debug(LOG_TAG, "App Center initialized.");
    }

    @WorkerThread
    private void applyStorageMaxSize() {
        boolean resizeResult = mChannel.setMaxStorageSize(mMaxStorageSizeInBytes);
        if (mSetMaxStorageSizeFuture != null) {
            mSetMaxStorageSizeFuture.complete(resizeResult);
        }
    }

    @SafeVarargs
    private final synchronized void startServices(final boolean startFromApp, Class<? extends AppCenterService>... services) {
        if (services == null) {
            AppCenterLog.error(LOG_TAG, "Cannot start services, services array is null. Failed to start services.");
            return;
        }
        if (!isInstanceConfigured()) {
            StringBuilder serviceNames = new StringBuilder();
            for (Class<? extends AppCenterService> service : services) {
                serviceNames.append("\t").append(service.getName()).append("\n");
            }
            AppCenterLog.error(LOG_TAG, "Cannot start services, App Center has not been configured. Failed to start the following services:\n" + serviceNames);
            return;
        }

        /* Start each service and collect info for send start service log. */
        final Collection<AppCenterService> startedServices = new ArrayList<>();
        final Collection<AppCenterService> updatedServices = new ArrayList<>();
        for (Class<? extends AppCenterService> service : services) {
            if (service == null) {
                AppCenterLog.warn(LOG_TAG, "Skipping null service, please check your varargs/array does not contain any null reference.");
            } else {
                try {
                    AppCenterService serviceInstance = (AppCenterService) service.getMethod("getInstance").invoke(null);
                    startOrUpdateService(serviceInstance, startedServices, updatedServices, startFromApp);
                } catch (Exception e) {
                    AppCenterLog.error(LOG_TAG, "Failed to get service instance '" + service.getName() + "', skipping it.", e);
                }
            }
        }

        /* Post to ensure service started after storage initialized. */
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                finishStartServices(updatedServices, startedServices, startFromApp);
            }
        });
    }

    private void startOrUpdateService(AppCenterService serviceInstance, Collection<AppCenterService> startedServices, Collection<AppCenterService> updatedServices, boolean startFromApp) {
        if (startFromApp) {
            startOrUpdateServiceFromApp(serviceInstance, startedServices, updatedServices);
        } else if (!mServices.contains(serviceInstance)) {
            startServiceFromLibrary(serviceInstance, startedServices);
        }
    }

    private void startOrUpdateServiceFromApp(AppCenterService serviceInstance, Collection<AppCenterService> startedServices, Collection<AppCenterService> updatedServices) {
        String serviceName = serviceInstance.getServiceName();
        if (mServices.contains(serviceInstance)) {
            if (mServicesStartedFromLibrary.remove(serviceInstance)) {
                updatedServices.add(serviceInstance);
            } else {
                AppCenterLog.warn(LOG_TAG, "App Center has already started the service with class name: " + serviceInstance.getServiceName());
            }
        } else if (mAppSecret == null && serviceInstance.isAppSecretRequired()) {
            AppCenterLog.error(LOG_TAG, "App Center was started without app secret, but the service requires it; not starting service " + serviceName + ".");
        } else {
            startService(serviceInstance, startedServices);
        }
    }

    private void startServiceFromLibrary(AppCenterService serviceInstance, Collection<AppCenterService> startedServices) {

        /*
         * We use the same app secret required check as services requiring app secret
         * also requires being started from application and are not supported by libraries.
         */
        String serviceName = serviceInstance.getServiceName();
        if (serviceInstance.isAppSecretRequired()) {
            AppCenterLog.error(LOG_TAG, "This service cannot be started from a library: " + serviceName + ".");
        } else if (startService(serviceInstance, startedServices)) {
            mServicesStartedFromLibrary.add(serviceInstance);
        }
    }

    private boolean startService(AppCenterService serviceInstance, Collection<AppCenterService> startedServices) {
        String serviceName = serviceInstance.getServiceName();
        if (ServiceInstrumentationUtils.isServiceDisabledByInstrumentation(serviceName)) {
            AppCenterLog.debug(LOG_TAG, "Instrumentation variable to disable service has been set; not starting service " + serviceName + ".");
            return false;
        } else {
            serviceInstance.onStarting(mAppCenterHandler);
            mApplicationLifecycleListener.registerApplicationLifecycleCallbacks(serviceInstance);
            mApplication.registerActivityLifecycleCallbacks(serviceInstance);
            mServices.add(serviceInstance);
            startedServices.add(serviceInstance);
            return true;
        }
    }

    @WorkerThread
    private void finishStartServices(Iterable<AppCenterService> updatedServices, Iterable<AppCenterService> startedServices, boolean startFromApp) {

        /* Update existing services with app secret and/or transmission target. */
        for (AppCenterService service : updatedServices) {
            service.onConfigurationUpdated(mAppSecret, mTransmissionTargetToken);
            AppCenterLog.info(LOG_TAG, service.getClass().getSimpleName() + " service configuration updated.");
        }

        /* Start new services. */
        boolean enabled = isInstanceEnabled();
        for (AppCenterService service : startedServices) {
            Map<String, LogFactory> logFactories = service.getLogFactories();
            if (logFactories != null) {
                for (Map.Entry<String, LogFactory> logFactory : logFactories.entrySet()) {
                    mLogSerializer.addLogFactory(logFactory.getKey(), logFactory.getValue());
                }
            }
            if (!enabled && service.isInstanceEnabled()) {
                service.setInstanceEnabled(false);
            }
            if (startFromApp) {
                service.onStarted(mContext, mChannel, mAppSecret, mTransmissionTargetToken, true);
                AppCenterLog.info(LOG_TAG, service.getClass().getSimpleName() + " service started from application.");
            } else {
                service.onStarted(mContext, mChannel, null, null, false);
                AppCenterLog.info(LOG_TAG, service.getClass().getSimpleName() + " service started from library.");
            }
        }

        /* If starting from a library, we will send start service log later when app starts with an app secret. */
        if (startFromApp) {

            /* Send start service log. */
            for (AppCenterService service : updatedServices) {
                mStartedServicesNamesToLog.add(service.getServiceName());
            }
            for (AppCenterService service : startedServices) {
                mStartedServicesNamesToLog.add(service.getServiceName());
            }
            sendStartServiceLog();
        }
    }

    /**
     * Queue start service log.
     */
    @WorkerThread
    private void sendStartServiceLog() {
        if (!mStartedServicesNamesToLog.isEmpty() && isInstanceEnabled()) {
            List<String> allServiceNamesToStart = new ArrayList<>(mStartedServicesNamesToLog);
            mStartedServicesNamesToLog.clear();
            StartServiceLog startServiceLog = new StartServiceLog();
            startServiceLog.setServices(allServiceNamesToStart);
            startServiceLog.oneCollectorEnabled(mTransmissionTargetToken != null);
            mChannel.enqueue(startServiceLog, CORE_GROUP, Flags.DEFAULTS);
        }
    }

    private synchronized void configureAndStartServices(Application application, String appSecret, Class<? extends AppCenterService>[] services) {
        if (appSecret == null || appSecret.isEmpty()) {
            AppCenterLog.error(LOG_TAG, "appSecret may not be null or empty.");
        } else {
            configureAndStartServices(application, appSecret, true, services);
        }
    }

    private synchronized void startInstanceFromLibrary(Context context, Class<? extends AppCenterService>[] services) {
        Application application = context != null ? (Application) context.getApplicationContext() : null;
        configureAndStartServices(application, null, false, services);
    }

    private void configureAndStartServices(Application application, String appSecret, boolean startFromApp, Class<? extends AppCenterService>[] services) {
        boolean configuredSuccessfully = configureInstance(application, appSecret, startFromApp);
        if (configuredSuccessfully) {
            startServices(startFromApp, services);
        }
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
    boolean isInstanceEnabled() {
        return SharedPreferencesManager.getBoolean(PrefStorageConstants.KEY_ENABLED, true);
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
            NetworkStateHelper.getSharedInstance(mContext).reopen();
        } else if (switchToDisabled) {
            mUncaughtExceptionHandler.unregister();
            NetworkStateHelper.getSharedInstance(mContext).close();
        }

        /* Update state now if true, services are checking this. */
        if (enabled) {
            SharedPreferencesManager.putBoolean(PrefStorageConstants.KEY_ENABLED, true);
        }

        /* Send started services. */
        if (!mStartedServicesNamesToLog.isEmpty() && switchToEnabled) {
            sendStartServiceLog();
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
            SharedPreferencesManager.putBoolean(PrefStorageConstants.KEY_ENABLED, false);
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
    private synchronized AppCenterFuture<UUID> getInstanceInstallIdAsync() {
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

    /**
     * Set the user identifier for logs sent for the default target token when the secret
     * passed in {@link AppCenter#start(Application, String, Class[])} contains "target={targetToken}".
     * <p>
     * For App Center servers the user identifier maximum length is 256 characters.
     * <p>
     * AppCenter must be configured or started before this API can be used.
     *
     * @param userId user identifier.
     */
    public static void setUserId(String userId) {
        getInstance().setInstanceUserId(userId);
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
    void resetApplication() {
        mApplication = null;
    }

    @VisibleForTesting
    AppCenterHandler getAppCenterHandler() {
        return mAppCenterHandler;
    }

    @VisibleForTesting
    UncaughtExceptionHandler getUncaughtExceptionHandler() {
        return mUncaughtExceptionHandler;
    }

    @VisibleForTesting
    public void setChannel(Channel channel) {
        mChannel = channel;
    }
}
