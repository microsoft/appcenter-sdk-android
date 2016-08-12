package avalanche.core.ingestion.models.json;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.Collections;

import avalanche.core.AndroidTestUtils;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;

import static avalanche.core.AndroidTestUtils.TAG;
import static avalanche.core.ingestion.models.json.MockLog.MOCK_LOG_TYPE;

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