package avalanche.analytics;

import android.app.Activity;

import java.util.HashMap;
import java.util.Map;

import avalanche.analytics.ingestion.models.EndSessionLog;
import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.json.EndSessionLogFactory;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.base.AbstractAvalancheFeature;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.json.LogFactory;

import static avalanche.base.channel.DefaultAvalancheChannel.ANALYTICS_GROUP;

public class Analytics extends AbstractAvalancheFeature {

    private static Analytics sharedInstance = null;

    protected Analytics() {
    }

    public static Analytics getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Analytics();
        }
        return sharedInstance;
    }

    private static String getDefaultPageName(Class<?> activityClass) {
        String name = activityClass.getSimpleName();
        String suffix = "Activity";
        if (name.endsWith(suffix) && name.length() > suffix.length())
            return name.substring(0, name.length() - suffix.length());
        else
            return name;
    }

    @Override
    public Map<String, LogFactory> getLogFactories() {
        HashMap<String, LogFactory> factories = new HashMap<>();
        factories.put(EndSessionLog.TYPE, new EndSessionLogFactory());
        factories.put(PageLog.TYPE, new PageLogFactory());
        factories.put(EventLog.TYPE, new EventLogFactory());
        return factories;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        sendPage(getDefaultPageName(activity.getClass()), null);
    }

    public void sendPage(String name, Map<String, String> properties) {
        PageLog pageLog = new PageLog();
        pageLog.setName(name);
        pageLog.setProperties(properties);
        send(pageLog);
    }

    public void sendEvent(String name, Map<String, String> properties) {
        EventLog eventLog = new EventLog();
        eventLog.setName(name);
        eventLog.setProperties(properties);
        send(eventLog);
    }

    private void send(Log pageLog) {
        mChannel.enqueue(pageLog, ANALYTICS_GROUP);
    }
}
