package com.microsoft.appcenter.push;

import com.microsoft.appcenter.ingestion.models.Log;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.ingestion.models.json.DefaultLogSerializer;
import com.microsoft.appcenter.ingestion.models.json.LogSerializer;
import com.microsoft.appcenter.push.ingestion.models.PushInstallationLog;
import com.microsoft.appcenter.push.ingestion.models.json.PushInstallationLogFactory;
import com.microsoft.appcenter.utils.UUIDUtils;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
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
            log.setTimestamp(new Date());
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
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}
