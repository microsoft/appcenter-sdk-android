/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.sasquatch.eventfilter;

import android.support.annotation.NonNull;

import com.microsoft.appcenter.AbstractAppCenterService;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

/**
 * Fake App Center module to show how to write an event filter module.
 */
public class EventFilter extends AbstractAppCenterService {

    /**
     * Name of the service.
     */
    private static final String SERVICE_NAME = "EventFilter";

    /**
     * TAG used in logging for this service.
     */
    private static final String LOG_TAG = AppCenterLog.LOG_TAG + SERVICE_NAME;

    /**
     * Shared instance.
     */
    private static EventFilter sInstance = null;

    /**
     * Channel listener.
     */
    private AbstractChannelListener mChannelListener;

    /**
     * Get shared instance.
     *
     * @return shared instance.
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized EventFilter getInstance() {
        if (sInstance == null) {
            sInstance = new EventFilter();
        }
        return sInstance;
    }

    /**
     * Check whether EventFilter service is enabled or not.
     *
     * @return future with result being <code>true</code> if enabled, <code>false</code> otherwise.
     * @see AppCenterFuture
     */
    public static AppCenterFuture<Boolean> isEnabled() {
        return getInstance().isInstanceEnabledAsync();
    }

    /**
     * Enable or disable EventFilter service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     * @return future with null result to monitor when the operation completes.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static AppCenterFuture<Void> setEnabled(boolean enabled) {
        return getInstance().setInstanceEnabledAsync(enabled);
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
    protected String getGroupName() {

        /* Return null when this module itself don't send logs of its own. */
        return null;
    }

    @Override
    protected synchronized void applyEnabledState(boolean enabled) {

        /* Enable filtering logs when this module is enabled. */
        if (enabled) {
            mChannelListener = new AbstractChannelListener() {

                @Override
                public boolean shouldFilter(@NonNull Log log) {

                    /* Filter out events. */
                    if (log instanceof EventLog) {
                        AppCenterLog.info(LOG_TAG, "Filtered an event out.");
                        return true;
                    }
                    return false;
                }
            };
            mChannel.addListener(mChannelListener);
        }

        /* On applying disabled state, let's make sure we remove listener. */
        else if (mChannel != null) {
            mChannel.removeListener(mChannelListener);
        }
    }
}
