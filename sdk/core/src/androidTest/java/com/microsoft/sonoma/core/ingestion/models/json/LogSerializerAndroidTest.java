package com.microsoft.sonoma.core.ingestion.models.json;

import com.microsoft.sonoma.core.AndroidTestUtils;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.LogContainer;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.Collections;

import static com.microsoft.sonoma.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;
import static com.microsoft.sonoma.test.TestUtils.TAG;

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
}