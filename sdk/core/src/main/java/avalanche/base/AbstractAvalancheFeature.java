package avalanche.base;

import android.app.Activity;
import android.os.Bundle;

import java.util.Map;

import avalanche.base.channel.AvalancheChannel;
import avalanche.base.ingestion.models.json.LogFactory;

public abstract class AbstractAvalancheFeature implements AvalancheFeature {

    protected AvalancheChannel mChannel;

    private boolean mEnabled = true;

    @Override
    public String getName() {
        return getClass().getName();
    }

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
    public boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public void onChannelReady(AvalancheChannel channel) {
        mChannel = channel;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        return null;
    }
}
