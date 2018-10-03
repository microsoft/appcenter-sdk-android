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

@PrepareForTest({
        AppCenterLog.class
})
public class EventPropertiesTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void keyValidate() {
        String string = "test";
        Date date = new Date(0);
        long longNumber = 0;
        double doubleNumber = 0.1;
        boolean bool = false;
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null key. */
        String nullKey = null;
        properties.set(nullKey, string);
        properties.set(nullKey, date);
        properties.set(nullKey, longNumber);
        properties.set(nullKey, doubleNumber);
        properties.set(nullKey, bool);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(5));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Normal keys. */
        properties.set("t1", string);
        properties.set("t2", date);
        properties.set("t3", longNumber);
        properties.set("t4", doubleNumber);
        properties.set("t5", bool);
        assertEquals(5, properties.getProperties().size());
        verifyStatic(times(5));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Already contains keys. */
        properties.set("t1", string);
        properties.set("t2", date);
        properties.set("t3", longNumber);
        properties.set("t4", doubleNumber);
        properties.set("t5", bool);
        assertEquals(5, properties.getProperties().size());
        verifyStatic(times(5));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
        verifyStatic(times(5));
        AppCenterLog.warn(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setString() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        String nullValue = null;
        properties.set(key, nullValue);
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
        Date nullValue = null;
        properties.set(key, nullValue);
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
        double positiveInfinityValue = Double.NaN;
        properties = new EventProperties();
        properties.set(key, positiveInfinityValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* NaN value. */
        double negativeInfinityValue = Double.NaN;
        properties = new EventProperties();
        properties.set(key, negativeInfinityValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());

        /* Normal value. */
        double normalValue = 0.0;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        DoubleTypedProperty expected = new DoubleTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        verifyStatic(times(2));
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }

    @Test
    public void setBoolean() {
        String key = "test";
        EventProperties properties = new EventProperties();
        assertEquals(0, properties.getProperties().size());

        /* Normal value. */
        boolean normalValue = false;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        BooleanTypedProperty expected = new BooleanTypedProperty();
        expected.setName(key);
        expected.setValue(normalValue);
        verifyStatic(never());
        AppCenterLog.error(eq(Analytics.LOG_TAG), anyString());
    }
}