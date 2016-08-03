package avalanche.core;

import android.app.Application;

import java.util.Map;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;

@SuppressWarnings("WeakerAccess")
public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Map<String, LogFactory> getLogFactories();

    void onChannelReady(AvalancheChannel channel);
}
