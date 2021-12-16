/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.AndroidTestUtils;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.microsoft.appcenter.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static com.microsoft.appcenter.test.TestUtils.TAG;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unused")
public class LogSerializerAndroidTest {

    @Test
    public void emptyLogs() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setLogs(Collections.<Log>emptyList());
        LogSerializer serializer = new DefaultLogSerializer();
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);
        assertEquals(expectedContainer, actualContainer);
    }

    @Test
    public void oneLog() throws JSONException {
        LogContainer expectedContainer = AndroidTestUtils.generateMockLogContainer();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);
        assertEquals(expectedContainer, actualContainer);
        assertEquals(expectedContainer.hashCode(), actualContainer.hashCode());
    }

    @Test(expected = JSONException.class)
    public void deserializeUnknownType() throws JSONException {
        MockLog log = AndroidTestUtils.generateMockLog();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeLog(log);
        android.util.Log.v(TAG, payload);
        new DefaultLogSerializer().deserializeLog(payload, null);
    }

    @Test
    public void readJsonWhenIsOneCollectorPropertyEnabledIsNull() throws JSONException {

        // Prepare start service log.
        StartServiceLog serviceLog = new StartServiceLog();
        List<String> services = new ArrayList<>();
        services.add("FIRST");
        services.add("SECOND");
        serviceLog.setServices(services);
        UUID sid = UUID.randomUUID();
        serviceLog.setSid(sid);
        serviceLog.setTimestamp(new Date());

        // Prepare log container.
        ArrayList<Log> logArrayList = new ArrayList<>();
        logArrayList.add(serviceLog);
        LogContainer serializerContainer = new LogContainer();
        serializerContainer.setLogs(logArrayList);

        // Serializer log container.
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        String payload = serializer.serializeContainer(serializerContainer);

        // Deserialize log container.
        LogContainer deserializeContainer = serializer.deserializeContainer(payload, null);
        StartServiceLog deserializeStartServiceLog = (StartServiceLog) deserializeContainer.getLogs().get(0);
        Assert.assertNull(deserializeStartServiceLog.isOneCollectorEnabled());
    }

    @Test
    public void startServiceLog() throws JSONException {
        StartServiceLog log = new StartServiceLog();
        List<String> services = new ArrayList<>();
        services.add("FIRST");
        services.add("SECOND");
        log.setServices(services);
        UUID sid = UUID.randomUUID();
        log.setSid(sid);
        log.setTimestamp(new Date());

        /* Verify serialize and deserialize. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        String payload = serializer.serializeLog(log);
        Log actualContainer = serializer.deserializeLog(payload, null);
        assertEquals(log, actualContainer);
    }

    @Test
    public void logWithUserId() throws JSONException {
        MockLog expectedLog = AndroidTestUtils.generateMockLog();
        expectedLog.setTimestamp(new Date());
        expectedLog.setUserId("charlie");

        /* Verify serialize and deserialize. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeLog(expectedLog);
        Log actualLog = serializer.deserializeLog(payload, null);
        assertEquals(expectedLog, actualLog);
        assertEquals("charlie", actualLog.getUserId());
    }

    @Test
    public void toCommonSchemaLog() {
        LogFactory logFactory = mock(LogFactory.class);
        MockLog log = AndroidTestUtils.generateMockLog();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, logFactory);
        serializer.toCommonSchemaLog(log);
        verify(logFactory).toCommonSchemaLogs(log);
    }
}