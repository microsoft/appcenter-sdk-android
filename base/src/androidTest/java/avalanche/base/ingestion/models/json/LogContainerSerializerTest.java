package avalanche.base.ingestion.models.json;

import java.io.IOException;

import org.junit.Test;

import android.util.Log;

import avalanche.base.ingestion.models.LogContainer;

import junit.framework.Assert;

public class LogContainerSerializerTest {

    public static final String TAG = "test";

    @Test
    public void emptyLogs() throws IOException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setAppId("app000123");
        expectedContainer.setInstallId("0123456789abcdef0123456789abcdef");
        DefaultLogContainerSerializer serializer = new DefaultLogContainerSerializer();
        String payload = serializer.serialize(expectedContainer);
        Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserialize(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}