package avalanche.core;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;

/**
 * Feature specification.
 */
@SuppressWarnings("WeakerAccess")
public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

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
     * @param channel channel.
     */
    void onChannelReady(@NonNull AvalancheChannel channel);
}
