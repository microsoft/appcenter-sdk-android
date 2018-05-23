package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class LocationExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new LocationExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        LocationExtension a = new LocationExtension();
        LocationExtension b = new LocationExtension();
        checkEquals(a, b);

        /* Timezone. */
        a.setTimeZone("a1");
        checkNotEquals(a, b);
        b.setTimeZone("b1");
        checkNotEquals(a, b);
        b.setTimeZone("a1");
        checkEquals(a, b);
    }
}
