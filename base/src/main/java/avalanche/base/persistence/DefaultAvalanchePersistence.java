package avalanche.base.persistence;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import avalanche.base.ingestion.models.Log;

// FIXME mock like implementation while waiting for actual implementation
public class DefaultAvalanchePersistence extends AvalanchePersistence {

    private Map<String, List<Log>> mLogs = new HashMap<>();

    @Override
    public void putLog(@NonNull String key, @NonNull Log log) throws PersistenceException {
        List<Log> logGroup = mLogs.get(key);
        if (logGroup == null) {
            logGroup = new ArrayList<>();
            mLogs.put(key, logGroup);
        }
        logGroup.add(log);
    }

    @Override
    public void deleteLog(@NonNull String key, @NonNull String id) {
        mLogs.remove(key);
    }

    @Override
    public String getLogs(@NonNull String key, @IntRange(from = 0) int limit, List<Log> outLogs) {
        List<Log> logGroup = mLogs.get(key);
        if (logGroup != null) {
            outLogs.addAll(logGroup);
        }
        return UUID.randomUUID().toString();
    }
}
