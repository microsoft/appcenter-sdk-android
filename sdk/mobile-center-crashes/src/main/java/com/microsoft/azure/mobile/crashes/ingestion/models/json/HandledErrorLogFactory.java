package com.microsoft.azure.mobile.crashes.ingestion.models.json;

import com.microsoft.azure.mobile.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

public class HandledErrorLogFactory implements LogFactory {

    private static final HandledErrorLogFactory sInstance = new HandledErrorLogFactory();

    private HandledErrorLogFactory() {
    }

    public static HandledErrorLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public HandledErrorLog create() {
        return new HandledErrorLog();
    }
}
