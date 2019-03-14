/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models.one.json;

import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class CommonSchemaEventLogFactory extends AbstractLogFactory {

    @Override
    public CommonSchemaEventLog create() {
        return new CommonSchemaEventLog();
    }
}
