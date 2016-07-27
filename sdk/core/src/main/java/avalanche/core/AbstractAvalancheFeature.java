package avalanche.core;

import android.app.Activity;
import android.os.Bundle;

import java.util.Map;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;

public abstract class AbstractAvalancheFeature implements AvalancheFeature {

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
    protected AvalancheChannel mChannel;

    /**
     * Flag that indicates the feature is enabled or not.
     */
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
        mChannel.addGroup(getGroupName(), getTriggerCount(), getTriggerInterval(), getTriggerMaxParallelRequests(), getChannelListener());
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
    protected int getTriggerInterval() {
        return DEFAULT_TRIGGER_INTERVAL;
    }

    /**
     * Gets a maximum number of requests being sent for the group.
     *
     * @return A maximum number of requests.
     */
    protected int getTriggerMaxParallelRequests() {
        return DEFAULT_TRIGGER_MAX_PARALLEL_REQUESTS;
    }

    /**
     * Gets a listener which will be called when channel completes synchronization.
     *
     * @return A listener for channel
     */
    protected AvalancheChannel.Listener getChannelListener() {
        return null;
    }
}
