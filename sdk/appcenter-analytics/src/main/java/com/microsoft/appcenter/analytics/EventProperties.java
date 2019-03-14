/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.appcenter.analytics.Analytics.LOG_TAG;

/**
 * Event properties builder.
 */
public class EventProperties {

    private static final String VALUE_NULL_ERROR_MESSAGE = "Property value cannot be null";

    /**
     * Properties key/value pairs.
     * Using a concurrent map to avoid concurrent modification exception when doing the traversal copy.
     * We also need to traverse a snapshot of properties when doing property inheritance between targets.
     * There is no need to block in a more global way than the snapshot traversal.
     */
    private final Map<String, TypedProperty> mProperties = new ConcurrentHashMap<>();

    Map<String, TypedProperty> getProperties() {
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
    public EventProperties set(String key, boolean value) {
        if (isValidKey(key)) {
            BooleanTypedProperty property = new BooleanTypedProperty();
            property.setName(key);
            property.setValue(value);
            mProperties.put(key, property);
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
    public EventProperties set(String key, Date value) {
        if (isValidKey(key) && isValidValue(value)) {
            DateTimeTypedProperty property = new DateTimeTypedProperty();
            property.setName(key);
            property.setValue(value);
            mProperties.put(key, property);
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
    public EventProperties set(String key, double value) {
        if (isValidKey(key)) {
            if (Double.isInfinite(value) || Double.isNaN(value)) {
                AppCenterLog.error(LOG_TAG, "Double property value cannot be NaN or infinite.");
            } else {
                DoubleTypedProperty property = new DoubleTypedProperty();
                property.setName(key);
                property.setValue(value);
                mProperties.put(key, property);
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
    public EventProperties set(String key, long value) {
        if (isValidKey(key)) {
            LongTypedProperty property = new LongTypedProperty();
            property.setName(key);
            property.setValue(value);
            mProperties.put(key, property);
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
    public EventProperties set(String key, String value) {
        if (isValidKey(key) && isValidValue(value)) {
            StringTypedProperty property = new StringTypedProperty();
            property.setName(key);
            property.setValue(value);
            mProperties.put(key, property);
        }
        return this;
    }

    /**
     * Common validation for both AppCenter and One Collector, specific validation happens later.
     */
    private boolean isValidKey(String key) {
        if (key == null) {
            AppCenterLog.error(LOG_TAG, "Property key must not be null");
            return false;
        }
        if (mProperties.containsKey(key)) {
            AppCenterLog.warn(LOG_TAG, "Property \"" + key + "\" is already set and will be overridden.");
        }
        return true;
    }

    /**
     * Common validation for both AppCenter and One Collector, specific validation happens later.
     */
    private boolean isValidValue(Object value) {
        if (value == null) {
            AppCenterLog.error(LOG_TAG, VALUE_NULL_ERROR_MESSAGE);
            return false;
        }
        return true;
    }
}
