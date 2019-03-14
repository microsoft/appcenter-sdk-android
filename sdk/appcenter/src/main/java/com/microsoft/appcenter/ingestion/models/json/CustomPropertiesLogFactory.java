/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.CustomPropertiesLog;
import com.microsoft.appcenter.ingestion.models.Log;

public class CustomPropertiesLogFactory extends AbstractLogFactory {

    @Override
    public Log create() {
        return new CustomPropertiesLog();
    }
}

