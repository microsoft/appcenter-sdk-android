package com.microsoft.azure.mobile.push;

import com.microsoft.azure.mobile.ingestion.models.Log;
import com.microsoft.azure.mobile.ingestion.models.LogContainer;
import com.microsoft.azure.mobile.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.azure.mobile.ingestion.models.json.LogSerializer;
import com.microsoft.azure.mobile.push.ingestion.models.PushInstallationLog;
import com.microsoft.azure.mobile.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.azure.mobile.utils.UUIDUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class PushSerializerTest {

    @Test
    public void serialize() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        List<Log> logs = new ArrayList<>();
        {
            PushInstallationLog log = new PushInstallationLog();
            log.setPushToken("TEST");
            logs.add(log);
        }
        expectedContainer.setLogs(logs);
        UUID sid = UUIDUtils.randomUUID();
        for (Log log : logs) {
            log.setSid(sid);
        }

        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(PushInstallationLog.TYPE, new PushInstallationLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        LogContainer actualContainer = serializer.deserializeContainer(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}
