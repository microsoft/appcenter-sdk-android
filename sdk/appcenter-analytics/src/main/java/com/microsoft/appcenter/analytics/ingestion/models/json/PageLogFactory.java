/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class PageLogFactory extends AbstractLogFactory {

    @Override
    public PageLog create() {
        return new PageLog();
    }
}
