package com.microsoft.sonoma.crashes.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.crashes.ingestion.models.ManagedErrorLog;

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
