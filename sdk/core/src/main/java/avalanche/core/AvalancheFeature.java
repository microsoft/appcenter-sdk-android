package avalanche.core;

import android.app.Application;

import java.util.Map;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.ingestion.models.json.LogFactory;

public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Map<String, LogFactory> getLogFactories();

    void onChannelReady(AvalancheChannel channel);
}
