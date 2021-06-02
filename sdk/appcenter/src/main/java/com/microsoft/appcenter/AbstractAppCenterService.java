/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import java.util.Map;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_COUNT;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_INTERVAL;
import static com.microsoft.appcenter.Constants.DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
import static com.microsoft.appcenter.utils.PrefStorageConstants.KEY_ENABLED;

public abstract class AbstractAppCenterService implements AppCenterService {

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
    private AppCenterHandler mHandler;

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
    public void onApplicationEnterForeground() {
    }

    @Override
    public void onApplicationEnterBackground() {
    }

    /**
     * Help implementing static isEnabled() for services with future.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     */
    protected synchronized AppCenterFuture<Boolean> isInstanceEnabledAsync() {
        final DefaultAppCenterFuture<Boolean> future = new DefaultAppCenterFuture<>();
        postAsyncGetter(new Runnable() {

            @Override
            public void run() {
                future.complete(true);
            }
        }, future, false);
        return future;
    }

    /**
     * Help implementing static setEnabled() for services with future.
     *
     * @param enabled true to enable, false to disable.
     * @return future with null result to monitor when the operation completes.
     */
    protected final synchronized AppCenterFuture<Void> setInstanceEnabledAsync(final boolean enabled) {

        /*
         * We need to execute this while the service is disabled to enable it again,
         * but not if core disabled... Hence the parameters in post.
         */
        final DefaultAppCenterFuture<Void> future = new DefaultAppCenterFuture<>();
        final Runnable coreDisabledRunnable = new Runnable() {

            @Override
            public void run() {
                AppCenterLog.error(LOG_TAG, "App Center SDK is disabled.");
                future.complete(null);
            }
        };
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                setInstanceEnabled(enabled);
                future.complete(null);
            }
        };
        if (!post(runnable, coreDisabledRunnable, runnable)) {
            future.complete(null);
        }
        return future;
    }

    @Override
    public synchronized boolean isInstanceEnabled() {
        return SharedPreferencesManager.getBoolean(getEnabledPreferenceKey(), true);
    }

    @WorkerThread
    @Override
    public synchronized void setInstanceEnabled(boolean enabled) {

        /* Nothing to do if state does not change. */
        if (enabled == isInstanceEnabled()) {
            AppCenterLog.info(getLoggerTag(), String.format("%s service has already been %s.", getServiceName(), enabled ? "enabled" : "disabled"));
            return;
        }

        /* Initialize channel group. */
        String groupName = getGroupName();
        if (mChannel != null && groupName != null) {

            /* Register service to channel on enabling. */
            if (enabled) {
                mChannel.addGroup(groupName, getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), null, getChannelListener());
            }

            /* Otherwise, clear all persisted logs and remove a group for the service. */
            else {
                mChannel.clear(groupName);
                mChannel.removeGroup(groupName);
            }
        }

        /* Save new state. */
        SharedPreferencesManager.putBoolean(getEnabledPreferenceKey(), enabled);
        AppCenterLog.info(getLoggerTag(), String.format("%s service has been %s.", getServiceName(), enabled ? "enabled" : "disabled"));

        /* Don't call it before the service starts. */
        if (mChannel != null) {

            /* Allow sub-class to handle state change. */
            applyEnabledState(enabled);
        }
    }

    @WorkerThread
    protected synchronized void applyEnabledState(boolean enabled) {

        /* Optional callback to react to enabled state change. */
    }

    @Override
    public boolean isAppSecretRequired() {
        return true;
    }

    @Override
    public final synchronized void onStarting(@NonNull AppCenterHandler handler) {

        /*
         * The method is final just to avoid a sub-class start using the handler now,
         * it is not supported and could cause a null pointer exception.
         */
        mHandler = handler;
    }

    @WorkerThread
    @Override
    public synchronized void onStarted(@NonNull Context context, @NonNull Channel channel, String appSecret, String transmissionTargetToken, boolean startedFromApp) {
        String groupName = getGroupName();
        boolean enabled = isInstanceEnabled();
        if (groupName != null) {
            channel.removeGroup(groupName);

            /* Add a group to the channel if the service is enabled */
            if (enabled) {
                channel.addGroup(groupName, getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), null, getChannelListener());
            }

            /* Otherwise, clear all persisted logs for the service. */
            else {
                channel.clear(groupName);
            }
        }
        mChannel = channel;
        applyEnabledState(enabled);
    }

    @Override
    public void onConfigurationUpdated(String appSecret, String transmissionTargetToken) {

        /* Nothing to do. */
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
    @SuppressWarnings({"WeakerAccess", "SameReturnValue"})
    protected long getTriggerInterval() {
        return DEFAULT_TRIGGER_INTERVAL;
    }

    /**
     * Gets a maximum number of requests being sent for the group.
     *
     * @return A maximum number of requests.
     */
    @SuppressWarnings("SameReturnValue")
    protected int getTriggerMaxParallelRequests() {
        return DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
    }

    /**
     * Gets a listener which will be called when channel completes synchronization.
     *
     * @return A listener for channel
     */
    protected Channel.GroupListener getChannelListener() {
        return null;
    }

    /**
     * Post a command in background.
     *
     * @param runnable command.
     */
    protected synchronized void post(Runnable runnable) {
        post(runnable, null, null);
    }

    /**
     * Post a command in background.
     *
     * @param runnable                command.
     * @param coreDisabledRunnable    optional alternate command if core is disabled.
     * @param serviceDisabledRunnable optional alternate command if this service is disabled.
     * @return false if core not configured (no handler ready yet), true otherwise.
     */
    protected synchronized boolean post(final Runnable runnable, final Runnable coreDisabledRunnable, final Runnable serviceDisabledRunnable) {
        if (mHandler == null) {
            AppCenterLog.error(LOG_TAG, getServiceName() + " needs to be started before it can be used.");
            return false;
        } else {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    if (isInstanceEnabled()) {
                        runnable.run();
                    } else if (serviceDisabledRunnable != null) {
                        serviceDisabledRunnable.run();
                    } else {
                        AppCenterLog.info(LOG_TAG, getServiceName() + " service disabled, discarding calls.");
                    }
                }
            }, coreDisabledRunnable);
            return true;
        }
    }

    /**
     * Helper method to handle getter methods in services.
     *
     * @param runnable                    command to run if service is enabled.
     * @param future                      future to complete the result of the async operation.
     * @param valueIfDisabledOrNotStarted value to use for the future async operation result if service is disabled or not started or App Center not started.
     * @param <T>                         getter value type.
     */
    protected synchronized <T> void postAsyncGetter(final Runnable runnable, final DefaultAppCenterFuture<T> future, final T valueIfDisabledOrNotStarted) {
        Runnable disabledOrNotStartedRunnable = new Runnable() {

            @Override
            public void run() {

                /* Same runnable is used whether App Center or the service is disabled or not started. */
                future.complete(valueIfDisabledOrNotStarted);
            }
        };
        if (!post(new Runnable() {

            @Override
            public void run() {
                runnable.run();
            }
        }, disabledOrNotStartedRunnable, disabledOrNotStartedRunnable)) {

            /* App Center is not configured if we reach this. */
            disabledOrNotStartedRunnable.run();
        }
    }
}
