/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

import java.util.Map;

/**
 * Service specification.
 */
@SuppressWarnings("WeakerAccess")
public interface MobileCenterService extends Application.ActivityLifecycleCallbacks {

    /**
     * Check whether this service is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    boolean isInstanceEnabled();

    /**
     * Enable or disable this service.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    void setInstanceEnabled(boolean enabled);

    /**
     * Factories for logs sent by this service.
     *
     * @return log factories.
     */
    @Nullable
    Map<String, LogFactory> getLogFactories();

    /**
     * Called when the channel is ready to be used. This is called even when the service is disabled.
     *
     * @param context application context.
     * @param channel  channel.
     */
    void onChannelReady(@NonNull Context context, @NonNull Channel channel);
}
