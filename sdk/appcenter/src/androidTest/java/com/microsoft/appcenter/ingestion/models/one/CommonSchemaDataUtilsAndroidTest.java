/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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

import static com.microsoft.appcenter.ingestion.models.one.CommonSchemaDataUtils.DATA_TYPE_DATETIME;
import static com.microsoft.appcenter.ingestion.models.one.CommonSchemaDataUtils.DATA_TYPE_DOUBLE;
import static com.microsoft.appcenter.ingestion.models.one.CommonSchemaDataUtils.DATA_TYPE_INT64;
import static com.microsoft.appcenter.ingestion.models.one.CommonSchemaDataUtils.METADATA_FIELDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class CommonSchemaDataUtilsAndroidTest {

    private static StringTypedProperty typedProperty(String key, String value) {
        StringTypedProperty stringTypedProperty = new StringTypedProperty();
        stringTypedProperty.setName(key);
        stringTypedProperty.setValue(value);
        return stringTypedProperty;
    }

    @Test
    public void coverInit() {
        new CommonSchemaDataUtils();
    }

    @Test
    public void nullProperties() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        CommonSchemaDataUtils.addCommonSchemaData(null, log);
        assertNull(log.getData());
        assertNull(log.getExt());
    }

    @Test
    public void emptyProperties() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        CommonSchemaDataUtils.addCommonSchemaData(Collections.<TypedProperty>emptyList(), log);
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt());
    }

    @Test
    public void emptyStringsAreAllowed() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("", ""));
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);
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
        CommonSchemaDataUtils.addCommonSchemaData(Collections.singletonList(typedProperty), log);

        /* Data is empty because the invalid property filtered out. */
        assertEquals(0, log.getData().getProperties().length());

        /* And we don't send metadata when using only standard types. */
        assertNull(log.getExt());
    }

    @Test
    public void nestingWithTypes() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("baseType", "Some.Type"));
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
        LongTypedProperty baseDataD = new LongTypedProperty();
        baseDataD.setName("baseData.d");
        baseDataD.setValue(4);
        properties.add(baseDataD);
        properties.add(typedProperty("baseData.e", "5"));
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data. */
        JSONObject p = new JSONObject();
        p.put("a", 1);
        p.put("b", 2.0);
        p.put("c", true);
        JSONObject baseData = new JSONObject();
        baseData.put("d", 4);
        baseData.put("e", "5");
        JSONObject expectedData = new JSONObject();
        expectedData.put("baseType", "Some.Type");
        expectedData.put("p", p);
        expectedData.put("baseData", baseData);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata, boolean is a default type. */
        JSONObject metadataPChildren = new JSONObject();
        metadataPChildren.put("a", DATA_TYPE_INT64);
        metadataPChildren.put("b", DATA_TYPE_DOUBLE);
        JSONObject metadataPFields = new JSONObject();
        metadataPFields.put(METADATA_FIELDS, metadataPChildren);
        JSONObject metadataBaseDataChildren = new JSONObject();
        metadataBaseDataChildren.put("d", DATA_TYPE_INT64);
        JSONObject metadataBaseDataFields = new JSONObject();
        metadataBaseDataFields.put(METADATA_FIELDS, metadataBaseDataChildren);
        JSONObject metadataFields = new JSONObject();
        metadataFields.put("p", metadataPFields);
        metadataFields.put("baseData", metadataBaseDataFields);
        JSONObject expectedMetadata = new JSONObject();
        expectedMetadata.put(METADATA_FIELDS, metadataFields);
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void overrideMetadataToNull() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("a.b.c");
        a.setValue(1);
        properties.add(a);
        StringTypedProperty b = new StringTypedProperty();
        b.setName("a.b");
        b.setValue("2");
        properties.add(b);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data. */
        JSONObject aData = new JSONObject();
        aData.put("b", "2");
        JSONObject expectedData = new JSONObject();
        expectedData.put("a", aData);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata is null */
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void overrideMetadataCleanup() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("a.b.c");
        a.setValue(1);
        properties.add(a);
        StringTypedProperty b = new StringTypedProperty();
        b.setName("a.b");
        b.setValue("2");
        properties.add(b);
        DoubleTypedProperty c = new DoubleTypedProperty();
        c.setName("a.c");
        c.setValue(3.14);
        properties.add(c);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data. */
        JSONObject aData = new JSONObject();
        aData.put("b", "2");
        aData.put("c", 3.14);
        JSONObject expectedData = new JSONObject();
        expectedData.put("a", aData);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata contains only a.c. */
        JSONObject aFields = new JSONObject();
        aFields.put("c", DATA_TYPE_DOUBLE);
        JSONObject aMetadata = new JSONObject();
        aMetadata.put(METADATA_FIELDS, aFields);
        JSONObject rootFields = new JSONObject();
        rootFields.put("a", aMetadata);
        JSONObject expectedMetadata = new JSONObject();
        expectedMetadata.put(METADATA_FIELDS, rootFields);
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void noNestingAccident() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("a.b");
        a.setValue(1);
        properties.add(a);
        DoubleTypedProperty b = new DoubleTypedProperty();
        b.setName("b.c");
        b.setValue(2.2);
        properties.add(b);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data. */
        JSONObject aData = new JSONObject();
        aData.put("b", 1);
        JSONObject bData = new JSONObject();
        bData.put("c", 2.2);
        JSONObject expectedData = new JSONObject();
        expectedData.put("a", aData);
        expectedData.put("b", bData);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata. a.b */
        JSONObject aFields = new JSONObject();
        aFields.put("b", DATA_TYPE_INT64);
        JSONObject aMetadata = new JSONObject();
        aMetadata.put(METADATA_FIELDS, aFields);

        /* b.c */
        JSONObject bFields = new JSONObject();
        bFields.put("c", DATA_TYPE_DOUBLE);
        JSONObject bMetadata = new JSONObject();
        bMetadata.put(METADATA_FIELDS, bFields);

        /* f at root. */
        JSONObject rootFields = new JSONObject();
        rootFields.put("a", aMetadata);
        rootFields.put("b", bMetadata);

        /* Check. */
        JSONObject expectedMetadata = new JSONObject();
        expectedMetadata.put(METADATA_FIELDS, rootFields);
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void noMetadataCleanupOnNestingString() throws JSONException {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("a.b");
        a.setValue(1);
        properties.add(a);
        StringTypedProperty b = new StringTypedProperty();
        b.setName("b.c");
        b.setValue("2");
        properties.add(b);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data. */
        JSONObject aData = new JSONObject();
        aData.put("b", 1);
        JSONObject bData = new JSONObject();
        bData.put("c", "2");
        JSONObject expectedData = new JSONObject();
        expectedData.put("a", aData);
        expectedData.put("b", bData);
        assertEquals(expectedData.toString(), log.getData().getProperties().toString());

        /* Check metadata. a.b only. */
        JSONObject aFields = new JSONObject();
        aFields.put("b", DATA_TYPE_INT64);
        JSONObject aMetadata = new JSONObject();
        aMetadata.put(METADATA_FIELDS, aFields);
        JSONObject rootFields = new JSONObject();
        rootFields.put("a", aMetadata);
        JSONObject expectedMetadata = new JSONObject();
        expectedMetadata.put(METADATA_FIELDS, rootFields);
        assertEquals(expectedMetadata.toString(), log.getExt().getMetadata().getMetadata().toString());
    }

    @Test
    public void invalidBaseType() {

        /* When using invalid base type. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("baseType");
        a.setValue(1);
        properties.add(a);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data and metadata is missing type. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void invalidBaseData() {

        /* When using invalid base data. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        StringTypedProperty a = new StringTypedProperty();
        a.setName("baseData");
        a.setValue("myData");
        properties.add(a);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data and metadata is missing type. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void baseDataMissing() {

        /* When using invalid base data. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        StringTypedProperty a = new StringTypedProperty();
        a.setName("baseType");
        a.setValue("Some.Type");
        properties.add(a);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data and metadata is missing type. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void baseTypeMissing() {

        /* When using invalid base data. */
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        StringTypedProperty a = new StringTypedProperty();
        a.setName("baseData.test");
        a.setValue("test");
        properties.add(a);
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check data and metadata is missing type. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void invalidBaseTypeRemovesBaseData() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        LongTypedProperty a = new LongTypedProperty();
        a.setName("baseType");
        a.setValue(3);
        properties.add(a);
        properties.add(typedProperty("baseData.something", "value"));
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check everything removed. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void invalidBaseDataRemovesBaseType() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("baseType", "Some.Type"));
        properties.add(typedProperty("baseData", "value"));
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check everything removed. */
        assertEquals(0, log.getData().getProperties().length());
        assertNull(log.getExt().getMetadata());
    }

    @Test
    public void baseTypeOverriddenToBeInvalid() {
        MockCommonSchemaLog log = new MockCommonSchemaLog();
        log.setExt(new Extensions());
        List<TypedProperty> properties = new ArrayList<>();
        properties.add(typedProperty("baseType", "Some.Type"));
        properties.add(typedProperty("baseType.something", "test"));
        properties.add(typedProperty("baseData.something", "test"));
        CommonSchemaDataUtils.addCommonSchemaData(properties, log);

        /* Check only string base type was kept. */
        assertEquals(2, log.getData().getProperties().length());
        assertEquals("Some.Type", log.getData().getProperties().optString("baseType"));
        assertNull(log.getExt().getMetadata());
    }
}
