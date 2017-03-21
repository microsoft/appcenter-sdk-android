package com.microsoft.azure.mobile.ingestion.models.json;

import com.microsoft.azure.mobile.AndroidTestUtils;
import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.ingestion.models.StartServiceLog;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.microsoft.azure.mobile.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static com.microsoft.azure.mobile.test.TestUtils.TAG;

@SuppressWarnings("unused")
public class LogSerializerAndroidTest {

    @Test
    public void emptyLogs() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setLogs(Collections.<Log>emptyList());
        LogSerializer serializer = new DefaultLogSerializer();
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }

    @Test
    public void oneLog() throws JSONException {
        LogContainer expectedContainer = AndroidTestUtils.generateMockLogContainer();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
        Assert.assertEquals(expectedContainer.hashCode(), actualContainer.hashCode());
    }

    @Test(expected = JSONException.class)
    public void deserializeUnknownType() throws JSONException {
        MockLog log = AndroidTestUtils.generateMockLog();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeLog(log);
        android.util.Log.v(TAG, payload);
        new DefaultLogSerializer().deserializeLog(payload);
    }

    @Test
    public void startServiceLog() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        List<Log> logs = new ArrayList<>();
        {
            StartServiceLog log = new StartServiceLog();
            List<String> services = new ArrayList<>();
            services.add("FIRST");
            services.add("SECOND");
            log.setServices(services);
            logs.add(log);
        }
        expectedContainer.setLogs(logs);
        UUID sid = UUIDUtils.randomUUID();
        for (Log log : logs) {
            log.setSid(sid);
        }

        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}