package com.microsoft.appcenter.analytics.ingestion.models.json.one;

import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;

import java.util.Collection;
import java.util.LinkedList;

public class CommonSchemaEventLogFactory implements LogFactory {

    @Override
    public CommonSchemaEventLog create() {
        return new CommonSchemaEventLog();
    }

    @Override
    public Collection<CommonSchemaLog> toCommonSchemaLogs(Log log) {
        Collection<CommonSchemaLog> commonSchemaLogs = new LinkedList<>();
        for (String transmissionTarget : log.getTransmissionTargetTokens()) {
            CommonSchemaEventLog commonSchemaEventLog = new CommonSchemaEventLog();
            PartAUtils.addPartAFromLog(log, commonSchemaEventLog, transmissionTarget);
            commonSchemaLogs.add(commonSchemaEventLog);
        }
        return commonSchemaLogs;
    }
}
