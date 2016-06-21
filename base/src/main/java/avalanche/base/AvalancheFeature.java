package avalanche.base;

import android.app.Application;

public interface AvalancheFeature extends Application.ActivityLifecycleCallbacks {

    String getName();

}
