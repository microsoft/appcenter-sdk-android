package avalanche.core.ingestion.models.json;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import avalanche.core.ingestion.models.Device;
import avalanche.core.ingestion.models.Log;
import avalanche.core.ingestion.models.LogContainer;
import avalanche.core.utils.UUIDUtils;

import static avalanche.core.TestUtils.TAG;
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
        LogContainer expectedContainer = new LogContainer();
        Device device = new Device();
        device.setSdkVersion("1.2.3");
        device.setModel("S5");
        device.setOemName("HTC");
        device.setOsName("Android");
        device.setOsVersion("4.0.3");
        device.setOsApiLevel(15);
        device.setLocale("en_US");
        device.setTimeZoneOffset(120);
        device.setScreenSize("800x600");
        device.setAppVersion("3.2.1");
        device.setAppBuild("42");
        Log log = new MockLog();
        log.setDevice(device);
        log.setSid(UUIDUtils.randomUUID());
        List<Log> logs = new ArrayList<>();
        logs.add(log);
        expectedContainer.setLogs(logs);
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(MOCK_LOG_TYPE, new MockLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}