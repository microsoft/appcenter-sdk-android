package com.microsoft.appcenter.utils;

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;

/**
 * Wraps InstrumentationRegistry class to enable mocking in unit tests.
 */
public class InstrumentationRegistryHelper {
    public static Bundle getArguments() throws NoClassDefFoundError, IllegalAccessError {
        return InstrumentationRegistry.getArguments();
    }
}
