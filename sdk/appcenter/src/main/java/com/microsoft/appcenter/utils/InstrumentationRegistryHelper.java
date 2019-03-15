/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

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
     * @throws LinkageError          if the class, method is not found or does not match, typically no test dependencies in release.
     * @throws IllegalStateException if no argument Bundle has been registered.
     */
    public static Bundle getArguments() throws LinkageError, IllegalStateException {
        return InstrumentationRegistry.getArguments();
    }
}
