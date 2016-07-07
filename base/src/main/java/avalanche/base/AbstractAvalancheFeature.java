package avalanche.base;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.utils.NetworkStateHelper;

public abstract class AbstractAvalancheFeature implements AvalancheFeature {

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
    public void onChannelReady(Context context, String appKey, LogSerializer logSerializer, NetworkStateHelper networkStateHelper) {
    }
}
