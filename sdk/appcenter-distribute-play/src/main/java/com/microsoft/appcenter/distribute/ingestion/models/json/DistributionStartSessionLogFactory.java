/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute.ingestion.models.json;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class DistributionStartSessionLogFactory extends AbstractLogFactory {

    @Override
    public DistributionStartSessionLog create() {
        return new DistributionStartSessionLog();
    }
}
