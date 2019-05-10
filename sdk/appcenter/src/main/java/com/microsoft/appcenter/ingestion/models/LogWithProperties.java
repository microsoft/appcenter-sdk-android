/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models;

import com.microsoft.appcenter.ingestion.models.json.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Map;

/**
 * The LogWithProperties model.
 */
public abstract class LogWithProperties extends AbstractLog {

    private static final String PROPERTIES = "properties";

    /**
     * Additional key/value pair parameters.
     */
    private Map<String, String> properties;

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * Set the properties value.
     *
     * @param properties the properties value to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public void read(JSONObject object) throws JSONException {
        super.read(object);
        setProperties(JSONUtils.readMap(object, PROPERTIES));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        JSONUtils.writeMap(writer, PROPERTIES, getProperties());
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
        LogWithProperties that = (LogWithProperties) o;
        return properties != null ? properties.equals(that.properties) : that.properties == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (properties != null ? properties.hashCode() : 0);
        return result;
    }
}
