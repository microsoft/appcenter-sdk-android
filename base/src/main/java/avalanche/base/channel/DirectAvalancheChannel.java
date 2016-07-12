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
import avalanche.base.persistence.AvalancheDatabasePersistence;
import avalanche.base.persistence.AvalanchePersistence;
import avalanche.base.utils.AvalancheLog;
import avalanche.base.utils.IdHelper;
import avalanche.base.utils.NetworkStateHelper;

// FIXME temporary class that directly send logs while fixing bugs on actual channel
public class DirectAvalancheChannel implements AvalancheChannel {

    private final UUID mAppKey;

    private final UUID mInstallId;

    private final AvalancheIngestion mIngestion;

    private AvalanchePersistence mPersistence;

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
        mPersistence = new AvalancheDatabasePersistence();
        mPersistence.setLogSerializer(logSerializer);
    }

    @Override
    public void enqueue(@NonNull Log log, @NonNull @GroupNameDef final String queueName) {
        try {
            mPersistence.putLog(queueName, log);
        } catch (AvalanchePersistence.PersistenceException e) {
            AvalancheLog.error("Cannot persist logs");
            return;
        }

        int batchSize = 50;
        ArrayList<Log> logs;
        do {
            logs = new ArrayList<>();
            final String batchId = mPersistence.getLogs(queueName, batchSize, logs);

            if (logs.size() <= 0)
                break;

            LogContainer logContainer = new LogContainer();
            logs.add(log);
            logContainer.setLogs(logs);
            mIngestion.sendAsync(mAppKey, mInstallId, logContainer, new ServiceCallback() {

                @Override
                public void success() {
                    mPersistence.deleteLog(queueName, batchId);
                }

                @Override
                public void failure(Throwable t) {
                    AvalancheLog.warn("Failed to send logs to ingestion service for batchId " + batchId, t);
                }
            });
        } while (logs.size() >= batchSize);
    }
}
