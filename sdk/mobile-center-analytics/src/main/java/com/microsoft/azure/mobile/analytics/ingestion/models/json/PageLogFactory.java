/*
 * Copyright © Microsoft Corporation. All rights reserved.
 */

package com.microsoft.azure.mobile.analytics.ingestion.models.json;

import com.microsoft.azure.mobile.analytics.ingestion.models.PageLog;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

public class PageLogFactory implements LogFactory {

    @Override
    public PageLog create() {
        return new PageLog();
    }
}
