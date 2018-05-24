package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class LocExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new LocExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        LocExtension a = new LocExtension();
        LocExtension b = new LocExtension();
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
