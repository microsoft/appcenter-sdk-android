/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.Constants;
import com.microsoft.appcenter.Flags;
import com.microsoft.appcenter.analytics.channel.AnalyticsListener;
import com.microsoft.appcenter.analytics.channel.AnalyticsValidator;
import com.microsoft.appcenter.analytics.channel.SessionTracker;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.json.CommonSchemaEventLogFactory;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.UserIdContext;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Analytics service.
 */
public class Analytics extends AbstractAppCenterService {

    /**
     * Constant marking event of the analytics group.
     */
    static final String ANALYTICS_GROUP = "group_analytics";

    /**
     * Constant marking event of the analytics critical group.
     */
    static final String ANALYTICS_CRITICAL_GROUP = ANALYTICS_GROUP + "_critical";

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "Analytics";

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Activity suffix to exclude from generated page names.
     */
    private static final String ACTIVITY_SUFFIX = "Activity";

    /**
     * Shared instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static Analytics sInstance;

    /**
     * Transmission interval minimum value.
     */
    @VisibleForTesting
    static final int MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS = 3;

    /**
     * Transmission interval maximum value.
     */
    @VisibleForTesting
    static final int MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS = 24 * 60 * 60;

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
    @VisibleForTesting
    AnalyticsTransmissionTarget mDefaultTransmissionTarget;

    /**
     * Current activity to replay onResume when enabled in foreground.
     */
    private WeakReference<Activity> mCurrentActivity;

    /**
     * Application context, if not null it means onStart was called.
     */
    private Context mContext;

    /**
     * True if started from app, false if started only from a library or not yet started at all.
     */
    private boolean mStartedFromApp;

    /**
     * Session tracker.
     */
    private SessionTracker mSessionTracker;

    /**
     * Event validator.
     */
    private AnalyticsValidator mAnalyticsValidator;

    /**
     * Channel listener used by transmission targets to decorate logs.
     */
    private Channel.Listener mAnalyticsTransmissionTargetListener;

    /**
     * Custom analytics listener.
     */
    private AnalyticsListener mAnalyticsListener;

