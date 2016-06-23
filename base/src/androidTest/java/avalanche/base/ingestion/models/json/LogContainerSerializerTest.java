package avalanche.base.ingestion.models.json;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

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
        DeviceLog expectedLog = new DeviceLog();
        expectedLog.setSdkVersion("1.2.3");
        expectedLog.setModel("S5");
        expectedLog.setOemName("HTC");
        expectedLog.setOsName("Android");
        expectedLog.setOsVersion("4.0.3");
        expectedLog.setOsApiLevel(15);
        expectedLog.setLocale("en_US");
        expectedLog.setTimeZoneOffset(120);
        expectedLog.setScreenSize("800x600");
        expectedLog.setAppVersion("3.2.1");
        ArrayList<Log> logs = new ArrayList<>();
        logs.add(expectedLog);
        expectedContainer.setLogs(logs);
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        String payload = serializer.serialize(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserialize(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}