package avalanche.base.ingestion.models.json;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import avalanche.base.ingestion.models.DeviceLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;

public class LogContainerSerializerTest {

    private static final String TAG = "TestRunner";

    @Test(expected = JSONException.class)
    public void nullFields() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        String payload = serializer.serialize(expectedContainer);
        android.util.Log.v(TAG, payload);
    }

    @Test
    public void emptyLogs() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setAppId("app000123");
        expectedContainer.setInstallId("0123456789abcdef0123456789abcdef");
        expectedContainer.setLogs(Collections.<Log>emptyList());
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        String payload = serializer.serialize(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserialize(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }

    @Test
    public void deviceLog() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setAppId("app000123");
        expectedContainer.setInstallId("0123456789abcdef0123456789abcdef");
        DeviceLog deviceLog = new DeviceLog();
        deviceLog.setSdkVersion("1.2.3");
        deviceLog.setModel("S5");
        deviceLog.setOemName("HTC");
        deviceLog.setOsName("Android");
        deviceLog.setOsVersion("4.0.3");
        deviceLog.setOsApiLevel(15);
        deviceLog.setLocale("en_US");
        deviceLog.setTimeZoneOffset(120);
        deviceLog.setScreenSize("800x600");
        deviceLog.setAppVersion("3.2.1");
        List<Log> logs = new ArrayList<>();
        logs.add(deviceLog);
        expectedContainer.setLogs(logs);
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        String payload = serializer.serialize(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserialize(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}