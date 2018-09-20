package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Populate Part C properties.
 */
public class PartCUtils {

    /**
     * Adds part C properties to a log.
     *
     * @param properties custom properties.
     * @param dest       destination common schema log.
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
                    AppCenterLog.warn(LOG_TAG, "Property key cannot be null.");
                    continue;
                }

                /* Validate value not null. */
                String value = entry.getValue();
                if (value == null) {
                    AppCenterLog.warn(LOG_TAG, "Value of property with key '" + key + "' cannot be null.");
                    continue;
                }

                /* Validate key is not Part B. */
                if (Data.BASE_DATA.equals(key) || Data.BASE_DATA_TYPE.equals(key)) {
                    AppCenterLog.warn(LOG_TAG, "Property key '" + key + "' is reserved.");
                    continue;
                }

                /* Split property name by dot. */
                String[] keys = key.split("\\.", -1);
                int lastIndex = keys.length - 1;
                JSONObject destProperties = data.getProperties();
                for (int i = 0; i < lastIndex; i++) {
                    JSONObject subObject = destProperties.optJSONObject(keys[i]);
                    if (subObject == null) {
                        if (destProperties.has(keys[i])) {
                            AppCenterLog.warn(LOG_TAG, "Property key '" + keys[i] + "' already has a value, the old value will be overridden.");
                        }
                        subObject = new JSONObject();
                        destProperties.put(keys[i], subObject);
                    }
                    destProperties = subObject;
                }
                if (destProperties.has(keys[lastIndex])) {
                    AppCenterLog.warn(LOG_TAG, "Property key '" + keys[lastIndex] + "' already has a value, the old value will be overridden.");
                }
                destProperties.put(keys[lastIndex], value);
            }
        } catch (JSONException ignore) {

            /* Can only happen with NaN or Infinite but our values are String. */
        }
    }
}
