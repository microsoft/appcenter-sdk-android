package com.microsoft.appcenter.utils;

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

/**
 * Wraps InstrumentationRegistry class to enable mocking in unit tests.
 */
public class InstrumentationRegistryHelper {

    /**
     * Get the instrumentation arguments from the InstrumentationRegistry. Wrapper exists for unit
     * tests.
     *
     * @return the instrumentation arguments
     */
    public static Bundle getArguments() throws NoClassDefFoundError, NoSuchMethodError, IllegalAccessError {
        return InstrumentationRegistry.getArguments();
    }
}
