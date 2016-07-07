package avalanche.analytics;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import avalanche.analytics.ingestion.models.EndSessionLog;
import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.json.EndSessionLogFactory;
import avalanche.analytics.ingestion.models.json.EventLogFactory;
import avalanche.analytics.ingestion.models.json.PageLogFactory;
import avalanche.base.AbstractAvalancheFeature;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.AvalancheIngestionNetworkStateHandler;
import avalanche.base.ingestion.http.AvalancheIngestionRetryer;
import avalanche.base.ingestion.http.DefaultUrlConnectionFactory;
import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.LogFactory;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.DeviceInfoHelper;
import avalanche.base.utils.NetworkStateHelper;

public class Analytics extends AbstractAvalancheFeature {

    private static Analytics sharedInstance = null;

    private final UUID mInstallId = UUID.randomUUID();

    private UUID mAppKey;

    private UUID mSid;

    private Context mContext;

    private AvalancheIngestionNetworkStateHandler mIngestionClient;

    private Analytics() {
    }

    public static Analytics getInstance() {
        if (sharedInstance == null) {
            sharedInstance = new Analytics();
        }
        return sharedInstance;
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
    public void onChannelReady(Context context, String appKey, LogSerializer logSerializer, NetworkStateHelper networkStateHelper) {
        mContext = context;
        mAppKey = UUID.fromString(appKey);
        AvalancheIngestionHttp api = new AvalancheIngestionHttp();
        api.setUrlConnectionFactory(new DefaultUrlConnectionFactory());
        api.setLogSerializer(logSerializer);
        api.setBaseUrl("https://jsonplaceholder.typicode.com");
        AvalancheIngestionRetryer retryer = new AvalancheIngestionRetryer(api);
        mIngestionClient = new AvalancheIngestionNetworkStateHandler(retryer, networkStateHelper);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        sendPage(activity.getLocalClassName(), null);
    }

    public void sendPage(String name, Map<String, String> properties) {
        PageLog pageLog = new PageLog();
        pageLog.setName(name);
        pageLog.setProperties(properties);
        send(pageLog);
    }

    private void send(Log log) {
        if (mAppKey == null)
            return;
        List<Log> logs = new ArrayList<>();
        if (mSid == null) {
            mSid = UUID.randomUUID();
            try {
                DeviceLog deviceLog = DeviceInfoHelper.getDeviceLog(mContext);
                deviceLog.setSid(mSid);
                logs.add(deviceLog);
            } catch (DeviceInfoHelper.DeviceInfoException e) {
                AvalancheLog.error("Device log cannot be generated", e);
                return;
            }
        }
        log.setSid(mSid);
        logs.add(log);
        LogContainer logContainer = new LogContainer();
        logContainer.setLogs(logs);
        mIngestionClient.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {

            @Override
            public void success() {
                AvalancheLog.info("Could send log");
            }

            @Override
            public void failure(Throwable t) {
                AvalancheLog.error("Could not send log", t);
            }
        });
    }
}
