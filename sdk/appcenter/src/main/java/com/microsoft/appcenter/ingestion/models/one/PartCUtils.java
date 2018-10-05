package com.microsoft.appcenter.ingestion.models.one;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
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

    @VisibleForTesting
    static final String METADATA_FIELDS = "f";

    @VisibleForTesting
    static final int DATA_TYPE_INT64 = 4;

    @VisibleForTesting
    static final int DATA_TYPE_DOUBLE = 6;

    @VisibleForTesting
    static final int DATA_TYPE_DATETIME = 9;

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

            /* We also build Part A metadata extension at the same time to reflect the data. */
            MetadataExtension metadata = new MetadataExtension();
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
                Object value = null;
                Integer metadataType = null;
                if (property instanceof StringTypedProperty) {
                    StringTypedProperty stringTypedProperty = (StringTypedProperty) property;
                    value = stringTypedProperty.getValue();
                } else if (property instanceof LongTypedProperty) {
                    LongTypedProperty longTypedProperty = (LongTypedProperty) property;
                    value = longTypedProperty.getValue();
                    metadataType = DATA_TYPE_INT64;
                } else if (property instanceof DoubleTypedProperty) {
                    DoubleTypedProperty doubleTypedProperty = (DoubleTypedProperty) property;
                    value = doubleTypedProperty.getValue();
                    metadataType = DATA_TYPE_DOUBLE;
                } else if (property instanceof DateTimeTypedProperty) {
                    value = JSONDateUtils.toString(((DateTimeTypedProperty) property).getValue());
                    metadataType = DATA_TYPE_DATETIME;
                } else if (property instanceof BooleanTypedProperty) {
                    BooleanTypedProperty booleanTypedProperty = (BooleanTypedProperty) property;
                    value = booleanTypedProperty.getValue();
                }

                /* Validate value not null. */
                if (value == null) {
                    AppCenterLog.warn(LOG_TAG, "Value of property with key '" + key + "' cannot be null.");
                    continue;
                }

                /* Split property name by dot. */
                String[] keys = key.split("\\.", -1);
                int lastIndex = keys.length - 1;

                /* Handle all intermediate keys. */
                JSONObject destProperties = data.getProperties();
                JSONObject destMetadata = metadata.getMetadata();
                for (int i = 0; i < lastIndex; i++) {

                    /* Add data sub object. */
                    String subKey = keys[i];
                    JSONObject subDataObject = destProperties.optJSONObject(subKey);
                    if (subDataObject == null) {
                        if (destProperties.has(subKey)) {
                            AppCenterLog.warn(LOG_TAG, "Property key '" + subKey + "' already has a value, the old value will be overridden.");
                        }

                        /* Add sub data intermediate object. */
                        subDataObject = new JSONObject();
                        destProperties.put(subKey, subDataObject);
                    }
                    destProperties = subDataObject;

                    /* Add sub metadata intermediate object if using a non default type. */
                    JSONObject fields = destMetadata.optJSONObject(METADATA_FIELDS);
                    if (metadataType != null) {
                        if (fields == null) {
                            fields = new JSONObject();
                            destMetadata.put(METADATA_FIELDS, fields);
                        }
                        JSONObject subMetadataObject = fields.optJSONObject(subKey);
                        if (subMetadataObject == null) {
                            subMetadataObject = new JSONObject();
                            fields.put(subKey, subMetadataObject);
                        }
                        destMetadata = subMetadataObject;
                    }

                    /*
                     * If overriding from metadata type in a sub object to default type in a parent object,
                     * Select sub object without creating it to be able to override metadata after the loop.
                     * Example: put "a.b.c": 2 then put "a.b": "3" will trigger that code
                     * and we need to cleanup metadata.
                     */
                    else if (fields != null) {
                        JSONObject subMetadataObject = fields.optJSONObject(subKey);
                        if (subMetadataObject != null) {
                            destMetadata = subMetadataObject;
                        }
                    }
                }

                /* Handle the last key, the leaf. */
                String lastKey = keys[lastIndex];
                if (destProperties.has(lastKey)) {
                    AppCenterLog.warn(LOG_TAG, "Property key '" + lastKey + "' already has a value, the old value will be overridden.");
                }
                destProperties.put(lastKey, value);

                /* Add metadata if not a default type. */
                JSONObject fields = destMetadata.optJSONObject(METADATA_FIELDS);
                if (metadataType != null) {
                    if (fields == null) {
                        fields = new JSONObject();
                        destMetadata.put(METADATA_FIELDS, fields);
                    }
                    fields.put(lastKey, metadataType);
                }

                /* If we override a key that needs metadata with a key that doesn't, cleanup. */
                else if (fields != null) {
                    fields.remove(lastKey);
                    if (fields.length() == 0) {
                        destMetadata.remove(METADATA_FIELDS);
                    }
                }
            }

            /* Add metadata extension only if not empty. */
            if (metadata.getMetadata().length() > 0) {
                dest.getExt().setMetadata(metadata);
            }
        } catch (JSONException ignore) {

            /* Can only happen with NaN or Infinite but our values are String. */
        }
    }
}
