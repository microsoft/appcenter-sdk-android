package com.microsoft.appcenter.ingestion.models.one;

import com.microsoft.appcenter.test.TestUtils;

import org.junit.Test;

import static com.microsoft.appcenter.test.TestUtils.checkEquals;
import static com.microsoft.appcenter.test.TestUtils.checkNotEquals;

public class AppExtensionTest {

    @Test
    public void compareDifferentType() {
        TestUtils.compareSelfNullClass(new AppExtension());
    }

    @Test
    public void equalsHashCode() {

        /* Empty objects. */
        AppExtension a = new AppExtension();
        AppExtension b = new AppExtension();
        checkEquals(a, b);

        /* Id. */
        a.setId("a1");
        checkNotEquals(a, b);
        b.setId("b1");
        checkNotEquals(a, b);
        b.setId("a1");
        checkEquals(a, b);

        /* Ver. */
        a.setVer("a2");
        checkNotEquals(a, b);
        b.setVer("b2");
        checkNotEquals(a, b);
        b.setVer("a2");
        checkEquals(a, b);

        /* Locale. */
        a.setLocale("a3");
        checkNotEquals(a, b);
        b.setLocale("b3");
        checkNotEquals(a, b);
        b.setLocale("a3");
        checkEquals(a, b);
    }
}
