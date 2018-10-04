package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.ingestion.models.json.JSONDateUtils;
import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PartCUtilsAndroidTest {

    private static StringTypedProperty typedProperty(String key, String value) {
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName(key);
        stringTypedProperty.setValue(value);
        return stringTypedProperty;
    }

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
        PartCUtils.addPartCFromLog(Collections.<TypedProperty>emptyList(), log);
        assertEquals(0, log.getData().getProperties().length());
    }

    @Test
    public void emptyStringsAreAllowed() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("", ""));
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("", log.getData().getProperties().getString(""));
    }

    @Test()
    public void filterInvalidProperties() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("a", "b"));
        properties.add(typedProperty(null, "c"));
        properties.add(typedProperty("d", null));
        properties.add(typedProperty("baseData", "{}"));
        properties.add(typedProperty("baseDataType", "custom"));
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("b", log.getData().getProperties().getString("a"));
    }

    @Test
    public void deepNestingProperty() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("a.b", "1"));
        properties.add(typedProperty("a.c.d", "2"));
        properties.add(typedProperty("a.c.e", "3"));
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
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("a.b", "1"));
        properties.add(typedProperty("a.b.c.d", "2"));
        properties.add(typedProperty("a.b.c", "3"));
        PartCUtils.addPartCFromLog(properties, log);
        JSONObject b = log.getData().getProperties().optJSONObject("a").optJSONObject("b");
        assertNotNull(b);
        assertEquals("3", b.optString("c", null));
    }

    @Test
    public void booleanTypedProperty() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        BooleanTypedProperty property = new BooleanTypedProperty();
        property.setName("a");
        property.setValue(true);
        properties.add(property);
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertTrue(log.getData().getProperties().getBoolean("a"));
    }

    @Test
    public void dateTimeTypedProperty() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        DateTimeTypedProperty property = new DateTimeTypedProperty();
        property.setName("a");
        property.setValue(new Date(100));
        properties.add(property);
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(new Date(100), JSONDateUtils.toDate(log.getData().getProperties().getString("a")));
    }

    @Test
    public void doubleTypedProperty() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        DoubleTypedProperty property = new DoubleTypedProperty();
        property.setName("a");
        property.setValue(1.1);
        properties.add(property);
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(1.1, log.getData().getProperties().getDouble("a"), 0);
    }

    @Test
    public void longTypedProperty() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty property = new LongTypedProperty();
        property.setName("a");
        property.setValue(10000000000L);
        properties.add(property);
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(10000000000L, log.getData().getProperties().getLong("a"));
    }
}
