/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.ingestion.models;

import com.microsoft.appcenter.ingestion.models.AbstractLog;

/**
 * Distribution start session log.
 */
public class DistributionStartSessionLog extends AbstractLog {

    public static final String TYPE = "distributionStartSession";

    @Override
    public String getType() {
        return TYPE;
    }
}
