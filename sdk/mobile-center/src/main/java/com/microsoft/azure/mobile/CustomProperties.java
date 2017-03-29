package com.microsoft.azure.mobile;

import com.microsoft.azure.mobile.utils.MobileCenterLog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Custom properties builder.
 * Collect multiple properties for send its at once in the same log.
 */
@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class CustomProperties {

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private static final String VALUE_NULL_ERROR_MESSAGE = "Value cannot be null, did you mean to call clear?";

    /**
     * Properties key/value pairs.
     * Null value means that key marked to clear.
     */
    private final Map<String, Object> mProperties = new HashMap<>();

    /**
     * Get the properties value.
     *
     * @return the properties value
     */
    Map<String, Object> getProperties() {
        return mProperties;
    }

    /**
     * Set the specified property value with the specified key.
     * If the properties previously contained a property for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be set.
     * @param value value to be set with the specified key.
     * @return this instance.
     */
    public CustomProperties set(String key, String value) {
        if (isValidKey(key)) {
            if (value != null) {
                mProperties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            }
        }
        return this;
    }

    /**
     * Set the specified property value with the specified key.
     * If the properties previously contained a property for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be set.
     * @param value value to be set with the specified key.
     * @return this instance.
     */
    public CustomProperties set(String key, Date value) {
        if (isValidKey(key)) {
            if (value != null) {
                mProperties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            }
        }
        return this;
    }

    /**
     * Set the specified property value with the specified key.
     * If the properties previously contained a property for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be set.
     * @param value value to be set with the specified key.
     * @return this instance.
     */
    public CustomProperties set(String key, Number value) {
        if (isValidKey(key)) {
            if (value != null) {
                mProperties.put(key, value);
            } else {
                MobileCenterLog.error(MobileCenter.LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            }
        }
        return this;
    }

    /**
     * Set the specified property value with the specified key.
     * If the properties previously contained a property for the key, the old
     * value is replaced.
     *
     * @param key   key with which the specified value is to be set.
     * @param value value to be set with the specified key.
     * @return this instance.
     */
    public CustomProperties set(String key, boolean value) {
        if (isValidKey(key)) {
            mProperties.put(key, value);
        }
        return this;
    }

    /**
     * Clear the property for the specified key.
     *
     * @param key key whose mapping is to be cleared.
     * @return this instance.
     */
    public CustomProperties clear(String key) {
        if (isValidKey(key)) {

            /* Null value means that key marked to clear. */
            mProperties.put(key, null);
        }
        return this;
    }

    private boolean isValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Invalid key: " + key);
            return false;
        }
        if (mProperties.containsKey(key)) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Key \"" + key + "\" is already set or cleared and will be overridden.");
        }
        return true;
    }
}
