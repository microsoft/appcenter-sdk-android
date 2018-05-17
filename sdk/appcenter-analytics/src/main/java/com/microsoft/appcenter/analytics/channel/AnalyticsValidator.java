package com.microsoft.appcenter.analytics.channel;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.microsoft.appcenter.analytics.ingestion.models.LogWithNameAndProperties;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.utils.AppCenterLog;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.analytics.Analytics.LOG_TAG;

public class AnalyticsValidator extends AbstractChannelListener {

    /**
     * Max length of event/page name.
     */
    @VisibleForTesting
    static final int MAX_NAME_LENGTH = 256;

    /**
     * Max length of properties.
     */
    @VisibleForTesting
    static final int MAX_PROPERTY_ITEM_LENGTH = 125;

    /**
     * Max number of properties.
     */
    @VisibleForTesting
    static final int MAX_PROPERTY_COUNT = 20;

    @Override
    public boolean shouldFilter(@NonNull Log log) {
        //noinspection SimplifiableIfStatement
        if (log instanceof EventLog || log instanceof PageLog) {
            return !validateLog((LogWithNameAndProperties) log);
        }
        return false;
    }

    /**
     * Validates log.
     *
     * @param log   The log.
     * @return true if validation passed, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean validateLog(@NonNull LogWithNameAndProperties log) {
        String name = validateName(log.getName(), log.getType());
        if (name == null) {
            return false;
        }
        Map<String, String> validatedProperties = validateProperties(log.getProperties(), name, log.getType());
        log.setName(name);
        log.setProperties(validatedProperties);
        return true;
    }

    /**
     * Validates name.
     *
     * @param name    Log name to validate.
     * @param logType Log type.
     * @return <code>null</code> if validation failed, otherwise a valid name within the length limit will be returned.
     */
    private static String validateName(String name, String logType) {
        if (name == null || name.isEmpty()) {
            AppCenterLog.error(LOG_TAG, logType + " name cannot be null or empty.");
            return null;
        }
        if (name.length() > MAX_NAME_LENGTH) {
            AppCenterLog.warn(LOG_TAG, String.format("%s '%s' : name length cannot be longer than %s characters. Name will be truncated.", logType, name, MAX_NAME_LENGTH));
            name = name.substring(0, MAX_NAME_LENGTH);
        }
        return name;
    }

    /**
     * Validates properties.
     *
     * @param properties Properties collection to validate.
     * @param logName    Log name.
     * @param logType    Log type.
     * @return valid properties collection with maximum size of 20.
     */
    private static Map<String, String> validateProperties(Map<String, String> properties, String logName, String logType) {
        if (properties == null) {
            return null;
        }
        String message;
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            String key = property.getKey();
            String value = property.getValue();
            if (result.size() >= MAX_PROPERTY_COUNT) {
                message = String.format("%s '%s' : properties cannot contain more than %s items. Skipping other properties.", logType, logName, MAX_PROPERTY_COUNT);
                AppCenterLog.warn(LOG_TAG, message);
                break;
            }
            if (key == null || key.isEmpty()) {
                message = String.format("%s '%s' : a property key cannot be null or empty. Property will be skipped.", logType, logName);
                AppCenterLog.warn(LOG_TAG, message);
                continue;
            }
            if (value == null) {
                message = String.format("%s '%s' : property '%s' : property value cannot be null. Property '%s' will be skipped.", logType, logName, key, key);
                AppCenterLog.warn(LOG_TAG, message);
                continue;
            }
            if (key.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s '%s' : property '%s' : property key length cannot be longer than %s characters. Property key will be truncated.", logType, logName, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(LOG_TAG, message);
                key = key.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            if (value.length() > MAX_PROPERTY_ITEM_LENGTH) {
                message = String.format("%s '%s' : property '%s' : property value cannot be longer than %s characters. Property value will be truncated.", logType, logName, key, MAX_PROPERTY_ITEM_LENGTH);
                AppCenterLog.warn(LOG_TAG, message);
                value = value.substring(0, MAX_PROPERTY_ITEM_LENGTH);
            }
            result.put(key, value);
        }
        return result;
    }
}
