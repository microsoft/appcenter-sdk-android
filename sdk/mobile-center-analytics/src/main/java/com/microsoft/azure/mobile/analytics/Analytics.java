package com.microsoft.azure.mobile.analytics;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.azure.mobile.AbstractMobileCenterService;
import com.microsoft.azure.mobile.analytics.channel.AnalyticsListener;
import com.microsoft.azure.mobile.analytics.channel.SessionTracker;
import com.microsoft.azure.mobile.analytics.ingestion.models.EventLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.PageLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.StartSessionLog;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.azure.mobile.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Analytics service.
 */
public class Analytics extends AbstractMobileCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Analytics";

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = MobileCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Constant marking event of the analytics group.
     */
    private static final String ANALYTICS_GROUP = "group_analytics";

    /**
     * Activity suffix to exclude from generated page names.
     */
    private static final String ACTIVITY_SUFFIX = "Activity";

    /**
     * Shared instance.
     */
    private static Analytics sInstance = null;

    /**
     * Log factories managed by this service.
     */
    private final Map<String, LogFactory> mFactories;

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
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable Analytics service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Sets an analytics listener.
     *
     * @param listener The custom crashes listener.
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
    static boolean isAutoPageTrackingEnabled() {
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
    static void setAutoPageTrackingEnabled(boolean autoPageTrackingEnabled) {
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
    static void trackPage(String name) {
        trackPage(name, null);
    }

    /**
     * Track a custom page with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys/names can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param name       A page name.
     * @param properties Optional properties.
     */
    static void trackPage(String name, Map<String, String> properties) {
        final String logType = "Page";
        if (validateName(name, logType)) {
            Map<String, String> validatedProperties = validateProperties(properties, name, logType);
            getInstance().queuePage(name, validatedProperties);
        }
    }

    /**
     * Track a custom event with name.
     *
     * @param name An event name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public static void trackEvent(String name) {
        trackEvent(name, null);
    }

    /**
     * Track a custom event with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys/names can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    public static void trackEvent(String name, Map<String, String> properties) {
        final String logType = "Event";
        if (validateName(name, logType)) {
            Map<String, String> validatedProperties = validateProperties(properties, name, logType);
            getInstance().queueEvent(name, validatedProperties);
        }
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
        if (name.endsWith(suffix) && name.length() > suffix.length())
            return name.substring(0, name.length() - suffix.length());
        else
            return name;
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
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        super.onStarted(context, appSecret, channel);
        applyEnabledState(isInstanceEnabled());
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        mCurrentActivity = new WeakReference<>(activity);
        if (mSessionTracker != null) {
            processOnResume(activity);
        }
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
        mCurrentActivity = null;
        if (mSessionTracker != null) {
            mSessionTracker.onActivityPaused();
        }
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        applyEnabledState(enabled);
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
    private synchronized void applyEnabledState(boolean enabled) {

        /* Delayed initialization once channel ready and enabled (both conditions). */
        if (enabled && mChannel != null && mSessionTracker == null) {
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
        else if (!enabled && mSessionTracker != null) {
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
    private synchronized void queuePage(String name, Map<String, String> properties) {
        if (isInactive())
            return;
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
    private synchronized void queueEvent(String name, Map<String, String> properties) {
        if (isInactive())
            return;
        EventLog eventLog = new EventLog();
        eventLog.setId(UUIDUtils.randomUUID());
        eventLog.setName(name);
        eventLog.setProperties(properties);
        mChannel.enqueue(eventLog, ANALYTICS_GROUP);
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

    /**
     * Validates name.
     *
     * @param name    Log name to validate.
     * @param logType Log type.
     * @return <code>true</code> if validation succeeds, otherwise <code>false</code>.
     */
    private static boolean validateName(String name, String logType) {
        final int maxNameLength = 256;
        if (name == null || name.isEmpty()) {
            MobileCenterLog.error(Analytics.LOG_TAG, logType + " name cannot be null or empty.");
            return false;
        }
        if (name.length() > maxNameLength) {
            MobileCenterLog.error(Analytics.LOG_TAG, String.format("%s '%s' : name length cannot be longer than %s characters.", logType, name, maxNameLength));
            return false;
        }
        return true;
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
        if (properties == null)
            return null;
        String message;
        final int maxPropertiesCount = 5;
        final int maxPropertyItemLength = 64;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (result.size() >= maxPropertiesCount) {
                message = String.format("%s '%s' : properties cannot contain more than %s items. Skipping other properties.", logType, logName, maxPropertiesCount);
                MobileCenterLog.warn(Analytics.LOG_TAG, message);
                break;
            }
            if (property.getKey() == null || property.getKey().isEmpty()) {
                message = String.format("%s '%s' : a property key cannot be null or empty. Property will be skipped.", logType, logName);
                MobileCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            if (property.getKey().length() > maxPropertyItemLength) {
                message = String.format("%s '%s' : property '%s' : property key length cannot be longer than %s characters. Property '%s' will be skipped.", logType, logName, property.getKey(), maxPropertyItemLength, property.getKey());
                MobileCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            if (property.getValue() == null) {
                message = String.format("%s '%s' : property '%s' : property value cannot be null. Property '%s' will be skipped.", logType, logName, property.getKey(), property.getKey());
                MobileCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            if (property.getValue().length() > maxPropertyItemLength) {
                message = String.format("%s '%s' : property '%s' : property value cannot be longer than %s characters. Property '%s' will be skipped.", logType, logName, property.getKey(), maxPropertyItemLength, property.getKey());
                MobileCenterLog.warn(Analytics.LOG_TAG, message);
                continue;
            }
            result.put(property.getKey(), property.getValue());
        }
        return result;
    }
}
