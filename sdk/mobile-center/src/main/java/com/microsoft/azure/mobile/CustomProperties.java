package com.microsoft.azure.mobile;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CustomProperties {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    /**
     * Additional key/value pair parameters.
     * Null value means that key marked to clear.
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

    public CustomProperties put(String key, Object value) {
        if (key == null || !isValidKey(key)) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Invalid key: " + key);
            return this;
        } else if (properties.containsKey(key)) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Key \"" + key + "\" is already put/clear and will be replaced");
        }
        if (value == null) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Value cannot be null");
        } else if (isSupportValueType(value)) {
            properties.put(key, value);
        } else {

            /* Fallback to string */
            String stringValue = value.toString();
            if (stringValue != null) {
                properties.put(key, stringValue);
            }
        }
        return this;
    }

    public CustomProperties put(Map<String, Object> properties) {
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties.entrySet()) {
                put(property.getKey(), property.getValue());
            }
        }
        return this;
    }

    public CustomProperties clear(String key) {
        if (key == null || !isValidKey(key)) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Invalid key: " + key);
            return this;
        } else if (properties.containsKey(key)) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Key \"" + key + "\" is already put/clear and will be replaced");
        }

        /* Null value means that key marked to clear. */
        properties.put(key, null);
        return this;
    }

    public void send() {
        MobileCenter.setCustomProperties(this);
    }

    public void reset() {
        properties = new HashMap<>();
    }

    private static boolean isValidKey(String key) {
        return KEY_PATTERN.matcher(key).matches();
    }

    private static boolean isSupportValueType(Object value) {
        return value instanceof Boolean ||
                value instanceof Number ||
                value instanceof Date ||
                value instanceof String;
    }
}
