/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SharedPreferencesManagerAndroidTest {

    @BeforeClass
    public static void setUpClass() {
        Context sContext = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(sContext);
    }

    @AfterClass
    public static void tearDownClass() {

        /* Clean up shared preferences. */
        try {
            for (SharedPreferencesTestData data : generateSharedPreferenceData()) {
                String key = data.value.getClass().getName();
                SharedPreferencesManager.remove(key);
            }
        } catch (NoSuchMethodException ignored) {
            /* Ignore exception. */
        }
    }

    private static SharedPreferencesTestData[] generateSharedPreferenceData() throws NoSuchMethodException {
        SharedPreferencesTestData[] testData = new SharedPreferencesTestData[6];

        /* boolean */
        testData[0] = new SharedPreferencesTestData();
        testData[0].value = true;
        testData[0].defaultValue = false;
        testData[0].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getBoolean", String.class);
        testData[0].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getBoolean", String.class, boolean.class);
        testData[0].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putBoolean", String.class, boolean.class);

        /* float */
        testData[1] = new SharedPreferencesTestData();
        testData[1].value = 111.22f;
        testData[1].defaultValue = 0.01f;
        testData[1].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getFloat", String.class);
        testData[1].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getFloat", String.class, float.class);
        testData[1].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putFloat", String.class, float.class);

        /* int */
        testData[2] = new SharedPreferencesTestData();
        testData[2].value = 123;
        testData[2].defaultValue = -1;
        testData[2].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getInt", String.class);
        testData[2].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getInt", String.class, int.class);
        testData[2].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putInt", String.class, int.class);

        /* long */
        testData[3] = new SharedPreferencesTestData();
        testData[3].value = 123456789000L;
        testData[3].defaultValue = 345L;
        testData[3].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getLong", String.class);
        testData[3].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getLong", String.class, long.class);
        testData[3].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putLong", String.class, long.class);

        /* String */
        testData[4] = new SharedPreferencesTestData();
        testData[4].value = "Hello World";
        testData[4].defaultValue = "Empty";
        testData[4].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getString", String.class);
        testData[4].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getString", String.class, String.class);
        testData[4].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putString", String.class, String.class);

        /* Set<String> */
        Set<String> data = new HashSet<>();
        data.add("ABC");
        data.add("Hello World");
        data.add("Welcome to the world!");
        Set<String> defaultSet = new HashSet<>();
        defaultSet.add("DEFAULT");

        testData[5] = new SharedPreferencesTestData();
        testData[5].value = data;
        testData[5].defaultValue = defaultSet;
        testData[5].getMethod1 = SharedPreferencesManager.class.getDeclaredMethod("getStringSet", String.class);
        testData[5].getMethod2 = SharedPreferencesManager.class.getDeclaredMethod("getStringSet", String.class, Set.class);
        testData[5].putMethod = SharedPreferencesManager.class.getDeclaredMethod("putStringSet", String.class, Set.class);

        return testData;
    }

    @Test
    public void sharedPreferences() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        for (SharedPreferencesTestData data : generateSharedPreferenceData()) {

            /* Put value to shared preferences. */
            String key = data.value.getClass().getName();
            data.putMethod.invoke(null, key, data.value);

            /* Get value from shared preferences. */
            Object actual = data.getMethod1.invoke(null, key);

            /* Verify the value is same as assigned. */
            assertEquals(data.value, actual);

            /* Remove key from shared preferences. */
            SharedPreferencesManager.remove(key);

            /* Verify the value equals to default value. */
            assertEquals(data.defaultValue, data.getMethod2.invoke(null, key, data.defaultValue));
        }

        /* Test clear. */
        SharedPreferencesManager.putString("test", "someTest");
        SharedPreferencesManager.putInt("test2", 2);
        SharedPreferencesManager.clear();
        assertNull(SharedPreferencesManager.getString("test"));
        assertEquals(0, SharedPreferencesManager.getInt("test2"));
    }

    /**
     * Temporary class for testing shared preferences.
     */
    private static class SharedPreferencesTestData {
        Object value;
        Object defaultValue;
        Method getMethod1;
        Method getMethod2;
        Method putMethod;
    }
}