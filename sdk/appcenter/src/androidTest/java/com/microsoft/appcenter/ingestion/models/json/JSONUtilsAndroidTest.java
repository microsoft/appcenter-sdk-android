/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.ingestion.models.json;

import android.util.Log;

import com.microsoft.appcenter.AndroidTestUtils;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

@SuppressWarnings("unused")
public class JSONUtilsAndroidTest {

    @Test
    public void utilsCoverage() {
        new JSONUtils();
    }

    @Test
    public void writeReadObject() throws JSONException {

        /* Write to JSON object. */
        JSONStringer writer = new JSONStringer();
        writer.object();
        JSONUtils.write(writer, "int", 1);
        JSONUtils.write(writer, "long", 1000000000L);
        JSONUtils.write(writer, "boolean", true);
        writer.endObject();

        /* Convert to string. */
        String json = writer.toString();
        assertNotNull(json);

        /* Read a JSON object and verify. */
        JSONObject object = new JSONObject(json);
        assertEquals(Integer.valueOf(1), JSONUtils.readInteger(object, "int"));
        assertEquals(Long.valueOf(1000000000L), JSONUtils.readLong(object, "long"));
        assertEquals(true, JSONUtils.readBoolean(object, "boolean"));
    }

    @Test
    public void writeReadMap() throws JSONException {

        /* Create a test map. */
        final Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        /* Write to JSON object. */
        JSONStringer writer = new JSONStringer();
        writer.object();
        JSONUtils.writeMap(writer, "map", map);
        writer.endObject();

        /* Convert to string. */
        String json = writer.toString();
        assertNotNull(json);

        /* Read a JSON object and verify. */
        JSONObject object = new JSONObject(json);
        assertEquals(map, JSONUtils.readMap(object, "map"));
    }

    @Test
    public void writeReadArray() throws JSONException {

        /* Generate mock logs. */
        MockLog firstLog = AndroidTestUtils.generateMockLog();
        MockLog secondLog = AndroidTestUtils.generateMockLog();

        /* Create a test list. */
        final List<MockLog> list = new ArrayList<>();
        list.add(firstLog);
        list.add(secondLog);

        /* Write to JSON object. */
        JSONStringer writer = new JSONStringer();
        writer.object();
        JSONUtils.writeArray(writer, "list", list);
        writer.endObject();

        /* Convert to string. */
        String json = writer.toString();
        assertNotNull(json);

        /* Read a JSON object and verify. */
        JSONObject object = new JSONObject(json);
        assertEquals(list, JSONUtils.readArray(object, "list", new MockLogFactory()));

        /* Test null value. */
        writer = new JSONStringer();
        JSONUtils.writeArray(writer, "null", null);

        //noinspection ConstantConditions
        assertNull(writer.toString());
    }

    @Test
    public void writeReadStringArray() throws JSONException {

        /* Create a test list. */
        final List<String> list = new ArrayList<>();
        list.add("FIRST");
        list.add("SECOND");

        /* Write to JSON object. */
        JSONStringer writer = new JSONStringer();
        writer.object();
        JSONUtils.writeStringArray(writer, "list", list);
        writer.endObject();

        /* Convert to string. */
        String json = writer.toString();
        assertNotNull(json);

        /* Read a JSON object and verify. */
        JSONObject object = new JSONObject(json);
        assertEquals(list, JSONUtils.readStringArray(object, "list"));
        assertNull(JSONUtils.readStringArray(object, "missing"));

        /* Test null value. */
        writer = new JSONStringer();
        JSONUtils.writeStringArray(writer, "null", null);

        //noinspection ConstantConditions
        assertNull(writer.toString());
    }

    @Test
    public void readKeyNotExists() throws JSONException {

        /* Create an empty JSON object. */
        JSONObject object = new JSONObject("{}");

        /* Verify. */
        assertNull(JSONUtils.readInteger(object, "key"));
        assertNull(JSONUtils.readLong(object, "key"));
        assertNull(JSONUtils.readBoolean(object, "key"));
        assertNull(JSONUtils.readMap(object, "key"));
        assertNull(JSONUtils.readArray(object, "key", new MockLogFactory()));
    }

    @Test
    public void serializeContainerWithDefaultWriter() throws JSONException {

        /* Create a mock log container. */
        LogContainer mockContainer = mock(LogContainer.class);

        /* Set log level to VERBOSE to instantiate JSONStringer for pretty JSON string. */
        AppCenterLog.setLogLevel(Log.VERBOSE);
        LogSerializer serializer = new DefaultLogSerializer();
        String json = serializer.serializeContainer(mockContainer);

        /* Remove new lines and spaces. */
        json = json.replace("\n", "").replace(" ", "");

        /* Set log level to ERROR to instantiate JSONStringer without indentations. */
        AppCenterLog.setLogLevel(Log.ERROR);

        /* Verify. */
        assertEquals(json, serializer.serializeContainer(mockContainer));
    }
}
