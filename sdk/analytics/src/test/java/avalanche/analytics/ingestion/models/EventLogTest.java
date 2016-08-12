package avalanche.analytics.ingestion.models;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import avalanche.test.TestUtils;

import static avalanche.test.TestUtils.checkEquals;
import static avalanche.test.TestUtils.checkNotEquals;

public class EventLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new EventLog());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        EventLog a = new EventLog();
        EventLog b = new EventLog();
        checkEquals(a, b);

        /* Properties. */
        Map<String, String> p1 = new HashMap<>();
        p1.put("a", "b");
        Map<String, String> p2 = new HashMap<>();
        p1.put("c", "d");
        a.setProperties(p1);
        checkNotEquals(a, b);
        a.setProperties(null);
        b.setProperties(p1);
        checkNotEquals(a, b);
        a.setProperties(p2);
        checkNotEquals(a, b);
        a.setProperties(p1);
        checkEquals(a, b);

        /* Id */
        UUID sid1 = UUID.randomUUID();
        UUID sid2 = UUID.randomUUID();
        a.setId(sid1);
        checkNotEquals(a, b);
        a.setId(null);
        b.setId(sid1);
        checkNotEquals(a, b);
        a.setId(sid2);
        checkNotEquals(a, b);
        a.setId(sid1);
        checkEquals(a, b);

        /* Name. */
        a.setName("a");
        checkNotEquals(a, b);
        a.setName(null);
        b.setName("a");
        checkNotEquals(a, b);
        a.setName("b");
        checkNotEquals(a, b);
        a.setName("a");
        checkEquals(a, b);
    }
}
