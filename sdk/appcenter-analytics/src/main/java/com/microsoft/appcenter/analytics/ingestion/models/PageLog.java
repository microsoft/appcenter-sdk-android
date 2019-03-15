/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
