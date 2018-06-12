package com.microsoft.appcenter.test;

import org.junit.After;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
    }

    public static void compareSelfNullClass(Object o) {
        assertEquals(o, o);
        assertNotEquals(o, null);
        assertNotEquals(o, o.getClass());
    }

    /**
     * Use this method as a last resort alternative when Whitebox.setInternalState fails
     * on static final variable and you are using the PowerMockRule (or just mockito).
     * Please note that even this method will be useless if the constant is inlined by the compiler
     * (e.g. the constant is directly a String or number or boolean, for that method to work,
     * the static final variable must be the result of a method call).
     * You need to reset the state on a {@link After} method to avoid side effects on other tests.
     *
     * @param clazz     class containing the field.
     * @param fieldName field name.
     * @param value     value to set.
     * @throws Exception if an exception occurs.
     */
    @SuppressWarnings({"SameParameterValue", "JavaReflectionMemberAccess"})
    public static void setInternalState(Class clazz, String fieldName, Object value) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Field modifiers = field.getClass().getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, value);
    }

    /**
     * Generates string of arbitrary length with contents composed of single character.
     *
     * @param length     length of the resulting string.
     * @param charToFill character to compose string of.
     * @return <code>String</code> of desired length.
     */
    public static String generateString(int length, char charToFill) {
        return new String(new char[length]).replace('\0', charToFill);
    }
}
