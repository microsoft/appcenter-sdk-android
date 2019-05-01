/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.ingestion.models.json;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.one.json.CommonSchemaEventLogFactory;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaDataUtils;
import com.microsoft.appcenter.ingestion.models.one.CommonSchemaLog;
import com.microsoft.appcenter.ingestion.models.one.PartAUtils;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
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
    @PrepareForTest({PartAUtils.class, CommonSchemaDataUtils.class})
    public void convertEventWithoutProperties() {

        /* Mock utilities. */
        mockStatic(PartAUtils.class);
        mockStatic(CommonSchemaDataUtils.class);

        /* Create event log. */
        EventLog log = new EventLog();
        log.setName("test");

        /* Old properties are ignored. */
        Map<String, String> oldProperties = new HashMap<>();
        oldProperties.put("ignored", "ignored");
        log.setProperties(oldProperties);

        /* Set typed properties. */
        List<TypedProperty> properties = new ArrayList<>();
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName("a");
        stringTypedProperty.setValue("b");
        properties.add(stringTypedProperty);
        log.setTypedProperties(properties);

        /* With 2 targets. */
        log.addTransmissionTarget("t1");
        log.addTransmissionTarget("t2");

        /* And with a tag. */
        Object tag = new Object();
        log.setTag(tag);

        /* When we convert logs. */
        Collection<CommonSchemaLog> convertedLogs = new EventLogFactory().toCommonSchemaLogs(log);

        /* Check number of logs: 1 per target. */
        assertNotNull(convertedLogs);
        assertEquals(2, convertedLogs.size());

        /* For each target. */
        for (CommonSchemaLog commonSchemaLog : convertedLogs) {

            /* Check name was added. */
            verifyStatic();
            PartAUtils.setName(same(commonSchemaLog), eq("test"));

            /* Check tag was added. */
            assertSame(tag, commonSchemaLog.getTag());
        }

        /* Check Part A was added with target tokens. */
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t1"));
        verifyStatic();
        PartAUtils.addPartAFromLog(eq(log), notNull(CommonSchemaLog.class), eq("t2"));

        /* Check data was added with typed properties (and thus not old ones). */
        verifyStatic(times(2));
        CommonSchemaDataUtils.addCommonSchemaData(eq(properties), notNull(CommonSchemaLog.class));
    }
}
