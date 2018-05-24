package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;

import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;

public class AbstractLogFactoryTest {

    private AbstractLogFactory mLogFactory = new AbstractLogFactory() {

        @Override
        public Log create() {
            return null;
        }
    };

    @Test
    public void toCommonSchemaLogs() {
        Collection<CommonSchemaLog> logs = mLogFactory.toCommonSchemaLogs(null);
        assertNotNull(logs);
        assertSame(0, logs.size());
    }
}