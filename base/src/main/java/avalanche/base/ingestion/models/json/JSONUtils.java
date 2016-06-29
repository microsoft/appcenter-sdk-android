package avalanche.base.ingestion.models.json;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import avalanche.base.ingestion.models.Model;

public final class JSONUtils {

    private JSONUtils() {
    }

    public static Integer readInteger(JSONObject object, String key) throws JSONException {
        if (object.has(key))
            return object.getInt(key);
        return null;
    }

    public static Long readLong(JSONObject object, String key) throws JSONException {
        if (object.has(key))
            return object.getLong(key);
        return null;
    }

    public static Boolean readBoolean(JSONObject object, String key) throws JSONException {
        if (object.has(key))
            return object.getBoolean(key);
        return null;
    }

    public static Map<String, String> readMap(JSONObject object, String key) throws JSONException {
        JSONObject jProperties = object.optJSONObject(key);
        if (jProperties == null)
            return null;
        Map<String, String> properties = new HashMap<>(jProperties.length());
        Iterator<String> subKeys = jProperties.keys();
        while (subKeys.hasNext()) {
            String subKey = subKeys.next();
            properties.put(subKey, jProperties.getString(subKey));
        }
        return properties;
    }

    public static <M extends Model> List<M> readArray(JSONObject object, String key, ModelFactory<M> factory) throws JSONException {
        JSONArray jArray = object.optJSONArray(key);
        if (jArray == null)
            return null;
        List<M> array = factory.createList(jArray.length());
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject jModel = jArray.getJSONObject(i);
            M model = factory.create();
            array.add(model);
        }
        return array;
    }

    public static void write(JSONStringer writer, String key, Object value, boolean required) throws JSONException {
        if (value != null || required)
            writer.key(key).value(value);
    }

    public static void writeMap(JSONStringer writer, String key, Map<String, String> value) throws JSONException {
        if (value != null) {
            writer.key(key).object();
            for (Map.Entry<String, String> property : value.entrySet())
                writer.key(property.getKey()).value(property.getValue());
            writer.endObject();
        }
    }

    public static void writeArray(JSONStringer writer, String key, List<? extends Model> value) throws JSONException {
        if (value != null) {
            writer.key(key).array();
            for (Model model : value) {
                writer.object();
                model.write(writer);
                writer.endObject();
            }
            writer.endArray();
        }
    }
}
