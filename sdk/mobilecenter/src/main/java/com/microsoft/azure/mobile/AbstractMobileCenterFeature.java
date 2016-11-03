package com.microsoft.azure.mobile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.Map;

import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;
import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;

public abstract class AbstractMobileCenterFeature implements MobileCenterFeature {

    /**
     * Separator for preference key.
     */
    private static final String PREFERENCE_KEY_SEPARATOR = "_";

    /**
     * Number of metrics queue items which will trigger synchronization.
     */
    private static final int DEFAULT_TRIGGER_COUNT = 50;

    /**
     * Maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size.
     */
    private static final int DEFAULT_TRIGGER_INTERVAL = 3 * 1000;
    /**
     * Maximum number of requests being sent for the group.
     */
    private static final int DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS = 3;

    /**
     * Channel instance.
     */
    protected Channel mChannel;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {
    }

    @Override
    public void onActivityResumed(Activity activity) {
    }

    @Override
    public void onActivityPaused(Activity activity) {
    }

    @Override
    public void onActivityStopped(Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
    }

    @Override
    public synchronized boolean isInstanceEnabled() {
        return StorageHelper.PreferencesStorage.getBoolean(getEnabledPreferenceKey(), true);
    }

    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {

        /* Check if the SDK is disabled. */
        if (!MobileCenter.isEnabled() && enabled) {
            MobileCenterLog.error(LOG_TAG, "The SDK is disabled. Re-enable the SDK from the core module first before enabling a specific feature");
            return;
        }

        /* Nothing to do if state does not change. */
        else if (enabled == isInstanceEnabled())
            return;

        /* If channel initialized. */
        if (mChannel != null) {

            /* Register feature to channel on enabling. */
            if (enabled)
                mChannel.addGroup(getGroupName(), getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), getChannelListener());

            /* Otherwise, clear all persisted logs and remove a group for the feature. */
            else {
                /* TODO: Expose a method and do this in one place. */
                mChannel.clear(getGroupName());
                mChannel.removeGroup(getGroupName());
            }
        }

        /* Save new state. */
        StorageHelper.PreferencesStorage.putBoolean(getEnabledPreferenceKey(), enabled);
    }

    @Override
    public synchronized void onChannelReady(@NonNull Context context, @NonNull Channel channel) {
        channel.removeGroup(getGroupName());

        /* Add a group to the channel if the feature is enabled */
        if (isInstanceEnabled())
            channel.addGroup(getGroupName(), getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), getChannelListener());

        /* Otherwise, clear all persisted logs for the feature. */
        else
            channel.clear(getGroupName());

        mChannel = channel;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return null;
    }

    /**
     * Gets a name of group for the feature.
     *
     * @return The group name.
     */
    protected abstract String getGroupName();

    /**
     * Gets a name of the feature.
     *
     * @return The name of the feature.
     */
    protected abstract String getFeatureName();

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected String getEnabledPreferenceKey() {
        return KEY_ENABLED + PREFERENCE_KEY_SEPARATOR + getGroupName();
    }

    /**
     * Gets a number of logs which will trigger synchronization.
     *
     * @return A number of logs.
     */
    protected int getTriggerCount() {
        return DEFAULT_TRIGGER_COUNT;
    }

    /**
     * Gets a maximum time interval in milliseconds after which a synchronize will be triggered, regardless of queue size
     *
     * @return A maximum time interval in milliseconds.
     */
    @SuppressWarnings("WeakerAccess")
    protected int getTriggerInterval() {
        return DEFAULT_TRIGGER_INTERVAL;
    }

    /**
     * Gets a maximum number of requests being sent for the group.
     *
     * @return A maximum number of requests.
     */
    @SuppressWarnings("WeakerAccess")
    protected int getTriggerMaxParallelRequests() {
        return DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
    }

    /**
     * Gets a listener which will be called when channel completes synchronization.
     *
     * @return A listener for channel
     */
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    protected Channel.GroupListener getChannelListener() {
        return null;
    }

    /**
     * Check if the feature is not active: disabled or not started.
     *
     * @return <code>true</code> if the feature is inactive, <code>false</code> otherwise.
     */
    protected synchronized boolean isInactive() {
        if (mChannel == null) {
            MobileCenterLog.error(LOG_TAG, getFeatureName() + " feature not initialized, discarding calls.");
            return true;
        }
        if (!isInstanceEnabled()) {
            MobileCenterLog.info(LOG_TAG, getFeatureName() + " feature not enabled, discarding calls.");
            return true;
        }
        return false;
    }
}
