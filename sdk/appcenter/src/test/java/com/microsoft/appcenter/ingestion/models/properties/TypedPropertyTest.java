package com.microsoft.appcenter.ingestion.models.properties;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class TypedPropertyTest {

    @Test
    public void equalsHashCode() {
        TypedProperty a = new MockTypedProperty();
        TypedProperty b = new MockTypedProperty();
        checkNotEquals(a, null);
        checkNotEquals(a, new Object());
        checkEquals(a, a);
        checkEquals(a, b);
        a.setName("name");
        checkNotEquals(a, b);
        b.setName("name");
        checkEquals(a, b);
    }

    private static class MockTypedProperty extends TypedProperty {

        @Override
        public String getType() {
            return null;
        }
    }
}
