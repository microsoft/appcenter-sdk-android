/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The custom properties log model.
 */
public class CustomPropertiesLog extends AbstractLog {

    /**
     * Log type.
     */
    public static final String TYPE = "customProperties";

    private static final String PROPERTIES = "properties";

    private static final String PROPERTY_TYPE = "type";

    private static final String PROPERTY_NAME = "name";

    private static final String PROPERTY_VALUE = "value";

    private static final String PROPERTY_TYPE_CLEAR = "clear";

    private static final String PROPERTY_TYPE_BOOLEAN = "boolean";

    private static final String PROPERTY_TYPE_NUMBER = "number";

    private static final String PROPERTY_TYPE_DATETIME = "dateTime";

    private static final String PROPERTY_TYPE_STRING = "string";

    /**
     * Properties key/value pairs.
     */
    private Map<String, Object> properties;

    private static Map<String, Object> readProperties(JSONObject object) throws JSONException {
        JSONArray jArray = object.getJSONArray(PROPERTIES);
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < jArray.length(); i++) {
            JSONObject jProperty = jArray.getJSONObject(i);
            String key = jProperty.getString(PROPERTY_NAME);
            Object value = readPropertyValue(jProperty);
            properties.put(key, value);
        }
        return properties;
    }

    @SuppressWarnings("IfCanBeSwitch")
    private static Object readPropertyValue(JSONObject object) throws JSONException {
        String type = object.getString(PROPERTY_TYPE);
        Object value;
        if (type.equals(PROPERTY_TYPE_CLEAR)) {
            value = null;
        } else if (type.equals(PROPERTY_TYPE_BOOLEAN)) {
            value = object.getBoolean(PROPERTY_VALUE);
        } else if (type.equals(PROPERTY_TYPE_NUMBER)) {
            value = object.get(PROPERTY_VALUE);
            if (!(value instanceof Number)) {
                throw new JSONException("Invalid value type");
            }
        } else if (type.equals(PROPERTY_TYPE_DATETIME)) {
            value = JSONDateUtils.toDate(object.getString(PROPERTY_VALUE));
        } else if (type.equals(PROPERTY_TYPE_STRING)) {
            value = object.getString(PROPERTY_VALUE);
        } else {
            throw new JSONException("Invalid value type");
        }
        return value;
    }

    private static void writeProperties(JSONStringer writer, Map<String, Object> properties) throws JSONException {
        if (properties != null) {
            writer.key(PROPERTIES).array();
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                writer.object();
                JSONUtils.write(writer, PROPERTY_NAME, property.getKey());
                writePropertyValue(writer, property.getValue());
                writer.endObject();
            }
            writer.endArray();
        } else {
            throw new JSONException("Properties cannot be null");
        }
    }

    private static void writePropertyValue(JSONStringer writer, Object value) throws JSONException {
        if (value == null) {
            JSONUtils.write(writer, PROPERTY_TYPE, PROPERTY_TYPE_CLEAR);
        } else if (value instanceof Boolean) {
            JSONUtils.write(writer, PROPERTY_TYPE, PROPERTY_TYPE_BOOLEAN);
            JSONUtils.write(writer, PROPERTY_VALUE, value);
        } else if (value instanceof Number) {
            JSONUtils.write(writer, PROPERTY_TYPE, PROPERTY_TYPE_NUMBER);
            JSONUtils.write(writer, PROPERTY_VALUE, value);
        } else if (value instanceof Date) {
            JSONUtils.write(writer, PROPERTY_TYPE, PROPERTY_TYPE_DATETIME);
            JSONUtils.write(writer, PROPERTY_VALUE, JSONDateUtils.toString((Date) value));
        } else if (value instanceof String) {
            JSONUtils.write(writer, PROPERTY_TYPE, PROPERTY_TYPE_STRING);
            JSONUtils.write(writer, PROPERTY_VALUE, value);
        } else {
            throw new JSONException("Invalid value type");
        }
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setProperties(readProperties(object));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        writeProperties(writer, getProperties());
    }

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        CustomPropertiesLog that = (CustomPropertiesLog) o;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
