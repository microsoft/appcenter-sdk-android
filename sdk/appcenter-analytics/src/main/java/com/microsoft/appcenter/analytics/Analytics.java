package com.microsoft.appcenter.analytics;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.analytics.channel.AnalyticsListener;
import com.microsoft.appcenter.analytics.channel.SessionTracker;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Analytics service.
 */
public class Analytics extends AbstractAppCenterService {

    /**
     * Max length of event/page name.
     */
    @VisibleForTesting
    static final int MAX_NAME_LENGTH = 256;

    /**
     * Max length of properties.
     */
    @VisibleForTesting
    static final int MAX_PROPERTY_ITEM_LENGTH = 64;

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Analytics";

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the analytics group.
     */
    private static final String ANALYTICS_GROUP = "group_analytics";

    /**
     * Activity suffix to exclude from generated page names.
     */
    private static final String ACTIVITY_SUFFIX = "Activity";

    /**
     * Max number of properties.
     */
    private static final int MAX_PROPERTY_COUNT = 5;

    /**
     * Shared instance.
     */
    private static Analytics sInstance = null;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * The map of transmission targets.
     */
    private final Map<String, AnalyticsTransmissionTarget> mTransmissionTargets;

    /**
     * The default transmission target.
     */
    private AnalyticsTransmissionTarget mDefaultTransmissionTarget;

    /**
     * Current activity to replay onResume when enabled in foreground.
     */
    private WeakReference<Activity> mCurrentActivity;

    /**
     * Session tracker.
     */
    private SessionTracker mSessionTracker;

    /**
     * Custom analytics listener.
     */
    private AnalyticsListener mAnalyticsListener;

    /**
     * Automatic page tracking flag.
     * TODO the backend does not support pages yet so the default value would be true after the service becomes public.
     */
    private boolean mAutoPageTrackingEnabled = false;

