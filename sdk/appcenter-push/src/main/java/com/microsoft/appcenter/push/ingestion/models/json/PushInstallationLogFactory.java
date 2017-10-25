package com.microsoft.appcenter.push.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.push.ingestion.models.PushInstallationLog;

public class PushInstallationLogFactory implements LogFactory {

    @Override
    public PushInstallationLog create() {
        return new PushInstallationLog();
    }
}

