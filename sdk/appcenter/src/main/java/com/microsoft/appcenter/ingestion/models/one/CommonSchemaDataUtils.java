/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import static com.microsoft.appcenter.ingestion.models.one.Data.BASE_DATA;
import static com.microsoft.appcenter.ingestion.models.one.Data.BASE_TYPE;
import static com.microsoft.appcenter.utils.AppCenterLog.LOG_TAG;

/**
 * Populate Part B and Part C of common schema logs (the data property).
 * Also populate Part A metadata extension.
 */
public class CommonSchemaDataUtils {

    @VisibleForTesting
    static final String METADATA_FIELDS = "f";

    @VisibleForTesting
    static final int DATA_TYPE_INT64 = 4;

    @VisibleForTesting
    static final int DATA_TYPE_DOUBLE = 6;

    @VisibleForTesting
    static final int DATA_TYPE_DATETIME = 9;

    /**
     * Adds part B and part C properties to a log and Part A metadata.
     *
     * @param properties custom properties as source of data.
     * @param dest       destination common schema log.
     */
    public static void addCommonSchemaData(List<TypedProperty> properties, CommonSchemaLog dest) {
        if (properties == null) {
            return;
        }
        try {

            /* Part B and C are mixed into the same top level data property. */
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

                /* Get metadata type. */
                Integer metadataType = getMetadataType(property);

                /* Split property name by dot. */
                String key = property.getName();
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

                    /* Handle metadata. */
                    destMetadata = addIntermediateMetadata(destMetadata, subKey);
                }

                /* Handle the last key for data, the leaf. */
                String lastKey = keys[lastIndex];
                if (destProperties.has(lastKey)) {
                    AppCenterLog.warn(LOG_TAG, "Property key '" + lastKey + "' already has a value, the old value will be overridden.");
                }
                destProperties.put(lastKey, value);

                /* Handle the last key for meta-data, the leaf. */
                addLeafMetadata(metadataType, destMetadata, lastKey);
            }

            /* Warn/cleanup if baseData and baseType are not paired. */
            JSONObject dataObject = data.getProperties();
            String baseType = dataObject.optString(BASE_TYPE, null);
            JSONObject baseData = dataObject.optJSONObject(BASE_DATA);
            if (baseType == null && baseData != null) {

                /* Discard unpaired data and metadata. */
                AppCenterLog.warn(LOG_TAG, "baseData was set but baseType is missing.");
                dataObject.remove(BASE_DATA);
                JSONObject baseMetaData = metadata.getMetadata().optJSONObject(METADATA_FIELDS);

                /* baseMetaData is always non null as baseData has at least 1 sub object and not cleaned up yet if empty. */
                baseMetaData.remove(BASE_DATA);
            }
            if (baseType != null && baseData == null) {

                /* Discard unpaired base type. */
                AppCenterLog.warn(LOG_TAG, "baseType was set but baseData is missing.");
                dataObject.remove(BASE_TYPE);
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

        /* Validate baseType. */
        if (key.equals(BASE_TYPE) && !(property instanceof StringTypedProperty)) {
            throw new IllegalArgumentException("baseType must be a string.");
        }
        if (key.startsWith(BASE_TYPE + ".")) {
            throw new IllegalArgumentException("baseType must be a string.");
        }

        /* Validate baseData is an object, meaning it has at least 1 dot. */
        if (key.equals(BASE_DATA)) {
            throw new IllegalArgumentException("baseData must be an object.");
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
