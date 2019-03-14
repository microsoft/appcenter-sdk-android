/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import java.util.Collection;
import java.util.Collections;

public abstract class AbstractLogFactory implements LogFactory {

    @Override
    public Collection<CommonSchemaLog> toCommonSchemaLogs(Log log) {
        return Collections.emptyList();
    }
}