    /**
     * Init.
     */
    private Analytics() {
        mFactories = new HashMap<>();
        mFactories.put(StartSessionLog.TYPE, new StartSessionLogFactory());
        mFactories.put(PageLog.TYPE, new PageLogFactory());
        mFactories.put(EventLog.TYPE, new EventLogFactory());
        mTransmissionTargets = new HashMap<>();
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized Analytics getInstance() {
        if (sInstance == null) {
            sInstance = new Analytics();
        }
        return sInstance;
    }

    @VisibleForTesting
    static synchronized void unsetInstance() {
        sInstance = null;
    }

    /**
     * Check whether Analytics service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable Analytics service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Sets an analytics listener.
     * <p>
     * Note: it needs to be protected for Xamarin to change it back to public in bindings.
     *
     * @param listener The custom analytics listener.
     */
    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    protected static void setListener(AnalyticsListener listener) {
        getInstance().setInstanceListener(listener);
    }

    /**
     * Check if automatic page tracking is enabled.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @return true if automatic page tracking is enabled. false otherwise.
     * @see #setAutoPageTrackingEnabled(boolean)
     */
    @SuppressWarnings("WeakerAccess")
    protected static boolean isAutoPageTrackingEnabled() {
        return getInstance().isInstanceAutoPageTrackingEnabled();
    }

    /**
     * If enabled (which is the default), automatic page tracking will call {@link #trackPage(String, Map)}
     * automatically every time an activity is resumed, with a generated name and no properties.
     * Call this method with false if you want to track pages yourself in your application.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param autoPageTrackingEnabled true to let the service track pages automatically, false otherwise (default state is true).
     */
    @SuppressWarnings("WeakerAccess")
    protected static void setAutoPageTrackingEnabled(boolean autoPageTrackingEnabled) {
        getInstance().setInstanceAutoPageTrackingEnabled(autoPageTrackingEnabled);
    }

    /**
     * Track a custom page with name.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param name A page name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    protected static void trackPage(String name) {
        trackPage(name, null);
    }

    /**
     * Track a custom page with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     * Any length of name/keys/values that are longer than each limit will be truncated.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param name       A page name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    protected static void trackPage(String name, Map<String, String> properties) {
        final String logType = "Page";
        name = validateName(name, logType);
        if (name != null) {
            Map<String, String> validatedProperties = validateProperties(properties, name, logType);
            getInstance().trackPageAsync(name, validatedProperties);
        }
    }

    /**
     * Track a custom event with name.
     *
     * @param name An event name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void trackEvent(String name) {
        trackEvent(name, null, null);
    }

    /**
     * Track a custom event with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     * Any length of name/keys/values that are longer than each limit will be truncated.
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    public static void trackEvent(String name, Map<String, String> properties) {
        trackEvent(name, properties, null);
    }

    /**
     * Track a custom event with name and transmissionTarget.
     *
     * @param name               A page name.
     * @param transmissionTarget The transmissionTarget for this event.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    static void trackEvent(String name, AnalyticsTransmissionTarget transmissionTarget) {
        trackEvent(name, null, transmissionTarget);
    }

    /**
     * Track a custom event with name and optional properties and optional transmissionTarget.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     * Any length of name/keys/values that are longer than each limit will be truncated.
     *
     * @param name               An event name.
     * @param properties         Optional properties.
     * @param transmissionTarget Optional transmissionTarget.
     */
    @SuppressWarnings("WeakerAccess")
    static void trackEvent(String name, Map<String, String> properties, AnalyticsTransmissionTarget transmissionTarget) {
        final String logType = "Event";
        name = validateName(name, logType);
        if (name != null) {
            Map<String, String> validatedProperties = validateProperties(properties, name, logType);
            getInstance().trackEventAsync(name, validatedProperties, transmissionTarget);
        }
    }

    /**
     * Get a transmission target to use to track events. Will create a new transmission target if necessary.
     *
     * @param transmissionTargetToken A string to identify a transmission target.
     * @return a transmission target.
     */
    public static AnalyticsTransmissionTarget getTransmissionTarget(String transmissionTargetToken) {
        return getInstance().getInstanceTransmissionTarget(transmissionTargetToken);
    }

    /**
     * Generate a page name for an activity.
     *
     * @param activityClass activity class.
     * @return page name.
     */
    private static String generatePageName(Class<?> activityClass) {
        String name = activityClass.getSimpleName();
        String suffix = ACTIVITY_SUFFIX;
        if (name.endsWith(suffix) && name.length() > suffix.length()) {
            return name.substring(0, name.length() - suffix.length());
        } else {
            return name;
        }
    }

    /**
     * Validates name.
     *
     * @param name    Log name to validate.
     * @param logType Log type.
     * @return <code>null</code> if validation failed, otherwise a valid name within the length limit will be returned.
     */
    private static String validateName(String name, String logType) {
        if (name == null || name.isEmpty()) {
            AppCenterLog.error(Analytics.LOG_TAG, logType + " name cannot be null or empty.");
            return null;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            AppCenterLog.warn(Analytics.LOG_TAG, String.format("%s '%s' : name length cannot be longer than %s characters. Name will be truncated.", logType, name, MAX_NAME_LENGTH));
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        return name;
    }

    /**
     * Validates properties.
     *
     * @param properties Properties collection to validate.
     * @param logName    Log name.
     * @param logType    Log type.
     * @return valid properties collection with maximum size of 5.
     */
    private static Map<String, String> validateProperties(Map<String, String> properties, String logName, String logType) {
        if (properties == null) {
            return null;
        }
        String message;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (result.size() >= MAX_PROPERTY_COUNT) {
                message = String.format("%s '%s' : properties cannot contain more than %s items. Skipping other properties.", logType, logName, MAX_PROPERTY_COUNT);
                AppCenterLog.warn(Analytics.LOG_TAG, message);
                break;
            }
            if (key == null || key.isEmpty()) {
                message = String.format("%s '%s' : a property key cannot be null or empty. Property will be skipped.", logType, logName);
                AppCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            if (value == null) {
                message = String.format("%s '%s' : property '%s' : property value cannot be null. Property '%s' will be skipped.", logType, logName, key, key);
                AppCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            if (key.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s '%s' : property '%s' : property key length cannot be longer than %s characters. Property key will be truncated.", logType, logName, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(Analytics.LOG_TAG, message);
                key = key.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            if (value.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s '%s' : property '%s' : property value cannot be longer than %s characters. Property value will be truncated.", logType, logName, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(Analytics.LOG_TAG, message);
                value = value.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            result.put(key, value);
        }
        return result;
    }

    /**
     * Get a transmission target to use to track events. Will create a new transmission target if necessary.
     *
     * @param transmissionTargetToken A string to identify a transmission target.
     * @return a transmission target.
     */
    private synchronized AnalyticsTransmissionTarget getInstanceTransmissionTarget(String transmissionTargetToken) {
        if (transmissionTargetToken == null || transmissionTargetToken.isEmpty()) {
            return null;
        } else {
            AnalyticsTransmissionTarget transmissionTarget = mTransmissionTargets.get(transmissionTargetToken);
            if (transmissionTarget != null) {
                AppCenterLog.debug(LOG_TAG, "Returning transmission target found with token " + transmissionTargetToken);
                return transmissionTarget;
            }
            transmissionTarget = new AnalyticsTransmissionTarget(transmissionTargetToken);
            AppCenterLog.debug(LOG_TAG, "Created transmission target with token " + transmissionTargetToken);
            mTransmissionTargets.put(transmissionTargetToken, transmissionTarget);
            return transmissionTarget;
        }
    }

    @Override
    public boolean isAppSecretRequired() {
        return false;
    }

    @Override
    protected String getGroupName() {
        return ANALYTICS_GROUP;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    protected String getLoggerTag() {
        return LOG_TAG;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    public synchronized void onActivityResumed(final Activity activity) {
        final Runnable updateCurrentActivityRunnable = new Runnable() {

            @Override
            public void run() {
                mCurrentActivity = new WeakReference<>(activity);
            }
        };
        post(new Runnable() {

            @Override
            public void run() {
                updateCurrentActivityRunnable.run();
                processOnResume(activity);
            }
        }, updateCurrentActivityRunnable, updateCurrentActivityRunnable);
    }

    /**
     * On an activity being resumed, start a new session if needed
     * and track current page automatically if that mode is enabled.
     *
     * @param activity current activity.
     */
    private void processOnResume(Activity activity) {
        mSessionTracker.onActivityResumed();
        if (mAutoPageTrackingEnabled) {
            queuePage(generatePageName(activity.getClass()), null);
        }
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        final Runnable updateCurrentActivityRunnable = new Runnable() {

            @Override
            public void run() {
                mCurrentActivity = null;
            }
        };
        post(new Runnable() {

            @Override
            public void run() {
                updateCurrentActivityRunnable.run();
                mSessionTracker.onActivityPaused();
            }
        }, updateCurrentActivityRunnable, updateCurrentActivityRunnable);
    }

    @Override
    protected Channel.GroupListener getChannelListener() {
        return new Channel.GroupListener() {

            @Override
            public void onBeforeSending(Log log) {
                if (mAnalyticsListener != null) {
                    mAnalyticsListener.onBeforeSending(log);
                }
            }

            @Override
            public void onSuccess(Log log) {
                if (mAnalyticsListener != null) {
                    mAnalyticsListener.onSendingSucceeded(log);
                }
            }

            @Override
            public void onFailure(Log log, Exception e) {
                if (mAnalyticsListener != null) {
                    mAnalyticsListener.onSendingFailed(log, e);
                }
            }
        };
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    @Override
    protected synchronized void applyEnabledState(boolean enabled) {

        /* Start session tracker when enabled. */
        if (enabled) {
            mSessionTracker = new SessionTracker(mChannel, ANALYTICS_GROUP);
            mChannel.addListener(mSessionTracker);
            if (mCurrentActivity != null) {
                Activity activity = mCurrentActivity.get();
                if (activity != null) {
                    processOnResume(activity);
                }
            }
        }

        /* Release resources if disabled and enabled before with resources. */
        else if (mSessionTracker != null) {
            mChannel.removeListener(mSessionTracker);
            mSessionTracker.clearSessions();
            mSessionTracker = null;
        }
    }

    /**
     * Send a page.
     *
     * @param name       page name.
     * @param properties optional properties.
     */
    private synchronized void trackPageAsync(final String name, final Map<String, String> properties) {
        post(new Runnable() {

            @Override
            public void run() {
                queuePage(name, properties);
            }
        });
    }

    /**
     * Enqueue page log now.
     */
    @WorkerThread
    private void queuePage(String name, Map<String, String> properties) {
        PageLog pageLog = new PageLog();
        pageLog.setName(name);
        pageLog.setProperties(properties);
        mChannel.enqueue(pageLog, ANALYTICS_GROUP);
    }

    /**
     * Send an event.
     *
     * @param name       event name.
     * @param properties optional properties.
     */
    private synchronized void trackEventAsync(final String name, final Map<String, String> properties, final AnalyticsTransmissionTarget transmissionTarget) {
        post(new Runnable() {

            @Override
            public void run() {
                EventLog eventLog = new EventLog();
                eventLog.setId(UUIDUtils.randomUUID());
                eventLog.setName(name);
                eventLog.setProperties(properties);
                AnalyticsTransmissionTarget aTransmissionTarget = (transmissionTarget == null) ? mDefaultTransmissionTarget : transmissionTarget;
                if (aTransmissionTarget != null) {
                    eventLog.addTransmissionTarget(aTransmissionTarget.getTransmissionTargetToken());
                }
                mChannel.enqueue(eventLog, ANALYTICS_GROUP);
            }
        });
    }

    /**
     * Implements {@link #isAutoPageTrackingEnabled()}.
     */
    private boolean isInstanceAutoPageTrackingEnabled() {
        return mAutoPageTrackingEnabled;
    }

    /**
     * Implements {@link #setAutoPageTrackingEnabled(boolean)}.
     */
    private synchronized void setInstanceAutoPageTrackingEnabled(boolean autoPageTrackingEnabled) {
        mAutoPageTrackingEnabled = autoPageTrackingEnabled;
    }

    /**
     * Implements {@link #setListener(AnalyticsListener)}.
     */
    private synchronized void setInstanceListener(AnalyticsListener listener) {
        mAnalyticsListener = listener;
    }

    @VisibleForTesting
    WeakReference<Activity> getCurrentActivity() {
        return mCurrentActivity;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, String appSecret, String transmissionTargetToken, @NonNull Channel channel) {
        super.onStarted(context, appSecret, transmissionTargetToken, channel);

        /* Initialize a default transmission target if a token has been provided. */
        mDefaultTransmissionTarget = getInstanceTransmissionTarget(transmissionTargetToken);
    }
}
