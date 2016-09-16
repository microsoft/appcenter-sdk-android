package com.microsoft.sonoma.analytics;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import com.microsoft.sonoma.analytics.channel.SessionTracker;
import com.microsoft.sonoma.analytics.ingestion.models.EventLog;
import com.microsoft.sonoma.analytics.ingestion.models.PageLog;
import com.microsoft.sonoma.analytics.ingestion.models.StartSessionLog;
import com.microsoft.sonoma.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.sonoma.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.sonoma.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.sonoma.core.AbstractSonomaFeature;
import com.microsoft.sonoma.core.channel.Channel;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.core.utils.SonomaLog;
import com.microsoft.sonoma.core.utils.UUIDUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Analytics feature.
 */
public class Analytics extends AbstractSonomaFeature {

    /**
     * TAG used in logging for Analytics.
     */
    public static final String LOG_TAG = SonomaLog.LOG_TAG + "Analytics";

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
     * Log factories managed by this module.
     */
    private final Map<String, LogFactory> mFactories;

    /**
     * Session tracker.
     */
    private SessionTracker mSessionTracker;

    /**
     * Automatic page tracking flag.
     */
    private boolean mAutoPageTrackingEnabled = true;

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
     * Check whether this feature is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    public static boolean isEnabled() {
        return getInstance().isInstanceEnabled();
    }

    /**
     * Enable or disable this feature.
     *
     * @param enabled true to enable, false to disable.
     */
    public static void setEnabled(boolean enabled) {
        getInstance().setInstanceEnabled(enabled);
    }

    /**
     * Check if automatic page tracking is enabled.
     *
     * @return true if automatic page tracking is enabled. false otherwise.
     * @see #setAutoPageTrackingEnabled(boolean)
     */
    public static boolean isAutoPageTrackingEnabled() {
        return getInstance().isInstanceAutoPageTrackingEnabled();
    }

    /**
     * If enabled (which is the default), automatic page tracking will call {@link #trackPage(String, Map)}
     * automatically every time an activity is resumed, with a generated name and no properties.
     * Call this method with false if you want to track pages yourself in your application.
     *
     * @param autoPageTrackingEnabled true to let the module track pages automatically, false otherwise (default state is true).
     */
    public static void setAutoPageTrackingEnabled(boolean autoPageTrackingEnabled) {
        getInstance().setInstanceAutoPageTrackingEnabled(autoPageTrackingEnabled);
    }

    /**
     * Track a page.
     *
     * @param name       page name.
     */
    public static void trackPage(@NonNull String name) {
        trackPage(name, null);
    }

    /**
     * Track a page.
     *
     * @param name       page name.
     * @param properties optional properties.
     */
    public static void trackPage(@NonNull String name, @Nullable Map<String, String> properties) {
        getInstance().queuePage(name, properties);
    }

    /**
     * Track an event.
     *
     * @param name       event name.
     */
    public static void trackEvent(@NonNull String name) {
        trackEvent(name, null);
    }

    /**
     * Track an event.
     *
     * @param name       event name.
     * @param properties optional properties.
     */
    public static void trackEvent(@NonNull String name, @Nullable Map<String, String> properties) {
        getInstance().queueEvent(name, properties);
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
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull Channel channel) {
        super.onChannelReady(context, channel);
        applyEnabledState(isInstanceEnabled());
    }

    @Override
    public synchronized void onActivityResumed(Activity activity) {
        if (mSessionTracker == null)
            return;
        mSessionTracker.onActivityResumed();
        if (mAutoPageTrackingEnabled)
            queuePage(generatePageName(activity.getClass()), null);
    }

    @Override
    public synchronized void onActivityPaused(Activity activity) {
        if (mSessionTracker != null)
            mSessionTracker.onActivityPaused();
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {
        super.setInstanceEnabled(enabled);
        applyEnabledState(enabled);
    }

    /**
     * React to enable state change.
     *
     * @param enabled current state.
     */
    private synchronized void applyEnabledState(boolean enabled) {

        /* Delayed initialization once channel ready and enabled (both conditions). */
        if (enabled && mChannel != null && mSessionTracker == null) {
            mSessionTracker = new SessionTracker(mChannel);
            mChannel.addListener(mSessionTracker);
        }

        /* Release resources if disabled and enabled before with resources. */
        else if (!enabled && mSessionTracker != null) {
            mChannel.removeListener(mSessionTracker);
            mSessionTracker.clearSessions();
            mSessionTracker = null;
        }
    }

    /**
     * Check if this feature is not active: disabled or not started.
     *
     * @return <code>true</code> if the feature is inactive, <code>false</code> otherwise.
     */
    private synchronized boolean isInactive() {
        if (mChannel == null) {
            SonomaLog.error(LOG_TAG, "Analytics feature not initialized, discarding calls.");
            return true;
        }
        if (!isInstanceEnabled()) {
            SonomaLog.info(LOG_TAG, "Analytics feature not enabled, discarding calls.");
            return true;
        }
        return false;
    }

    /**
     * Send a page.
     *
     * @param name       page name.
     * @param properties optional properties.
     */
    private synchronized void queuePage(@NonNull String name, @Nullable Map<String, String> properties) {
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
    private synchronized void queueEvent(@NonNull String name, @Nullable Map<String, String> properties) {
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
}
