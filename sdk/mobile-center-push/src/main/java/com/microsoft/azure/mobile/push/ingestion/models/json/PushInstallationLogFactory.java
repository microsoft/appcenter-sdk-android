package com.microsoft.azure.mobile.push.ingestion.models.json;

import com.microsoft.azure.mobile.ingestion.models.json.LogFactory;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;

public class PushInstallationLogFactory implements LogFactory {

    @Override
    public PushInstallationLog create() {
        return new PushInstallationLog();
    }
}

