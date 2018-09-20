package com.microsoft.appcenter.ingestion.models.one;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
    public void emptyStringsAreAllowed() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("", "");
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("", log.getData().getProperties().getString(""));
    }

    @Test()
    public void filterInvalidProperties() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("a", "b");
        properties.put(null, "c");
        properties.put("d", null);
        properties.put("baseData", "{}");
        properties.put("baseDataType", "custom");
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("b", log.getData().getProperties().getString("a"));
    }

    @Test
    public void deepNestingProperty() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new HashMap<>();
        properties.put("a.b", "1");
        properties.put("a.c.d", "2");
        properties.put("a.c.e", "3");
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        JSONObject a = log.getData().getProperties().optJSONObject("a");
        assertNotNull(a);
        assertEquals("1", a.optString("b", null));
        JSONObject c = a.optJSONObject("c");
        assertNotNull(c);
        assertEquals("2", c.optString("d", null));
        assertEquals("3", c.optString("e", null));
    }

    @Test
    public void overrideProperty() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("a.b", "1");
        properties.put("a.b.c.d", "2");
        properties.put("a.b.c", "3");
        PartCUtils.addPartCFromLog(properties, log);
        JSONObject b = log.getData().getProperties().optJSONObject("a").optJSONObject("b");
        assertNotNull(b);
        assertEquals("3", b.optString("c", null));
    }
}
