package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

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
    public static void addPartCFromLog(List<TypedProperty> properties, CommonSchemaLog dest) {
        if (properties == null) {
            return;
        }
        try {

            /* Part C creates properties in a deep structure using dot as an object separator. */
            Data data = new Data();
            dest.setData(data);
            for (TypedProperty property : properties) {

                /* Validate key not null. */
                String key = property.getName();
                if (key == null) {
                    AppCenterLog.warn(LOG_TAG, "Property key cannot be null.");
                    continue;
                }

                /* Validate key is not Part B. */
                if (Data.BASE_DATA.equals(key) || Data.BASE_DATA_TYPE.equals(key)) {
                    AppCenterLog.warn(LOG_TAG, "Property key '" + key + "' is reserved.");
                    continue;
                }

                /* Get value from property. */
                Object value;
                if (property instanceof StringTypedProperty) {
                    value = ((StringTypedProperty) property).getValue();
                } else if (property instanceof LongTypedProperty) {
                    value = ((LongTypedProperty) property).getValue();
                } else if (property instanceof DoubleTypedProperty) {
                    value = ((DoubleTypedProperty) property).getValue();
                } else if (property instanceof DateTimeTypedProperty) {
                    value = ((DateTimeTypedProperty) property).getValue();
                } else if (property instanceof BooleanTypedProperty) {
                    value = ((BooleanTypedProperty) property).getValue();
                } else {
                    value = null;
                }

                /* Validate value not null. */
                if (value == null) {
                    AppCenterLog.warn(LOG_TAG, "Value of property with key '" + key + "' cannot be null.");
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
