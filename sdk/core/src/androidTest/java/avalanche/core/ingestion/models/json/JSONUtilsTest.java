package avalanche.core.ingestion.models.json;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import avalanche.core.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("unused")
public class JSONUtilsTest {

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
        MockLog firstLog = TestUtils.generateMockLog();
        MockLog secondLog = TestUtils.generateMockLog();

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
}
