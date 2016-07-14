package avalanche.core.ingestion.models.json;

import org.json.JSONException;

import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;

public interface LogSerializer {

    String serializeLog(Log log) throws JSONException;

    Log deserializeLog(String json) throws JSONException;

    String serializeContainer(LogContainer container) throws JSONException;

    LogContainer deserializeContainer(String json) throws JSONException;

    void addLogFactory(String logType, LogFactory logFactory);
}
