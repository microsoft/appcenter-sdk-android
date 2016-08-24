package com.microsoft.sonoma.errors.ingestion.models.json;

import com.microsoft.sonoma.core.ingestion.models.json.LogFactory;
import com.microsoft.sonoma.errors.ingestion.models.JavaErrorLog;

public class JavaErrorLogFactory implements LogFactory {

    private static final JavaErrorLogFactory sInstance = new JavaErrorLogFactory();

    private JavaErrorLogFactory() {
    }

    public static JavaErrorLogFactory getInstance() {
        return sInstance;
    }

    @Override
    public JavaErrorLog create() {
        return new JavaErrorLog();
    }
}
