package avalanche.base.ingestion.models.json;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import android.util.JsonReader;
import android.util.JsonWriter;

import avalanche.base.ingestion.models.LogContainer;

public class DefaultLogContainerSerializer {

    public static final String APP_ID = "appId";

    public static final String INSTALL_ID = "installId";

    public static final String LOGS = "logs";

    public String serialize(LogContainer logContainer) throws IOException {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);
        writer.beginObject();
        writer.name(APP_ID).value(logContainer.getAppId());
        writer.name(INSTALL_ID).value(logContainer.getInstallId());
        writer.name(LOGS).beginArray();
//        List<Log> logs = logContainer.getLogs();
//        if (logs != null)
//            for (Log log : logs) {
//            }
        writer.endArray();
        writer.endObject();
        return out.toString();
    }

    public LogContainer deserialize(String json) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(json));
        reader.beginObject();
        LogContainer container = new LogContainer();
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case APP_ID:
                    container.setAppId(reader.nextString());
                    break;

                case INSTALL_ID:
                    container.setInstallId(reader.nextString());
                    break;

                default:
                    reader.skipValue();
            }
        }
        reader.endObject();
        return container;
    }
}
