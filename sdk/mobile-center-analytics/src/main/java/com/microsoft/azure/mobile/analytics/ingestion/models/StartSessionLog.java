package com.microsoft.azure.mobile.analytics.ingestion.models;

import com.microsoft.azure.mobile.ingestion.models.AbstractLog;

/**
 * Start session log.
 */
public class StartSessionLog extends AbstractLog {

    public static final String TYPE = "start_session";

    @Override
    public String getType() {
        return TYPE;
    }
}
