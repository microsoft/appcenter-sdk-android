package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;
import com.microsoft.appcenter.analytics.ingestion.models.StartSessionLog;
import com.microsoft.appcenter.analytics.ingestion.models.json.EventLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.PageLogFactory;
import com.microsoft.appcenter.analytics.ingestion.models.json.StartSessionLogFactory;
import com.microsoft.appcenter.ingestion.models.Device;
import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.utils.UUIDUtils;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class AnalyticsSerializerTest {

    private static final String TAG = "TestRunner";

    @Test
    public void someBatch() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        Device device = new Device();
        device.setSdkName("appcenter.android");
        device.setSdkVersion("1.2.3");
        device.setModel("S5");
        device.setOemName("HTC");
        device.setOsName("Android");
        device.setOsVersion("4.0.3");
        device.setOsBuild("LMY47X");
        device.setOsApiLevel(15);
        device.setLocale("en_US");
        device.setTimeZoneOffset(120);
        device.setScreenSize("800x600");
        device.setAppVersion("3.2.1");
        device.setAppBuild("42");
        List<Log> logs = new ArrayList<>();
        {
            StartSessionLog startSessionLog = new StartSessionLog();
            startSessionLog.setTimestamp(new Date());
            logs.add(startSessionLog);
        }
        expectedContainer.setLogs(logs);
        {
            PageLog pageLog = new PageLog();
            pageLog.setTimestamp(new Date());
            pageLog.setName("home");
            logs.add(pageLog);
        }
        {
            PageLog pageLog = new PageLog();
            pageLog.setTimestamp(new Date());
            pageLog.setName("settings");
            pageLog.setProperties(new HashMap<String, String>() {{
                put("from", "home_menu");
                put("orientation", "portrait");
            }});
            logs.add(pageLog);
        }
        {
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(new Date());
            eventLog.setId(UUIDUtils.randomUUID());
            eventLog.setName("subscribe");
            logs.add(eventLog);
        }
        {
            EventLog eventLog = new EventLog();
            eventLog.setTimestamp(new Date());
            eventLog.setId(UUIDUtils.randomUUID());
            eventLog.setName("click");
            eventLog.setProperties(new HashMap<String, String>() {{
                put("x", "1");
                put("y", "2");
            }});
            logs.add(eventLog);
        }
        UUID sid = UUIDUtils.randomUUID();
        for (Log log : logs) {
            log.setSid(sid);
            log.setDevice(device);
        }
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(StartSessionLog.TYPE, new StartSessionLogFactory());
        serializer.addLogFactory(PageLog.TYPE, new PageLogFactory());
        serializer.addLogFactory(EventLog.TYPE, new EventLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}