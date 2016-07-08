package avalanche.base.channel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import avalanche.base.ingestion.AvalancheIngestion;
import avalanche.base.ingestion.ServiceCallback;
import avalanche.base.ingestion.http.AvalancheIngestionHttp;
import avalanche.base.ingestion.http.AvalancheIngestionNetworkStateHandler;
import avalanche.base.ingestion.http.AvalancheIngestionRetryer;
import avalanche.base.ingestion.http.DefaultUrlConnectionFactory;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.LogSerializer;
import avalanche.base.utils.IdHelper;
import avalanche.base.utils.NetworkStateHelper;

// FIXME temporary class that directly send logs while fixing bugs on actual channel
public class DirectAvalancheChannel implements AvalancheChannel {

    private final UUID mAppKey;

    private final UUID mInstallId;

    private final AvalancheIngestion mIngestion;

    /**
     * Creates and initializes a new instance.
     */
    public DirectAvalancheChannel(@NonNull Context context, @NonNull UUID appKey, @NonNull LogSerializer logSerializer) {
        mAppKey = appKey;
        mInstallId = IdHelper.getInstallId();
        AvalancheIngestionHttp api = new AvalancheIngestionHttp();
        api.setUrlConnectionFactory(new DefaultUrlConnectionFactory());
        api.setLogSerializer(logSerializer);
        api.setBaseUrl("http://avalanche-perf.westus.cloudapp.azure.com:8081"); //TODO make that a parameter
        AvalancheIngestionRetryer retryer = new AvalancheIngestionRetryer(api);
        mIngestion = new AvalancheIngestionNetworkStateHandler(retryer, NetworkStateHelper.getSharedInstance(context));
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef String queueName) {
        LogContainer logContainer = new LogContainer();
        ArrayList<Log> logs = new ArrayList<>();
        logs.add(log);
        logContainer.setLogs(logs);
        mIngestion.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {

            @Override
            public void success() {
            }

            @Override
            public void failure(Throwable t) {
            }
        });
    }
}
