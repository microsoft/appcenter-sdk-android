package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.json.CommonSchemaEventLogFactory;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;
import com.microsoft.appcenter.ingestion.models.one.PartCUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
public class EventLogFactoryTest {

    @Test
    public void createEvent() {
        EventLog eventLog = new EventLogFactory().create();
        assertNotNull(eventLog);
        assertEquals(EventLog.TYPE, eventLog.getType());
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
    @PrepareForTest({PartAUtils.class, PartCUtils.class})
    public void convertEventWithoutProperties() {

        /* Mock utilities. */
        mockStatic(PartAUtils.class);

        /* Create event log. */
        EventLog log = new EventLog();
        log.setName("test");
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        log.setProperties(properties);

        /* With 2 targets. */
        log.addTransmissionTarget("t1");
        log.addTransmissionTarget("t2");
        Collection<CommonSchemaLog> convertedLogs = new EventLogFactory().toCommonSchemaLogs(log);
        assertNotNull(convertedLogs);
        assertEquals(2, convertedLogs.size());

        /* Check name and target token copy. */
        for (CommonSchemaLog commonSchemaLog : convertedLogs) {
            assertEquals("test", commonSchemaLog.getName());
        }

        /* Check Part A was added. */
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t1"));
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t2"));

        /* Check Part C was added. */
        verifyStatic(times(2));
        PartCUtils.addPartCFromLog(eq(log.getProperties()), notNull(CommonSchemaLog.class));
    }
}
