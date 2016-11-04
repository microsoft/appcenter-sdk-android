package com.microsoft.azure.mobile.crashes.ingestion.models.json;

import com.microsoft.azure.mobile.crashes.ingestion.models.ManagedErrorLog;
import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;

public class ManagedErrorLogFactory implements LogFactory {

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
