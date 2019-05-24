/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.os.Bundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Wraps InstrumentationRegistry class to enable mocking in unit tests.
 */
public class InstrumentationRegistryHelper {

    private static final List<String> LOCATIONS = Arrays.asList("androidx.test.platform.app.InstrumentationRegistry",
            "androidx.test.InstrumentationRegistry",
            "android.support.test.InstrumentationRegistry");

    /**
     * Get the instrumentation arguments from the InstrumentationRegistry. Wrapper exists for unit
     * tests.
     *
     * @return the instrumentation arguments.
     * @throws LinkageError          if the class, method is not found or does not match, typically no test dependencies in release.
     */
    public static Bundle getArguments() throws LinkageError, IllegalStateException {
        Iterator<String> iterator = LOCATIONS.iterator();
        while (iterator.hasNext()) {
            String location = iterator.next();

            try {
                Class<?> aClass = Class.forName(location);
                Method getArguments = aClass.getMethod("getArguments", (Class[]) null);
                return (Bundle) getArguments.invoke(null, (Object[]) null);
            } catch (IllegalStateException e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            } catch (LinkageError e) {
                if (!iterator.hasNext()) {
                    throw e;
                }
            } catch (Exception ignored) {
            }
        }

        return new Bundle();
    }
}
