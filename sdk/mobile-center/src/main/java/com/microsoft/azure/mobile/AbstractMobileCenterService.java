package com.microsoft.azure.mobile;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.utils.HandlerUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.async.DefaultSimpleFuture;
import com.microsoft.azure.mobile.utils.async.SimpleFuture;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import java.util.Map;

import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_COUNT;
import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_INTERVAL;
import static com.microsoft.azure.mobile.Constants.DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
import static com.microsoft.azure.mobile.MobileCenter.LOG_TAG;
import static com.microsoft.azure.mobile.utils.PrefStorageConstants.KEY_ENABLED;

public abstract class AbstractMobileCenterService implements MobileCenterService {

    /**
     * Separator for preference key.
     */
    private static final String PREFERENCE_KEY_SEPARATOR = "_";

    /**
     * Channel instance.
     */
    protected Channel mChannel;

    /**
     * Background thread handler.
     */
    private MobileCenterHandler mHandler;

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

    /**
     * Help implementing static isEnabled() for services with future.
     */
    protected synchronized SimpleFuture<Boolean> isInstanceEnabledAsync() {
        final DefaultSimpleFuture<Boolean> future = new DefaultSimpleFuture<>();
        if (!post(new Runnable() {

            @Override
            public void run() {
                future.complete(isInstanceEnabled());
            }
        }, true)) {
            future.complete(false);
        }
        return future;
    }

    /**
     * Help implementing static setEnabled() for services with future.
     */
    protected final synchronized void setInstanceEnabledAsync(final boolean enabled) {
        post(new Runnable() {

            @Override
            public void run() {
                setInstanceEnabled(enabled);
            }
        });
    }

    @Override
    public synchronized boolean isInstanceEnabled() {
        return StorageHelper.PreferencesStorage.getBoolean(getEnabledPreferenceKey(), true);
    }

    @Override
    public synchronized void setInstanceEnabled(final boolean enabled) {

        /* Nothing to do if state does not change. */
        if (enabled == isInstanceEnabled()) {
            MobileCenterLog.info(getLoggerTag(), String.format("%s service has already been %s.", getServiceName(), enabled ? "enabled" : "disabled"));
            return;
        }

        /* Initialize channel group. */
        String groupName = getGroupName();
        if (groupName != null) {

            /* Register service to channel on enabling. */
            if (enabled) {
                mChannel.addGroup(groupName, getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), getChannelListener());
            }

            /* Otherwise, clear all persisted logs and remove a group for the service. */
            else {
                mChannel.clear(groupName);
                mChannel.removeGroup(groupName);
            }
        }

        /* Save new state. */
        StorageHelper.PreferencesStorage.putBoolean(getEnabledPreferenceKey(), enabled);
        MobileCenterLog.info(getLoggerTag(), String.format("%s service has been %s.", getServiceName(), enabled ? "enabled" : "disabled"));

        /* Allow sub-class to handle state change in U.I. thread. */
        HandlerUtils.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                applyEnabledState(enabled);
            }
        });
    }

    @UiThread
    protected abstract void applyEnabledState(boolean enabled);

    @Override
    public void onStarting(@NonNull MobileCenterHandler handler) {
        mHandler = handler;
    }

    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull String appSecret, @NonNull Channel channel) {
        String groupName = getGroupName();
        boolean enabled = isInstanceEnabled();
        if (groupName != null) {
            channel.removeGroup(groupName);

            /* Add a group to the channel if the service is enabled */
            if (enabled)
                channel.addGroup(groupName, getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), getChannelListener());

            /* Otherwise, clear all persisted logs for the service. */
            else
                channel.clear(groupName);
        }
        mChannel = channel;
        if (enabled) {
            applyEnabledState(true);
        }
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return null;
    }

    /**
     * Gets a name of group for the service.
     *
     * @return The group name.
     */
    protected abstract String getGroupName();

    /**
     * Gets a tag of the logger.
     *
     * @return The tag of the logger.
     */
    protected abstract String getLoggerTag();

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected String getEnabledPreferenceKey() {
        return KEY_ENABLED + PREFERENCE_KEY_SEPARATOR + getServiceName();
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
     * Post a command in background.
     *
     * @param runnable command.
     */
    protected synchronized void post(Runnable runnable) {
        post(runnable, false);
    }

    /**
     * Post a command in background.
     *
     * @param runnable        command.
     * @param runWhenDisabled whether to run the command when disabled.
     */
    private synchronized boolean post(Runnable runnable, boolean runWhenDisabled) {
        if (mHandler == null) {
            MobileCenterLog.error(LOG_TAG, getServiceName() + " needs to be started before it can be used.");
            return false;
        } else {
            mHandler.post(runnable, runWhenDisabled);
            return true;
        }
    }
}
