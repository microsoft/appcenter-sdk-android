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
    Map<String, Object> getProperties() {
        return this.properties;
    }

    public CustomProperties put(String key, String value) {
        if (isValidKey(key)) {
            if (value != null) {
                properties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, "Value cannot be null");
            }
        }
        return this;
    }

    public CustomProperties put(String key, Date value) {
        if (isValidKey(key)) {
            if (value != null) {
                properties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, "Value cannot be null");
            }
        }
        return this;
    }

    public CustomProperties put(String key, Number value) {
        if (isValidKey(key)) {
            if (value != null) {
                properties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, "Value cannot be null");
            }
        }
        return this;
    }

    public CustomProperties put(String key, boolean value) {
        if (isValidKey(key)) {
            properties.put(key, value);
        }
        return this;
    }

    public CustomProperties clear(String key) {
        if (isValidKey(key)) {

            /* Null value means that key marked to clear. */
            properties.put(key, null);
        }
        return this;
    }

    private boolean isValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Invalid key: " + key);
            return false;
        }
        if (properties.containsKey(key)) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Key \"" + key + "\" is already put/clear and will be replaced");
        }
        return true;
    }
}
