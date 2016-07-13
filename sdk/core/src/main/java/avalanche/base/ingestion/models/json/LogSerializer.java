package avalanche.base.ingestion.models.json;

import org.json.JSONException;

import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;

public interface LogSerializer {

    String serializeLog(Log log) throws JSONException;

    Log deserializeLog(String json) throws JSONException;

    String serializeContainer(LogContainer container) throws JSONException;

    LogContainer deserializeContainer(String json) throws JSONException;

    void addLogFactory(String logType, LogFactory logFactory);
}
