package com.microsoft.azure.mobile;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

import java.util.Map;

/**
 * Feature specification.
 */
@SuppressWarnings("WeakerAccess")
public interface MobileCenterFeature extends Application.ActivityLifecycleCallbacks {

    /**
     * Check whether this feature is enabled or not.
     *
     * @return <code>true</code> if enabled, <code>false</code> otherwise.
     */
    boolean isInstanceEnabled();

    /**
     * Enable or disable this feature.
     *
     * @param enabled <code>true</code> to enable, <code>false</code> to disable.
     */
    void setInstanceEnabled(boolean enabled);

    /**
     * Factories for logs sent by this feature.
     *
     * @return log factories.
     */
    @Nullable
    Map<String, LogFactory> getLogFactories();

    /**
     * Called when the channel is ready to be used. This is called even when the feature is disabled.
     *
     * @param context application context.
     * @param channel  channel.
     */
    void onChannelReady(@NonNull Context context, @NonNull Channel channel);
}
