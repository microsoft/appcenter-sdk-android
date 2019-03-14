/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@PrepareForTest(AppCenterLog.class)
public class EventPropertiesTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void validKeys() {
        EventProperties properties = new EventProperties();
        properties.set("t1", "test");
        properties.set("t2", new Date(0));
        properties.set("t3", (long) 0);
        properties.set("t4", 0.1);
        properties.set("t5", false);
        assertEquals(5, properties.getProperties().size());
        verifyStatic(never());
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void nullKeyValidation() {
        EventProperties properties = new EventProperties();
        properties.set(null, "test");
        properties.set(null, new Date(0));
        properties.set(null, (long) 0);
        properties.set(null, 0.1);
        properties.set(null, false);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(5));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void warningWhenOverridingKeys() {
        EventProperties properties = new EventProperties();
        properties.set("t1", "test");
        properties.set("t1", new Date(0));
        assertEquals(1, properties.getProperties().size());
        verifyStatic(never());
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
        verifyStatic();
        AppCenterLog.warn(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setString() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        properties.set(key, (String) null);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Normal value. */
        String normalValue = "test";
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        StringTypedProperty expected = new StringTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        assertEquals(expected, properties.getProperties().get(key));
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setDate() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        properties.set(key, (Date) null);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Normal value. */
        Date normalValue = new Date(0);
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        DateTimeTypedProperty expected = new DateTimeTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        assertEquals(expected, properties.getProperties().get(key));
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setLong() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Normal value. */
        long normalValue = 0;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        LongTypedProperty expected = new LongTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        assertEquals(expected, properties.getProperties().get(key));
        verifyStatic(never());
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setDouble() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* NaN value. */
        double nanValue = Double.NaN;
        properties = new EventProperties();
        properties.set(key, nanValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Positive infinity value. */
        double positiveInfinityValue = Double.POSITIVE_INFINITY;
        properties = new EventProperties();
        properties.set(key, positiveInfinityValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Negative infinity value. */
        double negativeInfinityValue = Double.NEGATIVE_INFINITY;
        properties = new EventProperties();
        properties.set(key, negativeInfinityValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(3));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Normal value. */
        double normalValue = 0.0;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        DoubleTypedProperty expected = new DoubleTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        verifyStatic(times(3));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setBoolean() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Normal value. */
        properties.set(key, false);
        assertEquals(1, properties.getProperties().size());
        BooleanTypedProperty expected = new BooleanTypedProperty();
        expected.setName(key);
        expected.setValue(false);
        verifyStatic(never());
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }
}