package com.microsoft.azure.mobile.ingestion.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.Map;

/**
 * The custom properties log model.
 */
public class CustomPropertiesLog extends AbstractLog {

    /**
     * Log type.
     */
    public static final String TYPE = "start_service";

    private static final String PROPERTIES = "properties";

    /**
     * Additional key/value pair parameters.
     */
    private Map<String, Object> properties;

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
        //setProperties(JSONUtils.readMap(object, PROPERTIES));
    }

    @Override
    public void write(JSONStringer writer) throws JSONException {
        super.write(writer);
        //JSONUtils.writeMap(writer, PROPERTIES, getProperties());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

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
