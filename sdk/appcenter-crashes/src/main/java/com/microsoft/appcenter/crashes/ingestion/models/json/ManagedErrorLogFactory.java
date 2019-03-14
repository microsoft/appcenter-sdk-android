/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class ManagedErrorLogFactory extends AbstractLogFactory {

    private static final ManagedErrorLogFactory sInstance = new ManagedErrorLogFactory();

    private ManagedErrorLogFactory() {
    }

    public static ManagedErrorLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public ManagedErrorLog create() {
        return new ManagedErrorLog();
    }
}
