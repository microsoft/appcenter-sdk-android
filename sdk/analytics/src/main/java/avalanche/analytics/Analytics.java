package avalanche.analytics;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.core.AbstractAvalancheFeature;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.json.LogFactory;
import avalanche.core.utils.AvalancheLog;

import static avalanche.core.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;

/**
 * Analytics feature.
 */
public class Analytics extends AbstractAvalancheFeature {

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
     * Init.
     */
    private Analytics() {
        mFactories = new HashMap<>();
        mFactories.put(PageLog.TYPE, new PageLogFactory());
        mFactories.put(EventLog.TYPE, new EventLogFactory());
    }

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    public static Analytics getInstance() {
        if (sInstance == null) {
            sInstance = new Analytics();
        }
        return sInstance;
    }

    @VisibleForTesting
    static void unsetInstance() {
        sInstance = null;
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
    public Map<String, LogFactory> getLogFactories() {
        return mFactories;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        queuePage(generatePageName(activity.getClass()), null);
    }

    /**
     * Send log to channel.
     *
     * @param log log to send.
     */
    private void send(Log log) {
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
        if (!isEnabled())
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
        if (!isEnabled())
            return;
        EventLog eventLog = new EventLog();
        eventLog.setId(UUID.randomUUID());
        eventLog.setName(name);
        eventLog.setProperties(properties);
        send(eventLog);
    }
}
