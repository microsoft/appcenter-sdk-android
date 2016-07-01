package avalanche.base.ingestion.models.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.utils.LogUtils;

import static avalanche.base.ingestion.models.CommonProperties.TYPE;

public class DefaultLogSerializer implements LogSerializer {

    private static final String LOGS = "logs";

    private final Map<String, LogFactory> mLogFactories = new HashMap<>();

    public DefaultLogSerializer() {
        addLogFactory(DeviceLog.TYPE, new DeviceLogFactory());
    }

    private JSONStringer writeLog(JSONStringer writer, Log log) throws JSONException {
        writer.object();
        log.write(writer);
        try {
            log.validate();
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
        writer.endObject();
        return writer;
    }

    private Log readLog(JSONObject object) throws JSONException {
        String type = object.getString(TYPE);
        Log log = mLogFactories.get(type).create();
        log.read(object);
        return log;
    }

    @Override
    public String serializeLog(Log log) throws JSONException {
        return writeLog(new JSONStringer(), log).toString();
    }

    @Override
    public Log deserializeLog(String json) throws JSONException {
        return readLog(new JSONObject(json));
    }

    @Override
    public String serializeContainer(LogContainer logContainer) throws JSONException {
        try {
            LogUtils.checkNotNull(LOGS, logContainer.getLogs());
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
        JSONStringer writer = new JSONStringer();
        writer.object();
        writer.key(LOGS).array();
        for (Log log : logContainer.getLogs())
            writeLog(writer, log);
        writer.endArray();
        writer.endObject();
        return writer.toString();
    }

    @Override
    public LogContainer deserializeContainer(String json) throws JSONException {
        JSONObject jContainer = new JSONObject(json);
        LogContainer container = new LogContainer();
        JSONArray jLogs = jContainer.getJSONArray(LOGS);
        List<Log> logs = new ArrayList<>();
        for (int i = 0; i < jLogs.length(); i++) {
            JSONObject jLog = jLogs.getJSONObject(i);
            Log log = readLog(jLog);
            logs.add(log);
        }
        container.setLogs(logs);
        return container;
    }

    @Override
    public void addLogFactory(String logType, LogFactory logFactory) {
        mLogFactories.put(logType, logFactory);
    }
}
