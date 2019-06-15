/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.os.Bundle;

import java.lang.reflect.Method;

/**
 * Wraps InstrumentationRegistry class to enable mocking in unit tests.
 */
public class InstrumentationRegistryHelper {

    private static final String[] LOCATIONS = new String[]{"androidx.test.platform.app.InstrumentationRegistry",
            "androidx.test.InstrumentationRegistry",
            "android.support.test.InstrumentationRegistry"};

    /**
     * Get the instrumentation arguments from the InstrumentationRegistry. Wrapper exists for unit
     * tests.
     *
     * @return the instrumentation arguments.
     * @throws IllegalStateException if it cannot call getArguments using any of the provided classes.
     */
    public static Bundle getArguments() throws IllegalStateException {
        Exception exception = null;
        for (String location : LOCATIONS) {
            try {
                Class<?> aClass = getClass(location);
                Method getArguments = aClass.getMethod("getArguments");
                return (Bundle) getArguments.invoke(null);
            } catch (Exception e) {
                exception = e;
            }
        }
        throw new IllegalStateException(exception);
    }

    /**
     * Get class by name. Separate method so that it can be overridden by mocks in unit tests. Do not inline.
     *
     * @param className class name to resolve.
     * @return the Class object from name.
     * @throws ClassNotFoundException if the class could no be found.
     */
    private static Class<?> getClass(String className) throws ClassNotFoundException {
        return Class.forName(className);
    }
}
