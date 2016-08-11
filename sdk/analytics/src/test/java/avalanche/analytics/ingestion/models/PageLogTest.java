package avalanche.analytics.ingestion.models;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import avalanche.analytics.utils.TestUtils;

import static avalanche.analytics.utils.TestUtils.checkEquals;
import static avalanche.analytics.utils.TestUtils.checkNotEquals;

@SuppressWarnings("unused")
public class PageLogTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new PageLog());
    }

    @Test
    public void compareDevices() {

        /* Empty objects. */
        PageLog a = new PageLog();
        PageLog b = new PageLog();
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
