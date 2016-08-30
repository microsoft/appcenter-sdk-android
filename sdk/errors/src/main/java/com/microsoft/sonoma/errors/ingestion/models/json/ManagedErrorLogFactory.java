package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.errors.ingestion.models.ManagedErrorLog;

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
