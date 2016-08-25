package com.microsoft.sonoma.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public final class TestUtils {

    /**
     * TAG for test log.
     */
    public static final String TAG = "TestRunner";

    private TestUtils() {
    }

    public static void checkEquals(Object a, Object b) {
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }

    public static void checkNotEquals(Object a, Object b) {
        assertNotEquals(a, b);
        assertNotEquals(b, a);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    public static void compareSelfNullClass(Object o) {
        assertEquals(o, o);
        assertNotEquals(o, null);
        assertNotEquals(o, o.getClass());
    }
}
