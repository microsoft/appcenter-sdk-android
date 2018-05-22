package com.microsoft.appcenter.analytics.ingestion.models.one.json;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.CommonSchemaEventLog;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
public class CommonSchemaEventLogFactoryTest {

    @Test
    public void createEvent() {
        CommonSchemaEventLog eventLog = new CommonSchemaEventLogFactory().create();
        assertNotNull(eventLog);
        assertEquals(CommonSchemaEventLog.TYPE, eventLog.getType());
    }

    @Test
    public void convertStartSessionLog() {

        /* Start session log not supported yet so empty list. */
        StartSessionLog log = new StartSessionLog();
        log.addTransmissionTarget("test");
        Collection<CommonSchemaLog> convertedLogs = new CommonSchemaEventLogFactory().toCommonSchemaLogs(log);
        assertNotNull(convertedLogs);
        assertEquals(0, convertedLogs.size());
    }

    @Test
    public void convertPageLog() {

        /* Page log not supported yet so empty list. */
        PageLog log = new PageLog();
        log.addTransmissionTarget("test");
        Collection<CommonSchemaLog> convertedLogs = new CommonSchemaEventLogFactory().toCommonSchemaLogs(log);
        assertNotNull(convertedLogs);
        assertEquals(0, convertedLogs.size());
    }

    @Test
    public void dontConvertEventWithoutTargetTokens() {

        /* Create event log with just a name and no target. */
        EventLog log = new EventLog();
        log.setName("test");
        Collection<CommonSchemaLog> convertedLogs = new CommonSchemaEventLogFactory().toCommonSchemaLogs(log);
        assertNotNull(convertedLogs);
        assertEquals(0, convertedLogs.size());
    }

    @Test
    @PrepareForTest(PartAUtils.class)
    public void convertEventWithoutProperties() {

        /* Mock utilities. */
        mockStatic(PartAUtils.class);

        /* Create event log with just a name. */
        EventLog log = new EventLog();
        log.setName("test");

        /* With 2 targets. */
        log.addTransmissionTarget("t1");
        log.addTransmissionTarget("t2");
        Collection<CommonSchemaLog> convertedLogs = new CommonSchemaEventLogFactory().toCommonSchemaLogs(log);
        assertNotNull(convertedLogs);
        assertEquals(2, convertedLogs.size());

        /* Check name. */
        for (CommonSchemaLog commonSchemaLog : convertedLogs) {
            assertEquals("test", commonSchemaLog.getName());
        }

        /* Verify utility interactions. */
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t1"));
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t2"));
    }
}
