package com.microsoft.appcenter.ingestion.models.json;

import com.microsoft.appcenter.AndroidTestUtils;
import com.microsoft.appcenter.ingestion.models.CustomPropertiesLog;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.StartServiceLog;
import com.microsoft.appcenter.utils.UUIDUtils;

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
        Assert.assertEquals(expectedContainer, actualContainer);
    }

    @Test
    public void oneLog() throws JSONException {
        LogContainer expectedContainer = AndroidTestUtils.generateMockLogContainer();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);
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
        new DefaultLogSerializer().deserializeLog(payload, null);
    }

    @Test
    public void startServiceLog() throws JSONException {
        StartServiceLog log = new StartServiceLog();
        List<String> services = new ArrayList<>();
        services.add("FIRST");
        services.add("SECOND");
        log.setServices(services);
        UUID sid = UUIDUtils.randomUUID();
        log.setSid(sid);
        log.setTimestamp(new Date());

        /* Verify serialize and deserialize. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(StartServiceLog.TYPE, new StartServiceLogFactory());
        String payload = serializer.serializeLog(log);
        Log actualContainer = serializer.deserializeLog(payload, null);
        Assert.assertEquals(log, actualContainer);
    }

    @Test
    public void customPropertiesLog() throws JSONException {
        CustomPropertiesLog log = new CustomPropertiesLog();
        Map<String, Object> properties = new HashMap<>();
        properties.put("t1", "test");
        properties.put("t2", new Date(0));
        properties.put("t3", 0);
        properties.put("t4", false);
        properties.put("t5", null);
        log.setProperties(properties);
        UUID sid = UUIDUtils.randomUUID();
        log.setSid(sid);
        log.setTimestamp(new Date());

        /* Verify serialize and deserialize. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        String payload = serializer.serializeLog(log);
        Log actualContainer = serializer.deserializeLog(payload, null);
        Assert.assertEquals(log, actualContainer);
    }

    @Test(expected = JSONException.class)
    public void deserializeWithoutProperties() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        serializer.deserializeLog("{" +
                "\"type\": \"customProperties\"," +
                "\"timestamp\": \"2017-07-08T00:32:58.123Z\"" +
                "}", null);
    }

    @Test(expected = JSONException.class)
    public void deserializeWithInvalidType() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        serializer.deserializeLog("{" +
                "\"type\": \"customProperties\"," +
                "\"timestamp\": \"2017-07-08T00:32:58.123Z\"," +
                "\"properties\":[{\"name\":\"test\",\"type\":\"unknown\",\"value\":42}]" +
                "}", null);
    }

    @Test(expected = JSONException.class)
    public void deserializeWithInvalidDate() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        serializer.deserializeLog("{" +
                "\"type\": \"customProperties\"," +
                "\"timestamp\": \"2017-07-08T00:32:58.123Z\"," +
                "\"properties\":[{\"name\":\"test\",\"type\":\"dateTime\",\"value\":\"today\"}]" +
                "}", null);
    }

    @Test(expected = JSONException.class)
    public void deserializeWithInvalidNumber() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(CustomPropertiesLog.TYPE, new CustomPropertiesLogFactory());
        serializer.deserializeLog("{" +
                "\"type\": \"customProperties\"," +
                "\"timestamp\": \"2017-07-08T00:32:58.123Z\"," +
                "\"properties\":[{\"name\":\"test\",\"type\":\"number\",\"value\":false}]" +
                "}", null);
    }

    @Test(expected = JSONException.class)
    public void serializeWithoutProperties() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        CustomPropertiesLog invalidTypeLog = new CustomPropertiesLog();
        invalidTypeLog.setTimestamp(new Date());
        serializer.serializeLog(invalidTypeLog);
    }

    @Test(expected = JSONException.class)
    public void serializeWithInvalidType() throws JSONException {
        LogSerializer serializer = new DefaultLogSerializer();
        CustomPropertiesLog invalidTypeLog = new CustomPropertiesLog();
        invalidTypeLog.setTimestamp(new Date());
        Map<String, Object> invalidTypeProperties = new HashMap<>();
        invalidTypeProperties.put("nested", new HashMap<String, Object>());
        invalidTypeLog.setProperties(invalidTypeProperties);
        serializer.serializeLog(invalidTypeLog);
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