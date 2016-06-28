package avalanche.base.ingestion.models.json;

import org.json.JSONException;

import avalanche.base.ingestion.models.LogContainer;

public interface LogContainerSerializer {

    String serialize(LogContainer container) throws JSONException;

    LogContainer deserialize(String json) throws JSONException;

    void addLogFactory(String logType, LogFactory logFactory);
}
