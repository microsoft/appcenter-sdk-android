package com.microsoft.appcenter.analytics.ingestion.models;

/**
 * Page log.
 */
public class PageLog extends LogWithNameAndProperties {

    public static final String TYPE = "page";

    @Override
    public String getType() {
        return TYPE;
    }
}
