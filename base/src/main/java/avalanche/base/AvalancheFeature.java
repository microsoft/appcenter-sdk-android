package avalanche.base;

import android.app.Application;
import android.content.Context;

import java.util.Map;

import avalanche.base.ingestion.models.json.LogFactory;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.utils.NetworkStateHelper;

public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

    String getName();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    Map<String, LogFactory> getLogFactories();

    /**
     * TODO replace dependencies by the channel object once it will be implemented.
     */
    void onChannelReady(Context context, String appKey, LogSerializer logSerializer, NetworkStateHelper networkStateHelper);
}
