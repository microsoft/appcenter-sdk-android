package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import java.util.Collection;

public interface LogFactory {

    Log create();

    Collection<CommonSchemaLog> toCommonSchemaLogs(Log log);
}
