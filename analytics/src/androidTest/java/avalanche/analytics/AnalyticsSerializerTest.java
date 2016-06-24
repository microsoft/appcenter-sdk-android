package avalanche.analytics;

import junit.framework.Assert;

import org.json.JSONException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import avalanche.analytics.ingestion.models.EventLog;
import avalanche.analytics.ingestion.models.PageLog;
import avalanche.analytics.ingestion.models.SessionLog;
import avalanche.base.ingestion.models.Log;
import avalanche.base.ingestion.models.LogContainer;
import avalanche.base.ingestion.models.json.DefaultLogContainerSerializer;
import avalanche.base.ingestion.models.json.LogContainerSerializer;

public class AnalyticsSerializerTest {

    private static final String TAG = "TestRunner";

    @Test
    public void someBatch() throws JSONException {
        LogContainer expectedContainer = new LogContainer();
        expectedContainer.setAppId("app000123");
        expectedContainer.setInstallId("0123456789abcdef0123456789abcdef");
        List<Log> logs = new ArrayList<>();
        expectedContainer.setLogs(logs);

        String sid = UUID.randomUUID().toString();
        {
            SessionLog startSessionLog = new SessionLog();
            startSessionLog.setSid(sid);
            logs.add(startSessionLog);
        }
        {
            PageLog pageLog = new PageLog();
            pageLog.setSid(sid);
            pageLog.setName("home");
            logs.add(pageLog);
        }
        {
            PageLog pageLog = new PageLog();
            pageLog.setSid(sid);
            pageLog.setName("settings");
            pageLog.setProperties(new HashMap<String, String>() {{
                put("from", "home_menu");
                put("orientation", "portrait");
            }});
            logs.add(pageLog);
        }
        {
            EventLog eventLog = new EventLog();
            eventLog.setId(UUID.randomUUID().toString());
            eventLog.setSid(sid);
            eventLog.setName("subscribe");
            logs.add(eventLog);
        }
        {
            EventLog eventLog = new EventLog();
            eventLog.setId(UUID.randomUUID().toString());
            eventLog.setSid(sid);
            eventLog.setName("click");
            eventLog.setProperties(new HashMap<String, String>() {{
                put("x", "1");
                put("y", "2");
            }});
            logs.add(eventLog);
        }
        {
            SessionLog endSessionLog = new SessionLog();
            endSessionLog.setSid(sid);
            endSessionLog.setEnd(true);
            logs.add(endSessionLog);
        }
        LogContainerSerializer serializer = new DefaultLogContainerSerializer();
        serializer.addLogFactory(SessionLog.TYPE, new SessionLogFactory());
        serializer.addLogFactory(PageLog.TYPE, new PageLogFactory());
        serializer.addLogFactory(EventLog.TYPE, new EventLogFactory());
        String payload = serializer.serialize(expectedContainer);
        android.util.Log.v(TAG, payload);
        LogContainer actualContainer = serializer.deserialize(payload);
        Assert.assertEquals(expectedContainer, actualContainer);
    }
}