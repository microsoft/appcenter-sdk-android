package avalanche.analytics;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

import avalanche.analytics.channel.SessionTracker;
import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.StartSessionLog;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.analytics.ingestion.models.json.StartSessionLogFactory;
import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.UUIDUtils;

/**
 * Analytics feature.
 */
public class Analytics extends AbstractAvalancheFeature {

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
     * @param properties optional properties.
     */
    public static void trackPage(@NonNull String name, @Nullable Map<String, String> properties) {
        getInstance().queuePage(name, properties);
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
    public synchronized void onChannelReady(@NonNull AvalancheChannel channel) {
        super.onChannelReady(channel);
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
            mSessionTracker = null;
        }
    }

    /**
     * Send log to channel.
     *
     * @param log log to send.
     */
    private synchronized void send(Log log) {
        if (mChannel == null)
            AvalancheLog.error("Analytics feature not initialized, discarding calls.");
        else
            mChannel.enqueue(log, ANALYTICS_GROUP);
    }

    /**
     * Send a page.
     *
     * @param name       page name.
     * @param properties optional properties.
     */
    private void queuePage(@NonNull String name, @Nullable Map<String, String> properties) {
        if (!isInstanceEnabled())
            return;
        PageLog pageLog = new PageLog();
        pageLog.setName(name);
        pageLog.setProperties(properties);
        send(pageLog);
    }

    /**
     * Send an event.
     *
     * @param name       event name.
     * @param properties optional properties.
     */
    private void queueEvent(@NonNull String name, @Nullable Map<String, String> properties) {
        if (!isInstanceEnabled())
            return;
        EventLog eventLog = new EventLog();
        eventLog.setId(UUIDUtils.randomUUID());
        eventLog.setName(name);
        eventLog.setProperties(properties);
        send(eventLog);
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
