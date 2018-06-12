package com.microsoft.appcenter.distribute;

import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
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
import java.util.List;
import java.util.UUID;

@SuppressWarnings("unused")
public class DistributeSerializerTest {

    @Test
    public void serialize() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        List<Log> logs = new ArrayList<>();
        {
            DistributionStartSessionLog log = new DistributionStartSessionLog();
            log.setTimestamp(new Date());
            logs.add(log);
        }
        expectedContainer.setLogs(logs);
        UUID sid = UUIDUtils.randomUUID();
        for (Log log : logs) {
            log.setSid(sid);
        }

        /* Serialize and deserialize logs container. */
        LogSerializer serializer = new DefaultLogSerializer();
        serializer.addLogFactory(DistributionStartSessionLog.TYPE, new DistributionStartSessionLogFactory());
        String payload = serializer.serializeContainer(expectedContainer);
        LogContainer actualContainer = serializer.deserializeContainer(payload, null);

        /* Verify that logs container successfully deserialized. */
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}
