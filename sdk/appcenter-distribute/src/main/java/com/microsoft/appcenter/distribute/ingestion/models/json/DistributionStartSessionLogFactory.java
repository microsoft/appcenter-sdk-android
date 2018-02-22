package com.microsoft.appcenter.distribute.ingestion.models.json;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;

public class DistributionStartSessionLogFactory implements LogFactory {

    @Override
    public DistributionStartSessionLog create() {
        return new DistributionStartSessionLog();
    }
}
