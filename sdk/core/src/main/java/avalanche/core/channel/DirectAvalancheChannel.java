package avalanche.core.channel;

import android.content.Context;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.UUID;

import avalanche.core.ingestion.AvalancheIngestion;
import avalanche.core.ingestion.ServiceCallback;
import avalanche.core.ingestion.http.AvalancheIngestionHttp;
import avalanche.core.ingestion.http.AvalancheIngestionNetworkStateHandler;
import avalanche.core.ingestion.http.AvalancheIngestionRetryer;
import avalanche.core.ingestion.http.DefaultUrlConnectionFactory;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.json.LogSerializer;
import avalanche.core.persistence.AvalancheDatabasePersistence;
import avalanche.core.persistence.AvalanchePersistence;
import avalanche.core.utils.AvalancheLog;
import avalanche.core.utils.IdHelper;
import avalanche.core.utils.NetworkStateHelper;

// FIXME temporary class that directly send logs while fixing bugs on actual channel
public class DirectAvalancheChannel implements AvalancheChannel {

    private static final int BATCH_SIZE = 50;

    private final UUID mAppKey;

    private final UUID mInstallId;

    private final AvalancheIngestion mIngestion;

    private final AvalanchePersistence mPersistence;

    /**
     * Creates and initializes a new instance.
     */
    public DirectAvalancheChannel(@NonNull Context context, @NonNull UUID appKey, @NonNull LogSerializer logSerializer) {
        mAppKey = appKey;
        mInstallId = IdHelper.getInstallId();
        AvalancheIngestionHttp api = new AvalancheIngestionHttp(new DefaultUrlConnectionFactory(), logSerializer);
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
            AvalancheLog.error("Cannot persist logs", e);
            return;
        }

        ArrayList<Log> logs;
        do {
            logs = new ArrayList<>();
            final String batchId = mPersistence.getLogs(queueName, BATCH_SIZE, logs);

            if (batchId == null || logs.size() <= 0)
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
        } while (logs.size() >= BATCH_SIZE);
    }
}
