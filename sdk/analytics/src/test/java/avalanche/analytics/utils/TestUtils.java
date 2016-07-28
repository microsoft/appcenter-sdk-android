package avalanche.analytics.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class TestUtils {

    private TestUtils() {
    }

    public static void checkNotEquals(Object a, Object b) {
        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    public static void checkEquals(Object a, Object b) {
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    public static void compareSelfNullClass(Object o) {
        assertEquals(o, o);
        assertNotEquals(o, null);
        assertNotEquals(o, o.getClass());
    }
}
