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

import java.util.Iterator;
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

                /* Validate property and get type. */
                Object value;
                try {
                    value = validateProperty(property);
                } catch (IllegalArgumentException e) {
                    AppCenterLog.warn(LOG_TAG, e.getMessage());
                    continue;
                }

                /* Split property name by dot. */
                String[] keys = property.getName().split("\\.", -1);

                /* Handle all intermediate keys. */
                addDataWithMetadata(data.getProperties(), metadata.getMetadata(), keys, value, getMetadataType(property), 0);
            }

            /* Add metadata extension only if not empty after cleanup. */
            if (!cleanUpEmptyObjectsInMetadata(metadata.getMetadata())) {
                if (dest.getExt() == null) {
                    dest.setExt(new Extensions());
                }
                dest.getExt().setMetadata(metadata);
            }
        } catch (JSONException ignore) {

            /* Can only happen with NaN or Infinite but this is already checked before. */
        }
    }

    /**
     * Validate typed property.
     *
     * @param property typed property.
     * @return property value.
     * @throws IllegalArgumentException if the property is invalid.
     * @throws JSONException            if JSON date formatting fails (never happens).
     */
    private static Object validateProperty(TypedProperty property) throws IllegalArgumentException, JSONException {

        /* Validate key not null. */
        String key = property.getName();
        if (key == null) {
            throw new IllegalArgumentException("Property key cannot be null.");
        }

        /* Validate key is not Part B. */
        if (Data.BASE_DATA.equals(key) || Data.BASE_DATA_TYPE.equals(key)) {
            throw new IllegalArgumentException("Property key '" + key + "' is reserved.");
        }

        /* Get value from property. */
        Object value;
        if (property instanceof StringTypedProperty) {
            StringTypedProperty stringTypedProperty = (StringTypedProperty) property;
            value = stringTypedProperty.getValue();
        } else if (property instanceof LongTypedProperty) {
            LongTypedProperty longTypedProperty = (LongTypedProperty) property;
            value = longTypedProperty.getValue();
        } else if (property instanceof DoubleTypedProperty) {
            DoubleTypedProperty doubleTypedProperty = (DoubleTypedProperty) property;
            value = doubleTypedProperty.getValue();
        } else if (property instanceof DateTimeTypedProperty) {
            value = JSONDateUtils.toString(((DateTimeTypedProperty) property).getValue());
        } else if (property instanceof BooleanTypedProperty) {
            BooleanTypedProperty booleanTypedProperty = (BooleanTypedProperty) property;
            value = booleanTypedProperty.getValue();
        } else {
            throw new IllegalArgumentException("Unsupported property type: " + property.getType());
        }

        /* Validate value not null. */
        if (value == null) {
            throw new IllegalArgumentException("Value of property with key '" + key + "' cannot be null.");
        }
        return value;
    }

    /**
     * Get metadata type for the specified value.
     *
     * @param property property to check type.
     * @return metadata type or null if the type is a default one.
     */
    private static Integer getMetadataType(TypedProperty property) {
        if (property instanceof LongTypedProperty) {
            return DATA_TYPE_INT64;
        }
        if (property instanceof DoubleTypedProperty) {
            return DATA_TYPE_DOUBLE;
        }
        if (property instanceof DateTimeTypedProperty) {
            return DATA_TYPE_DATETIME;
        }
        return null;
    }

    /**
     * Add a key and a value to data properties with metadata.
     *
     * @param destProperties the parent data properties object.
     * @param destMetadata   the parent metadata object.
     * @param keys           the dot-splitted keys
     * @param value          the value for the given keys.
     * @param metadataType   metadata type.
     * @param index          the index for the parent key in <code>keys</code>.
     * @throws JSONException if it fails to add the key and value to data property and metadata.
     */
    private static void addDataWithMetadata(JSONObject destProperties, JSONObject destMetadata, String[] keys, Object value, Integer metadataType, int index) throws JSONException {
        if (index == keys.length - 1) {
            if (destProperties.has(keys[index])) {
                AppCenterLog.warn(LOG_TAG, "Property key '" + keys[index] + "' already has a value, the old value will be overridden.");
            }

            /* Add a child object for the key and value. */
            destProperties.put(keys[index], value);

            /* Add a leaf metadata for the key. */
            addLeafMetadata(metadataType, destMetadata, keys[index]);
            return;
        }

        /* Get a child property of the key. */
        JSONObject childProperties = destProperties.optJSONObject(keys[index]);
        if (childProperties == null) {
            childProperties = new JSONObject();
            destProperties.put(keys[index], childProperties);
        }
        destProperties = childProperties;

        /* Add an intermediateMetadata for the key. */
        destMetadata = addIntermediateMetadata(destMetadata, keys[index]);

        /* Add metadata for its children. */
        addDataWithMetadata(destProperties, destMetadata, keys, value, metadataType, index + 1);
    }

    /**
     * Add the last level of metadata.
     *
     * @param metadataType metadata type.
     * @param destMetadata the parent metadata object.
     * @param lastKey      the last key from the dot split.
     * @throws JSONException if JSON put fails.
     */
    private static void addLeafMetadata(Integer metadataType, JSONObject destMetadata, String lastKey) throws JSONException {
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
        }
    }

    /**
     * Add a level of metadata nesting or return the existing intermediate object.
     *
     * @param destMetadata the parent metadata object.
     * @param subKey       the intermediate key from the dot split.
     * @return metadata object on next level.
     * @throws JSONException if JSON put fails.
     */
    private static JSONObject addIntermediateMetadata(JSONObject destMetadata, String subKey) throws JSONException {
        JSONObject fields = destMetadata.optJSONObject(METADATA_FIELDS);
        if (fields == null) {
            fields = new JSONObject();
            destMetadata.put(METADATA_FIELDS, fields);
        }
        JSONObject subMetadataObject = fields.optJSONObject(subKey);
        if (subMetadataObject == null) {
            subMetadataObject = new JSONObject();
            fields.put(subKey, subMetadataObject);
        }
        return subMetadataObject;
    }

    /**
     * Remove all empty children from JSON object.
     * <p>
     * For example, if a property is {"a.b.c": 3}, the metadata contains {"f": {"a": {"f": {"b": {"f": {"c":4}}}}}}.
     * When {"a.b": "a"} property overrides the metadata JSON object (since string type doesn't require metadata),
     * it would remove "b" but {"f": {"a": {"f": {}}}} remains in the metadata.
     * <p>
     * This method cleans up empty child objects in the given JSON object that were created while
     * building metadata but remained after its properties were overridden.
     *
     * @param object Parent JSON object.
     * @return true if the object has no children and safe to be removed from its parent.
     */
    private static boolean cleanUpEmptyObjectsInMetadata(JSONObject object) {
        for (Iterator<String> iterator = object.keys(); iterator.hasNext(); ) {
            String childKey = iterator.next();
            JSONObject child = object.optJSONObject(childKey);
            if (child != null) {
                if (cleanUpEmptyObjectsInMetadata(child)) {
                    iterator.remove();
                }
            }
        }
        return object.length() == 0;
    }
}
