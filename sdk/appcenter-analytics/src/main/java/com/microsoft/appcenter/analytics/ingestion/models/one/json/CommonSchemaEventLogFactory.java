package com.microsoft.appcenter.analytics.ingestion.models.one.json;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Conversion of analytics logs to Common Schema.
 */
public class CommonSchemaEventLogFactory implements LogFactory {

    @Override
    public CommonSchemaEventLog create() {
        return new CommonSchemaEventLog();
    }

    @Override
    public Collection<CommonSchemaLog> toCommonSchemaLogs(Log log) {

        /* Convert events only for now. */
        if (log instanceof EventLog) {

            /* 1 App Center log can map to multiple common schema logs if multiple keys. */
            Collection<CommonSchemaLog> commonSchemaLogs = new LinkedList<>();
            for (String transmissionTarget : log.getTransmissionTargetTokens()) {

                /* Part A common fields. */
                CommonSchemaEventLog commonSchemaEventLog = new CommonSchemaEventLog();
                PartAUtils.addPartAFromLog(log, commonSchemaEventLog, transmissionTarget);

                /* Event name goes to Part A too. */
                EventLog eventLog = (EventLog) log;
                commonSchemaEventLog.setName(eventLog.getName());

                /* TODO properties go to Part C. */
                commonSchemaLogs.add(commonSchemaEventLog);
            }
            return commonSchemaLogs;
        }
        return Collections.emptyList();
    }
}
