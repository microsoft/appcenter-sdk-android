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

                /* Validate key is not Part B. */
                String key = entry.getKey();
                if (Data.BASE_DATA.equals(key) || Data.BASE_DATA_TYPE.equals(key)) {
                    AppCenterLog.warn(LOG_TAG, "Cannot use '" + key + "' in properties, skipping that property.");
                    continue;
                }

                /* TODO validate properties here, skip invalid ones and log a warning. */

                /* Split property name by dot. */
                String[] keys = key.split("\\.");
                int lastIndex = keys.length - 1;
                JSONObject destProperties = data.getProperties();
                for (int i = 0; i < lastIndex; i++) {
                    JSONObject subObject = new JSONObject();
                    destProperties.put(keys[i], subObject);
                    destProperties = subObject;
                }
                destProperties.put(keys[lastIndex], entry.getValue());
            }
        } catch (JSONException ignore) {

            /* Can only happen with NaN or Infinite but our values are String. */
        }
    }
}
