package com.microsoft.sonoma.core;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.microsoft.sonoma.core.channel.Channel;
import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;

import java.util.Map;

/**
 * Feature specification.
 */
@SuppressWarnings("WeakerAccess")
public interface SonomaFeature extends Application.ActivityLifecycleCallbacks {

    /**
     * Check whether this feature is enabled.
     *
     * @return true if enabled, false otherwise.
     */
    boolean isInstanceEnabled();

    /**
     * Enable or disable this feature.
     *
     * @param enabled true to enable, false to disable.
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
