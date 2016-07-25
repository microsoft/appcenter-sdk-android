package avalanche.core;

import android.app.Activity;
import android.os.Bundle;

import java.util.Map;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;

public abstract class AbstractAvalancheFeature implements AvalancheFeature {

    protected AvalancheChannel mChannel;

    private boolean mEnabled = true;

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
    public synchronized boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public synchronized void setEnabled(boolean enabled) {
        mEnabled = enabled;

        /* Clear all persisted logs for the feature if the feature is disabled after the channel is ready. */
        if (!mEnabled && mChannel != null)
            mChannel.clear(getGroupName());
    }

    @Override
    public synchronized void onChannelReady(AvalancheChannel channel) {
        /* Clear all persisted logs for the feature if it wan't cleared in previous setEnabled(false) call. */
        if (!mEnabled)
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
}
