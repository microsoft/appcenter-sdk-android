package avalanche.base.ingestion.models.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.utils.LogUtils;

public class DefaultLogContainerSerializer implements LogContainerSerializer {

    private static final String APP_ID = "appId";

    private static final String INSTALL_ID = "installId";

    private static final String LOGS = "logs";

    private final Map<String, LogFactory> mLogFactories = new HashMap<>();

    public DefaultLogContainerSerializer() {
        addLogFactory(DeviceLog.TYPE, new DeviceLogFactory());
    }

    @Override
    public String serialize(LogContainer logContainer) throws JSONException {
        try {
            LogUtils.checkNotNull(APP_ID, logContainer.getAppId());
            LogUtils.checkNotNull(INSTALL_ID, logContainer.getInstallId());
            LogUtils.checkNotNull(LOGS, logContainer.getLogs());
        } catch (IllegalArgumentException e) {
            throw new JSONException(e.getMessage());
        }
        JSONStringer writer = new JSONStringer();
        writer.object();
        writer.key(APP_ID).value(logContainer.getAppId());
        writer.key(INSTALL_ID).value(logContainer.getInstallId());
        writer.key(LOGS).array();
        for (Log log : logContainer.getLogs()) {
            writer.object();
            log.write(writer);
            try {
                log.validate();
            } catch (IllegalArgumentException e) {
                throw new JSONException(e.getMessage());
            }
            writer.endObject();
        }
        writer.endArray();
        writer.endObject();
        return writer.toString();
    }

    @Override
    public LogContainer deserialize(String json) throws JSONException {
        JSONTokener reader = new JSONTokener(json);
        JSONObject jContainer = (JSONObject) reader.nextValue();
        LogContainer container = new LogContainer();
        container.setAppId(jContainer.getString(APP_ID));
        container.setInstallId(jContainer.getString(INSTALL_ID));
        JSONArray jLogs = jContainer.getJSONArray(LOGS);
        List<Log> logs = new ArrayList<>();
        for (int i = 0; i < jLogs.length(); i++) {
            JSONObject jLog = jLogs.getJSONObject(i);
            String type = jLog.getString(Log.TYPE);
            Log log = mLogFactories.get(type).create();
            log.read(jLog);
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
