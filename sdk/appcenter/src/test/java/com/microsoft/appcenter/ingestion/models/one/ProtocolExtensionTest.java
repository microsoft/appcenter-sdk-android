package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class ProtocolExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new ProtocolExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        ProtocolExtension a = new ProtocolExtension();
        ProtocolExtension b = new ProtocolExtension();
        checkEquals(a, b);

        /* Dev make. */
        a.setDevMake("a1");
        checkNotEquals(a, b);
        b.setDevMake("b1");
        checkNotEquals(a, b);
        b.setDevMake("a1");
        checkEquals(a, b);

        /* Dev model. */
        a.setDevModel("a2");
        checkNotEquals(a, b);
        b.setDevModel("b2");
        checkNotEquals(a, b);
        b.setDevModel("a2");
        checkEquals(a, b);
    }
}