    /**
     * Transmission interval in milliseconds.
     */
    private long mTransmissionInterval;

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
        mFactories.put(CommonSchemaEventLog.TYPE, new CommonSchemaEventLogFactory());
        mTransmissionTargets = new HashMap<>();
        mTransmissionInterval = TimeUnit.SECONDS.toMillis(MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS);
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
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
     * <p>
     * The state is persisted in the device's storage across application launches.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
    }

    /**
     * Set transmission interval. The transmission interval should be between 3 seconds and 86400 seconds (1 day).
     * Should be called before the service is started.
     *
     * @param seconds the latency of sending events to Analytics in seconds.
     * @return <code>true</code> if the interval is set, <code>false</code> otherwise.
     */
    public static boolean setTransmissionInterval(int seconds) {
        return getInstance().setInstanceTransmissionInterval(seconds);
    }

    /**
     * Pauses log transmission. This API cannot be used if the service is disabled.
     * Transmission is resumed:
     * <ul>
     * <li>when calling {@link #resume()}.</li>
     * <li>when restarting the application process and calling AppCenter.start again.</li>
     * <li>when disabling and re-enabling the SDK or the Analytics module.</li>
     * </ul>
     */
    public static void pause() {
        getInstance().pauseInstanceAsync();
    }

    /**
     * Resumes log transmission if paused.
     * This API cannot be used if the service is disabled.
     */
    public static void resume() {
        getInstance().resumeInstanceAsync();
    }

    /**
     * Sets an analytics listener.
     * <p>
     * Note: it needs to be protected for Xamarin to change it back to public in bindings.
     *
     * @param listener The custom analytics listener.
     */
    @SuppressWarnings({"WeakerAccess", "RedundantSuppression"})
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
    @SuppressWarnings("WeakerAccess")
    protected static void trackPage(String name) {
        trackPage(name, null);
    }

    /**
     * Track a custom page with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 20.
     * The properties keys can not be null or empty, maximum allowed key length = 125.
     * The properties values can not be null, maximum allowed value length = 125.
     * Any length of name/keys/values that are longer than each limit will be truncated.
     * <p>
     * TODO the backend does not support that service yet, will be public method later.
     *
     * @param name       A page name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    protected static void trackPage(String name, Map<String, String> properties) {
        getInstance().trackPageAsync(name, properties);
    }

    /**
     * Track a custom event with name.
     * <p>
     * The name cannot be null or empty.
     * <p>
     * Additional validation rules apply depending on the configured secret.
     * <p>
     * For App Center, the name cannot be longer than 256 and is truncated otherwise.
     * For One Collector, the name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.
     *
     * @param name An event name.
     */
    public static void trackEvent(String name) {
        trackEvent(name, null, null, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The name cannot be null or empty.
     * <p>
     * The property names or values cannot be null.
     * <p>
     * Additional validation rules apply depending on the configured secret.
     * <p>
     * For App Center:
     * <ul>
     * <li>The event name cannot be longer than 256 and is truncated otherwise.</li>
     * <li>The property names cannot be empty.</li>
     * <li>The property names and values are limited to 125 characters each (truncated).</li>
     * <li>The number of properties per event is limited to 20 (truncated).</li>
     * </ul>
     * <p>
     * For One Collector:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    public static void trackEvent(String name, Map<String, String> properties) {
        getInstance().trackEventAsync(name, convertProperties(properties), null, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional string properties.
     * <p>
     * The name cannot be null or empty.
     * <p>
     * The property names or values cannot be null.
     * <p>
     * Additional validation rules apply depending on the configured secret.
     * <p>
     * For App Center:
     * <ul>
     * <li>The event name cannot be longer than 256 and is truncated otherwise.</li>
     * <li>The property names cannot be empty.</li>
     * <li>The property names and values are limited to 125 characters each (truncated).</li>
     * <li>The number of properties per event is limited to 20 (truncated).</li>
     * </ul>
     * <p>
     * For One Collector:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     * @param flags      Optional flags. Events tracked with the {@link Flags#CRITICAL}
     *                   flag will take precedence over all other events in storage.
     *                   An event tracked with this option will only be dropped
     *                   if storage must make room for a newer event that is also marked with the
     *                   {@link Flags#CRITICAL} flag.
     */
    public static void trackEvent(String name, Map<String, String> properties, int flags) {
        getInstance().trackEventAsync(name, convertProperties(properties), null, flags);
    }

    /**
     * Track a custom event with name and optional typed properties.
     * <p>
     * The name cannot be null or empty.
     * <p>
     * The property names or values cannot be null.
     * <p>
     * Double values must be finite (NaN or Infinite values are discarded).
     * <p>
     * Additional validation rules apply depending on the configured secret.
     * <p>
     * For App Center:
     * <ul>
     * <li>The event name cannot be longer than 256 and is truncated otherwise.</li>
     * <li>The property names cannot be empty.</li>
     * <li>The property names and values are limited to 125 characters each (truncated).</li>
     * <li>The number of properties per event is limited to 20 (truncated).</li>
     * </ul>
     * <p>
     * For One Collector:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    public static void trackEvent(String name, EventProperties properties) {
        trackEvent(name, properties, Flags.DEFAULTS);
    }

    /**
     * Track a custom event with name and optional typed properties.
     * <p>
     * The name cannot be null or empty.
     * <p>
     * The property names or values cannot be null.
     * <p>
     * Double values must be finite (NaN or Infinite values are discarded).
     * <p>
     * Additional validation rules apply depending on the configured secret.
     * <p>
     * For App Center:
     * <ul>
     * <li>The event name cannot be longer than 256 and is truncated otherwise.</li>
     * <li>The property names cannot be empty.</li>
     * <li>The property names and values are limited to 125 characters each (truncated).</li>
     * <li>The number of properties per event is limited to 20 (truncated).</li>
     * </ul>
     * <p>
     * For One Collector:
     * <ul>
     * <li>The event name needs to match the <tt>[a-zA-Z0-9]((\.(?!(\.|$)))|[_a-zA-Z0-9]){3,99}</tt> regular expression.</li>
     * <li>The <tt>baseData</tt> and <tt>baseDataType</tt> properties are reserved and thus discarded.</li>
     * <li>The full event size when encoded as a JSON string cannot be larger than 1.9MB.</li>
     * </ul>
     *
     * @param name       An event name.
     * @param properties Optional properties.
     * @param flags      Optional flags. Events tracked with the {@link Flags#CRITICAL}
     *                   flag will take precedence over all other events in storage.
     *                   An event tracked with this option will only be dropped
     *                   if storage must make room for a newer event that is also marked with the
     *                   {@link Flags#CRITICAL} flag.
     */
    public static void trackEvent(String name, EventProperties properties, int flags) {
        trackEvent(name, properties, null, flags);
    }

    /**
     * Internal method redirection for trackEvent.
     */
    static void trackEvent(String name, EventProperties properties, AnalyticsTransmissionTarget transmissionTarget, int flags) {
        getInstance().trackEventAsync(name, convertProperties(properties), transmissionTarget, flags);
    }

    /**
     * Internal conversion for properties.
     *
     * @param properties input properties.
     * @return copy as a list.
     */
    private static List<TypedProperty> convertProperties(EventProperties properties) {
        if (properties == null) {
            return null;
        }

        /* Make a copy to avoid concurrent modifications after trackEvent. */
        return new ArrayList<>(properties.getProperties().values());
    }

    /**
     * Internal conversion for properties.
     *
     * @param properties input properties.
     * @return copy as a typed list.
     */
    private static List<TypedProperty> convertProperties(Map<String, String> properties) {
        if (properties == null) {
            return null;
        }
        List<TypedProperty> typedProperties = new ArrayList<>(properties.size());
        for (Map.Entry<String, String> property : properties.entrySet()) {
            StringTypedProperty typedProperty = new StringTypedProperty();
            typedProperty.setName(property.getKey());
            typedProperty.setValue(property.getValue());
            typedProperties.add(typedProperty);
        }
        return typedProperties;
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
     * Get a transmission target to use to track events. Will create a new transmission target if necessary.
     *
     * @param transmissionTargetToken A string to identify a transmission target.
     * @return a transmission target or null if the token is invalid.
     */
    private synchronized AnalyticsTransmissionTarget getInstanceTransmissionTarget(String transmissionTargetToken) {
        if (transmissionTargetToken == null || transmissionTargetToken.isEmpty()) {
            AppCenterLog.error(LOG_TAG, "Transmission target token may not be null or empty.");
            return null;
        } else if (!AppCenter.isConfigured()) {
            AppCenterLog.error(LOG_TAG, "Cannot create transmission target, AppCenter is not configured or started.");
            return null;
        } else {
            AnalyticsTransmissionTarget transmissionTarget = mTransmissionTargets.get(transmissionTargetToken);
            if (transmissionTarget != null) {
                AppCenterLog.debug(LOG_TAG, "Returning transmission target found with token " + transmissionTargetToken);
                return transmissionTarget;
            }
            transmissionTarget = createAnalyticsTransmissionTarget(transmissionTargetToken);
            mTransmissionTargets.put(transmissionTargetToken, transmissionTarget);
            return transmissionTarget;
        }
    }

    /**
     * Unconditionally create a new transmission target at root level, even if one exists with the given token.
     *
     * @param transmissionTargetToken the token.
     * @return the created target.
     */
    private AnalyticsTransmissionTarget createAnalyticsTransmissionTarget(String transmissionTargetToken) {
        final AnalyticsTransmissionTarget transmissionTarget = new AnalyticsTransmissionTarget(transmissionTargetToken, null);
        AppCenterLog.debug(LOG_TAG, "Created transmission target with token " + transmissionTargetToken);
        postCommandEvenIfDisabled(new Runnable() {

            @Override
            public void run() {
                transmissionTarget.initInBackground(mContext, mChannel);
            }
        });
        return transmissionTarget;
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

    @Override
    protected long getTriggerInterval() {
        return mTransmissionInterval;
    }

    /**
     * On an activity being resumed, start a new session if needed
     * and track current page automatically if that mode is enabled.
     *
     * @param activity current activity.
     */
    private void processOnResume(Activity activity) {
        if (mSessionTracker != null) {
            mSessionTracker.onActivityResumed();
            if (mAutoPageTrackingEnabled) {
                queuePage(generatePageName(activity.getClass()), null);
            }
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
                if (mSessionTracker != null) {
                    mSessionTracker.onActivityPaused();
                }
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

        /* If we enabled the service. */
        if (enabled) {
            mChannel.addGroup(ANALYTICS_CRITICAL_GROUP, getTriggerCount(), Constants.DEFAULT_TRIGGER_INTERVAL, getTriggerMaxParallelRequests(), null, getChannelListener());

            /* Check if service started at application level and enable corresponding features. */
            startAppLevelFeatures();
        }

        /* On disabling service. */
        else {
            mChannel.removeGroup(ANALYTICS_CRITICAL_GROUP);

            /* Cleanup resources. */
            if (mAnalyticsValidator != null) {
                mChannel.removeListener(mAnalyticsValidator);
                mAnalyticsValidator = null;
            }
            if (mSessionTracker != null) {
                mChannel.removeListener(mSessionTracker);
                mSessionTracker.clearSessions();
                mSessionTracker = null;
            }
            if (mAnalyticsTransmissionTargetListener != null) {
                mChannel.removeListener(mAnalyticsTransmissionTargetListener);
                mAnalyticsTransmissionTargetListener = null;
            }
        }
    }

    /**
     * Start features at app level, this is not done if only libraries started the service.
     */
    @WorkerThread
    private void startAppLevelFeatures() {

        /* Share the started from app check between all calls. */
        if (mStartedFromApp) {

            /* Enable filtering logs. */
            mAnalyticsValidator = new AnalyticsValidator();
            mChannel.addListener(mAnalyticsValidator);

            /* Start session tracker. */
            mSessionTracker = new SessionTracker(mChannel, ANALYTICS_GROUP);
            mChannel.addListener(mSessionTracker);

            /* If we are in foreground, make sure we send start session log now (and track page). */
            if (mCurrentActivity != null) {
                Activity activity = mCurrentActivity.get();
                if (activity != null) {
                    processOnResume(activity);
                }
            }

            /* Add new channel listener for transmission target. */
            mAnalyticsTransmissionTargetListener = AnalyticsTransmissionTarget.getChannelListener();
            mChannel.addListener(mAnalyticsTransmissionTargetListener);
        }
    }

    /**
     * Send a page.
     *
     * @param name       page name.
     * @param properties optional properties.
     */
    private synchronized void trackPageAsync(final String name, final Map<String, String> properties) {

        /* Make a copy to prevent concurrent modification. */
        final Map<String, String> propertiesCopy = properties != null ? new HashMap<>(properties) : null;
        post(new Runnable() {

            @Override
            public void run() {

                /* This flag is always read/written in the background thread. */
                if (mStartedFromApp) {
                    queuePage(name, propertiesCopy);
                } else {
                    AppCenterLog.error(LOG_TAG, "Cannot track page if not started from app.");
                }
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
        mChannel.enqueue(pageLog, ANALYTICS_GROUP, Flags.DEFAULTS);
    }

    /**
     * Send an event.
     *
     * @param name               event name.
     * @param properties         optional properties.
     * @param transmissionTarget optional target.
     * @param flags              optional flags.
     */
    private synchronized void trackEventAsync(final String name, final List<TypedProperty> properties, final AnalyticsTransmissionTarget transmissionTarget, final int flags) {
        final String userId = UserIdContext.getInstance().getUserId();
        post(new Runnable() {

            @Override
            public void run() {
                AnalyticsTransmissionTarget aTransmissionTarget = (transmissionTarget == null) ? mDefaultTransmissionTarget : transmissionTarget;
                EventLog eventLog = new EventLog();
                if (aTransmissionTarget != null) {
                    if (aTransmissionTarget.isEnabled()) {
                        eventLog.addTransmissionTarget(aTransmissionTarget.getTransmissionTargetToken());
                        eventLog.setTag(aTransmissionTarget);
                        if (aTransmissionTarget == mDefaultTransmissionTarget) {
                            eventLog.setUserId(userId);
                        }
                    } else {
                        AppCenterLog.error(LOG_TAG, "This transmission target is disabled.");
                        return;
                    }
                } else if (!mStartedFromApp) {
                    AppCenterLog.error(LOG_TAG, "Cannot track event using Analytics.trackEvent if not started from app, please start from the application or use Analytics.getTransmissionTarget.");
                    return;
                }
                eventLog.setId(UUID.randomUUID());
                eventLog.setName(name);
                eventLog.setTypedProperties(properties);

                /* Filter and validate flags. For now we support only persistence. */
                int filteredFlags = Flags.getPersistenceFlag(flags, true);
                mChannel.enqueue(eventLog, filteredFlags == Flags.CRITICAL ? ANALYTICS_CRITICAL_GROUP : ANALYTICS_GROUP, filteredFlags);
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

    /**
     * Implements {@link #pause()}}.
     */
    private synchronized void pauseInstanceAsync() {
        post(new Runnable() {

            @Override
            public void run() {
                mChannel.pauseGroup(ANALYTICS_GROUP, null);
                mChannel.pauseGroup(ANALYTICS_CRITICAL_GROUP, null);
            }
        });
    }

    /**
     * Implements {@link #resume()}}.
     */
    private synchronized void resumeInstanceAsync() {
        post(new Runnable() {

            @Override
            public void run() {
                mChannel.resumeGroup(ANALYTICS_GROUP, null);
                mChannel.resumeGroup(ANALYTICS_CRITICAL_GROUP, null);
            }
        });
    }

    @VisibleForTesting
    WeakReference<Activity> getCurrentActivity() {
        return mCurrentActivity;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        mContext = context;
        mStartedFromApp = startedFromApp;
        super.onStarted(context, channel, appSecret, transmissionTargetToken, startedFromApp);
        setDefaultTransmissionTarget(transmissionTargetToken);
    }

    @Override
    public void onConfigurationUpdated(String appSecret, String transmissionTargetToken) {
        mStartedFromApp = true;
        startAppLevelFeatures();
        setDefaultTransmissionTarget(transmissionTargetToken);
    }

    /**
     * Set a default transmission target if a token has been provided.
     */
    @WorkerThread
    private void setDefaultTransmissionTarget(String transmissionTargetToken) {
        if (transmissionTargetToken != null) {
            mDefaultTransmissionTarget = createAnalyticsTransmissionTarget(transmissionTargetToken);
        }
    }

    /**
     * Set transmission interval. The interval should be between 3 seconds and 86400 seconds (1 day).
     * Should be called before the service is started.
     *
     * @param seconds the latency of sending events to Analytics.
     * @return <code>true</code> if the interval is set, <code>false</code> otherwise.
     */
    private boolean setInstanceTransmissionInterval(int seconds) {
        if (mChannel != null) {
            AppCenterLog.error(LOG_TAG, "Transmission interval should be set before the service is started.");
            return false;
        }
        if (seconds < MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS || seconds > MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS) {
            AppCenterLog.error(LOG_TAG, String.format(Locale.ENGLISH,
                    "The transmission interval is invalid. The value should be between %d seconds and %d seconds (%d day).",
                    MINIMUM_TRANSMISSION_INTERVAL_IN_SECONDS,
                    MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS,
                    TimeUnit.SECONDS.toDays(MAXIMUM_TRANSMISSION_INTERVAL_IN_SECONDS)));
            return false;
        }
        mTransmissionInterval = TimeUnit.SECONDS.toMillis(seconds);
        return true;
    }

    /**
     * Post a command.
     *
     * @param runnable                    command.
     * @param future                      future to hold result.
     * @param valueIfDisabledOrNotStarted result to set in future if AppCenter or Analytics disabled or not started.
     * @param <T>                         result type.
     */
    <T> void postCommand(Runnable runnable, DefaultAppCenterFuture<T> future, T valueIfDisabledOrNotStarted) {

        /*
         * For the purpose of the commands used for this method,
         * it turns out the non get operations use the same flow as get.
         */
        postAsyncGetter(runnable, future, valueIfDisabledOrNotStarted);
    }

    @SuppressWarnings("EmptyMethod")
    @Override
    protected synchronized void post(Runnable runnable) {

        /* Override so that AnalyticsTransmissionTarget has access to it. */
        super.post(runnable);
    }

    /**
     * Post a command that will run on background even if SDK disabled (needs to be configured though).
     *
     * @param runnable command.
     */
    void postCommandEvenIfDisabled(Runnable runnable) {
        post(runnable, runnable, runnable);
    }

    /**
     * Get preference storage key prefix for this service.
     *
     * @return storage key.
     */
    String getEnabledPreferenceKeyPrefix() {
        return getEnabledPreferenceKey() + "/";
    }
}
