/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.microsoft.appcenter.AppCenter.LOG_TAG;

/**
 * Custom properties builder.
 * Collect multiple properties for send its at once in the same log.
 */
public class CustomProperties {

    @VisibleForTesting
    static final int MAX_PROPERTIES_COUNT = 60;

    @VisibleForTesting
    static final int MAX_PROPERTY_KEY_LENGTH = 128;

    private static final int MAX_PROPERTY_VALUE_LENGTH = 128;

    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]*$");

    private static final String VALUE_NULL_ERROR_MESSAGE = "Custom property value cannot be null, did you mean to call clear?";

    /**
     * Properties key/value pairs.
     * Null value means that key marked to clear.
     */
    private final Map<String, Object> mProperties = new HashMap<>();

    /**
     * Get the property values as a copy.
     *
     * @return the property values.
     */
    synchronized Map<String, Object> getProperties() {
        return new HashMap<>(mProperties);
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
    public synchronized CustomProperties set(String key, String value) {
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
    public synchronized CustomProperties set(String key, Date value) {
        if (isValidKey(key)) {
            if (value != null) {
                addProperty(key, value);
            } else {
                AppCenterLog.error(LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
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
    public synchronized CustomProperties set(String key, Number value) {
        if (isValidKey(key) && isValidNumberValue(key, value)) {
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
    public synchronized CustomProperties set(String key, boolean value) {
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
    public synchronized CustomProperties clear(String key) {
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
            AppCenterLog.error(LOG_TAG, "Custom properties cannot contain more than " + MAX_PROPERTIES_COUNT + " items");
        }
    }

    private boolean isValidKey(String key) {
        if (key == null || !KEY_PATTERN.matcher(key).matches()) {
            AppCenterLog.error(LOG_TAG, "Custom property \"" + key + "\" must match \"" + KEY_PATTERN + "\"");
            return false;
        }
        if (key.length() > MAX_PROPERTY_KEY_LENGTH) {
            AppCenterLog.error(LOG_TAG, "Custom property \"" + key + "\" length cannot be longer than " + MAX_PROPERTY_KEY_LENGTH + " characters.");
            return false;
        }
        if (mProperties.containsKey(key)) {
            AppCenterLog.warn(LOG_TAG, "Custom property \"" + key + "\" is already set or cleared and will be overridden.");
        }
        return true;
    }

    private boolean isValidStringValue(String key, String value) {
        if (value == null) {
            AppCenterLog.error(LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            return false;
        }
        if (value.length() > MAX_PROPERTY_VALUE_LENGTH) {
            AppCenterLog.error(LOG_TAG, "Custom property \"" + key + "\" value length cannot be longer than " + MAX_PROPERTY_VALUE_LENGTH + " characters.");
            return false;
        }
        return true;
    }

    private boolean isValidNumberValue(String key, Number value) {
        if (value == null) {
            AppCenterLog.error(LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            return false;
        }
        double doubleValue = value.doubleValue();
        if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
            AppCenterLog.error(LOG_TAG, "Custom property \"" + key + "\" value cannot be NaN or infinite.");
            return false;
        }
        return true;
    }
}
