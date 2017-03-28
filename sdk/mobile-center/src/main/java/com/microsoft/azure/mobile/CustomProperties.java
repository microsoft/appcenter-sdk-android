package com.microsoft.azure.mobile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CustomProperties {

    /**
     * Additional key/value pair parameters.
     *
     * Note: null value here represent key to clear. This is just temp data representation.
     */
    private Map<String, Object> properties = new HashMap<>();

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public CustomProperties put(String key, String value) {
        return this;
    }

    public CustomProperties put(String key, Date value) {
        return this;
    }

    public CustomProperties put(String key, Number value) {
        return this;
    }

    public CustomProperties put(String key, Boolean value) {
        return this;
    }

    public CustomProperties put(Map<String, Object> properties) {
        return this;
    }

    public CustomProperties clear(String key) {
        return this;
    }

    public void send() {
        MobileCenter.setCustomProperties(this);
    }

    public void reset() {
        properties = new HashMap<>();
    }
}
