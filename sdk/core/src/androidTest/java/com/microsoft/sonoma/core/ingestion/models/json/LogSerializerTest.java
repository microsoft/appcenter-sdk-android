package com.microsoft.sonoma.core.ingestion.models.json;

import com.microsoft.sonoma.core.AndroidTestUtils;
import com.microsoft.sonoma.core.ingestion.models.Log;
import com.microsoft.sonoma.core.ingestion.models.LogContainer;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.Collections;

import static com.microsoft.sonoma.test.TestUtils.TAG;
import static com.microsoft.sonoma.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;

@SuppressWarnings("unused")
public class LogSerializerTest {

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
    public void deviceLog() throws JSONException {
        LogContainer expectedContainer = AndroidTestUtils.generateMockLogContainer();
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
        Assert.assertEquals(expectedContainer.hashCode(), actualContainer.hashCode());
    }
}