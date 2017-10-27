package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.util.Date;

import static com.microsoft.appcenter.test.TestUtils.generateString;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@SuppressWarnings({"unused", "ConstantConditions"})
@PrepareForTest({
        AppCenterLog.class
})
public class CustomPropertiesTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Before
    public void setUp() throws Exception {
        mockStatic(AppCenterLog.class);
    }

    @Test
    public void keyValidate() {
        String string = "test";
        Date date = new Date(0);
        Number number = 0;
        boolean bool = false;
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null key. */
        String nullKey = null;
        properties.set(nullKey, string);
        properties.set(nullKey, date);
        properties.set(nullKey, number);
        properties.set(nullKey, bool);
        properties.clear(nullKey);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(5));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Invalid key. */
        String invalidKey = "!";
        properties.set(invalidKey, string);
        properties.set(invalidKey, date);
        properties.set(invalidKey, number);
        properties.set(invalidKey, bool);
        properties.clear(invalidKey);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(10));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Too long key. */
        String tooLongKey = generateString(CustomProperties.MAX_PROPERTY_KEY_LENGTH + 1, 'a');
        properties.set(tooLongKey, string);
        properties.set(tooLongKey, date);
        properties.set(tooLongKey, number);
        properties.set(tooLongKey, bool);
        properties.clear(tooLongKey);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(15));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal keys. */
        properties.set("t1", string);
        properties.set("t2", date);
        properties.set("t3", number);
        properties.set("t4", bool);
        properties.clear("t5");
        assertEquals(5, properties.getProperties().size());
        verifyStatic(times(15));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal max length keys. */
        String maxLengthKey = generateString(CustomProperties.MAX_PROPERTY_KEY_LENGTH - 1, 'a');
        properties.set(maxLengthKey + "1", string);
        properties.set(maxLengthKey + "2", date);
        properties.set(maxLengthKey + "3", number);
        properties.set(maxLengthKey + "4", bool);
        properties.clear(maxLengthKey + "5");
        assertEquals(10, properties.getProperties().size());
        verifyStatic(times(15));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Already contains keys. */
        properties.set("t1", string);
        properties.set("t2", date);
        properties.set("t3", number);
        properties.set("t4", bool);
        properties.clear("t5");
        assertEquals(10, properties.getProperties().size());
        verifyStatic(times(15));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
        verifyStatic(times(5));
        AppCenterLog.warn(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void setString() {
        String key = "test";
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        String nullValue = null;
        properties.set(key, nullValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Too long value. */
        String tooLongValue = generateString(CustomProperties.MAX_PROPERTY_KEY_LENGTH + 1, 'a');
        properties.set(key, tooLongValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal value. */
        String normalValue = "test";
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal value with max length. */
        String maxLengthValue = generateString(CustomProperties.MAX_PROPERTY_KEY_LENGTH, 'a');
        properties.set(key + "1", maxLengthValue);
        assertEquals(2, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void setDate() {
        String key = "test";
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        Date nullValue = null;
        properties.set(key, nullValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal value. */
        Date normalValue = new Date(0);
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void setNumber() {
        String key = "test";
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());

        /* Null value. */
        Number nullValue = null;
        properties.set(key, nullValue);
        assertEquals(0, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Normal value. */
        Number normalValue = 0;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void setBoolean() {
        String key = "test";
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());

        /* Normal value. */
        boolean normalValue = false;
        properties.set(key, normalValue);
        assertEquals(1, properties.getProperties().size());
        verifyStatic(never());
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void clear() {
        String key = "test";
        CustomProperties properties = new CustomProperties();
        assertEquals(0, properties.getProperties().size());
        properties.clear(key);
        assertEquals(1, properties.getProperties().size());
        verifyStatic(never());
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }

    @Test
    public void maxPropertiesCount(){
        CustomProperties properties = new CustomProperties();
        for (int i = 0; i < CustomProperties.MAX_PROPERTIES_COUNT; i++){
            properties.set("key" + i, "value" + i);
        }
        assertEquals(CustomProperties.MAX_PROPERTIES_COUNT, properties.getProperties().size());

        /* Add extra property 1. */
        properties.set("over1", "value");
        assertEquals(CustomProperties.MAX_PROPERTIES_COUNT, properties.getProperties().size());
        verifyStatic(times(1));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());

        /* Add extra property 2. */
        properties.set("over2", "value");
        assertEquals(CustomProperties.MAX_PROPERTIES_COUNT, properties.getProperties().size());
        verifyStatic(times(2));
        AppCenterLog.error(eq(AppCenter.LOG_TAG), anyString());
    }
}