package com.microsoft.azure.mobile;

import android.support.annotation.VisibleForTesting;

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

    @VisibleForTesting
    static final int MAX_PROPERTIES_COUNT = 60;

    private static final int MAX_PROPERTY_ITEM_LENGTH = 128;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private static final String VALUE_NULL_ERROR_MESSAGE = "Custom property value cannot be null, did you mean to call clear?";

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
        if (isValidKey(key) && isValidStringValue(key, value)) {
            addProperty(key, value);
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
                addProperty(key, value);
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
                addProperty(key, value);
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
            addProperty(key, value);
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
            addProperty(key, null);
        }
        return this;
    }

    private void addProperty(String key, Object value) {
        if (mProperties.containsKey(key) || mProperties.size() < MAX_PROPERTIES_COUNT) {
            mProperties.put(key, value);
        } else {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Custom properties cannot contain more than " + MAX_PROPERTIES_COUNT + " items");
        }
    }

    private boolean isValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Custom property \""+ key + "\" must match \"" + KEY_PATTERN + "\"");
            return false;
        }
        if (key.length() > MAX_PROPERTY_ITEM_LENGTH) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Custom property \""+ key + "\" length cannot be longer than " + MAX_PROPERTY_ITEM_LENGTH + " characters.");
            return false;
        }
        if (mProperties.containsKey(key)) {
            MobileCenterLog.warn(MobileCenter.LOG_TAG, "Custom property \"" + key + "\" is already set or cleared and will be overridden.");
        }
        return true;
    }

    private boolean isValidStringValue(String key, String value) {
        if (value == null) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            return false;
        }
        if (value.length() > MAX_PROPERTY_ITEM_LENGTH) {
            MobileCenterLog.error(MobileCenter.LOG_TAG, "Custom property \""+ key + "\" value length cannot be longer than " + MAX_PROPERTY_ITEM_LENGTH + " characters.");
            return false;
        }
        return true;
    }
}
