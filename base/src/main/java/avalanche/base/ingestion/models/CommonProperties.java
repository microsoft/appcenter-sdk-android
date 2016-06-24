package avalanche.base.ingestion.models;

import org.json.JSONException;
import org.json.JSONStringer;

import java.util.Map;

public final class CommonProperties {

    public static final String TYPE = "type";

    public static final String ID = "id";

    public static final String SID = "sid";

    public static final String NAME = "name";

    private CommonProperties() {
    }

    public static void serializeMap(String key, Map<String, String> value, JSONStringer writer) throws JSONException {
        if (value != null) {
            writer.key(key).object();
            for (Map.Entry<String, String> property : value.entrySet())
                writer.key(property.getKey()).value(property.getValue());
            writer.endObject();
        }
    }
}
