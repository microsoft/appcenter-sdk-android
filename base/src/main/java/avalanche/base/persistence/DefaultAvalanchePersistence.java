package avalanche.base.persistence;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.List;

import avalanche.base.ingestion.models.Log;

public class DefaultAvalanchePersistence extends AvalanchePersistence {


    @Override
    public void putLog(@NonNull String key, @NonNull Log log) throws PersistenceException {

    }

    @Override
    public void deleteLog(@NonNull String key, @NonNull String id) {

    }

    @Override
    public String getLogs(@NonNull String key, @IntRange(from = 0) int limit, List<Log> outLogs) {
        return null;
    }
}
