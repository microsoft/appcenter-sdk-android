package com.microsoft.appcenter.push;

import android.support.test.InstrumentationRegistry;

import org.junit.Test;

public class FirebaseCoverageTest {

    @Test
    @SuppressWarnings("MissingPermission")
    public void coverage() {

        /*
         * We can't verify interaction of this due to mock limitation and no getter.
         * Just execute the code without exception for coverage.
         */
        FirebaseAnalyticsUtils.setEnabled(InstrumentationRegistry.getTargetContext(), false);

        /* Also need to cover init... */
        new FirebaseAnalyticsUtils();
    }
}
