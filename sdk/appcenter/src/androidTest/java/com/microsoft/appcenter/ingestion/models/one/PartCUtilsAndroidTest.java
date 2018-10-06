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

import static com.microsoft.appcenter.ingestion.models.one.PartCUtils.DATA_TYPE_DATETIME;
import static com.microsoft.appcenter.ingestion.models.one.PartCUtils.DATA_TYPE_DOUBLE;
import static com.microsoft.appcenter.ingestion.models.one.PartCUtils.DATA_TYPE_INT64;
import static com.microsoft.appcenter.ingestion.models.one.PartCUtils.METADATA_FIELDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
        assertNull(log.getExt());
    }

    @Test
    public void emptyProperties() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        PartCUtils.addPartCFromLog(Collections.<TypedProperty>emptyList(), log);
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt());
    }

    @Test
    public void emptyStringsAreAllowed() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("", ""));
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals("", log.getData().getProperties().getString(""));
        assertNull(log.getExt());
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
        assertNull(log.getExt());
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
        assertNull(log.getExt());
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
        assertNull(log.getExt());
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

        /* Check data. */
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(new Date(100), JSONDateUtils.toDate(log.getData().getProperties().getString("a")));

        /* Check metadata. */
        JSONObject expectedMetadata = new JSONObject();
        JSONObject a = new JSONObject();
        a.put("a", DATA_TYPE_DATETIME);
        expectedMetadata.put(METADATA_FIELDS, a);
        assertNotNull(log.getExt());
        assertNotNull(log.getExt().getMetadata());
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
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

        /* Check data. */
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(1.1, log.getData().getProperties().getDouble("a"), 0);

        /* Check metadata. */
        JSONObject expectedMetadata = new JSONObject();
        JSONObject a = new JSONObject();
        a.put("a", DATA_TYPE_DOUBLE);
        expectedMetadata.put(METADATA_FIELDS, a);
        assertNotNull(log.getExt());
        assertNotNull(log.getExt().getMetadata());
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
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

        /* Check metadata. */
        JSONObject expectedMetadata = new JSONObject();
        JSONObject a = new JSONObject();
        a.put("a", DATA_TYPE_INT64);
        expectedMetadata.put(METADATA_FIELDS, a);
        assertNotNull(log.getExt());
        assertNotNull(log.getExt().getMetadata());
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void longTypedPropertyReuseExtensions() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        Extensions ext = new Extensions();
        log.setExt(ext);
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty property = new LongTypedProperty();
        property.setName("a");
        property.setValue(10000000000L);
        properties.add(property);
        PartCUtils.addPartCFromLog(properties, log);
        assertEquals(1, log.getData().getProperties().length());
        assertEquals(10000000000L, log.getData().getProperties().getLong("a"));

        /* Check metadata. */
        JSONObject expectedMetadata = new JSONObject();
        JSONObject a = new JSONObject();
        a.put("a", DATA_TYPE_INT64);
        expectedMetadata.put(METADATA_FIELDS, a);
        assertSame(ext, log.getExt());
        assertNotNull(log.getExt().getMetadata());
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void unknownTypedProperty() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        TypedProperty typedProperty = new TypedProperty() {

            @Override
            public String getType() {
                return "unknown";
            }
        };
        typedProperty.setName("a");
        PartCUtils.addPartCFromLog(Collections.singletonList(typedProperty), log);

        /* Data is empty because the invalid property filtered out. */
        assertEquals(0, log.getData().getProperties().length());

        /* And we don't send metadata when using only standard types. */
        assertNull(log.getExt());
    }

    @Test
    public void nestingWithTypes() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("p.a");
        a.setValue(1);
        properties.add(a);
        DoubleTypedProperty b = new DoubleTypedProperty();
        b.setName("p.b");
        b.setValue(2.0);
        properties.add(b);
        BooleanTypedProperty c = new BooleanTypedProperty();
        c.setName("p.c");
        c.setValue(true);
        properties.add(c);
        PartCUtils.addPartCFromLog(properties, log);

        /* Check data. */
        JSONObject p = new JSONObject();
        p.put("a", 1);
        p.put("b", 2.0);
        p.put("c", true);
        JSONObject expectedData = new JSONObject();
        expectedData.put("p", p);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata, boolean is a default type. */
        JSONObject f2 = new JSONObject();
        f2.put("a", DATA_TYPE_INT64);
        f2.put("b", DATA_TYPE_DOUBLE);
        JSONObject mp = new JSONObject();
        mp.put(METADATA_FIELDS, f2);
        JSONObject f1 = new JSONObject();
        f1.put("p", mp);
        JSONObject expectedMetadata = new JSONObject();
        expectedMetadata.put(METADATA_FIELDS, f1);
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }
}
