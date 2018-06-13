package com.microsoft.appcenter.crashes.ingestion.models.json;

import com.microsoft.appcenter.crashes.ingestion.models.HandledErrorLog;
import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;

public class HandledErrorLogFactory extends AbstractLogFactory {

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
