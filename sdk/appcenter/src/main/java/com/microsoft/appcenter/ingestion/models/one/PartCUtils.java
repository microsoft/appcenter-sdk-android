package com.microsoft.appcenter.ingestion.models.one;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * Populate Part C properties.
 */
public class PartCUtils {

    /**
     * Adds part C properties to a log.
     *
     * @param properties custom properties.
     * @param dest       destination common schema log.
     * @throws IllegalArgumentException if properties are not valid.
     */
    public static void addPartCFromLog(Map<String, String> properties, CommonSchemaLog dest) {
        if (properties == null) {
            return;
        }
        try {

            /* Part C creates properties in a deep structure using dot as an object separator. */
            Data data = new Data();
            dest.setData(data);
            for (Map.Entry<String, String> entry : properties.entrySet()) {

                /* Validate key not null. */
                String key = entry.getKey();
                if (key == null) {
                    throw new IllegalArgumentException("Property key cannot be null.");
                }

                /* Validate value not null. */
                String value = entry.getValue();
                if (value == null) {
                    throw new IllegalArgumentException("Property value cannot be null.");
                }

                /* Validate key is not Part B. */
                if (Data.BASE_DATA.equals(key) || Data.BASE_DATA_TYPE.equals(key)) {
                    throw new IllegalArgumentException("Property key '" + key + "' is reserved.");
                }

                /* Split property name by dot. */
                String[] keys = key.split("\\.", -1);
                int lastIndex = keys.length - 1;
                JSONObject destProperties = data.getProperties();
                for (int i = 0; i < lastIndex; i++) {
                    JSONObject subObject = new JSONObject();
                    destProperties.put(keys[i], subObject);
                    destProperties = subObject;
                }
                destProperties.put(keys[lastIndex], value);
            }
        } catch (JSONException ignore) {

            /* Can only happen with NaN or Infinite but our values are String. */
        }
    }
}
