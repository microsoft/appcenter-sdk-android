package avalanche.core.ingestion.models.json;

import android.support.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.ingestion.models.StartSessionLog;
import avalanche.core.utils.AvalancheLog;

import static avalanche.core.ingestion.models.CommonProperties.TYPE;

public class DefaultLogSerializer implements LogSerializer {

    private static final String LOGS = "logs";

    private final Map<String, LogFactory> mLogFactories;

    public DefaultLogSerializer() {
        mLogFactories = new HashMap<>();
        mLogFactories.put(StartSessionLog.TYPE, new StartSessionLogFactory());
    }

    private JSONStringer writeLog(JSONStringer writer, Log log) throws JSONException {
        writer.object();
        log.write(writer);
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
    public String serializeLog(@NonNull Log log) throws JSONException {
        return writeLog(new JSONStringer(), log).toString();
    }

    @Override
    public Log deserializeLog(@NonNull String json) throws JSONException {
        return readLog(new JSONObject(json));
    }

    @Override
    public String serializeContainer(@NonNull LogContainer logContainer) throws JSONException {

        /* Init JSON serializer, in debug/verbose: try to make it pretty. */
        JSONStringer writer = null;
        if (AvalancheLog.getLogLevel() <= android.util.Log.VERBOSE) {
            try {
                Constructor<JSONStringer> constructor = JSONStringer.class.getDeclaredConstructor(int.class);
                constructor.setAccessible(true);
                writer = constructor.newInstance(2);
            } catch (Exception e) {
                AvalancheLog.error("Failed to setup pretty json, falling back to default one", e);
            }
        }
        if (writer == null)
            writer = new JSONStringer();

        /* Start writing JSON. */
        writer.object();
        writer.key(LOGS).array();
        for (Log log : logContainer.getLogs())
            writeLog(writer, log);
        writer.endArray();
        writer.endObject();
        return writer.toString();
    }

    @Override
    public LogContainer deserializeContainer(@NonNull String json) throws JSONException {
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
    public void addLogFactory(@NonNull String logType, @NonNull LogFactory logFactory) {
        mLogFactories.put(logType, logFactory);
    }
}
