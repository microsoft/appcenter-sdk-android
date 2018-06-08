package com.microsoft.appcenter.push.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.json.AbstractLogFactory;
import com.microsoft.appcenter.push.ingestion.models.PushInstallationLog;

public class PushInstallationLogFactory extends AbstractLogFactory {

    @Override
    public PushInstallationLog create() {
        return new PushInstallationLog();
    }
}

