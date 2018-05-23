package com.microsoft.appcenter.ingestion.models.one;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PartCUtilsAndroidTest {

    @Test
    public void coverInit() {
        new PartCUtils();
    }

    @Test
    public void nullProperties() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        PartCUtils.addPartCFromLog(null, log);
        assertNull(log.getData());
    }

    @Test
    public void emptyProperties() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        PartCUtils.addPartCFromLog(new HashMap<String, String>(), log);
        assertEquals(0, log.getData().getProperties().length());
    }

    @Test
    public void filterPartBReserved() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        properties.put("baseDataType", "custom");
        properties.put("baseData", "{}");
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("b", log.getData().getProperties().optString("a", null));
    }

    @Test
    public void deepNestingProperty() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("a.b.c", "v");
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        JSONObject a = log.getData().getProperties().optJSONObject("a");
        assertNotNull(a);
        JSONObject b = a.getJSONObject("b");
        assertNotNull(b);
        assertEquals("v", b.optString("c", null));
    }
}
