package com.microsoft.appcenter.analytics.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

/**
 * Event log.
 */
public class CommonSchemaEventLog extends CommonSchemaLog {

    /**
     * Type property.
     */
    public static final String TYPE = "commonSchemaEvent";

    @Override
    public String getType() {
        return TYPE;
    }
}
