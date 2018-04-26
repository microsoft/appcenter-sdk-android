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
     * @return the instrumentation arguments.
     * @throws NoClassDefFoundError     if the class is not found, typically no test dependencies in release.
     * @throws NoSuchMethodError        if method not found, can happen with proguard.
     * @throws IllegalAccessError       if a reflection error happens.
     * @throws IllegalArgumentException if no argument Bundle has been registered.
     */
    public static Bundle getArguments() throws NoClassDefFoundError, NoSuchMethodError, IllegalAccessError, IllegalArgumentException {
        return InstrumentationRegistry.getArguments();
    }
}
