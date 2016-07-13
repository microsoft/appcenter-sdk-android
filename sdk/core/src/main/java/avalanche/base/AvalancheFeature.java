package avalanche.base;

import android.app.Application;

import java.util.Map;

import avalanche.base.channel.AvalancheChannel;
import avalanche.base.ingestion.models.json.LogFactory;

public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Map<String, LogFactory> getLogFactories();

    void onChannelReady(AvalancheChannel channel);
}
