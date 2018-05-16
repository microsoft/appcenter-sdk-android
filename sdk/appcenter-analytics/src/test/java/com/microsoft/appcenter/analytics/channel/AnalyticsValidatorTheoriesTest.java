package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.LogWithNameAndProperties;
import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.analytics.ingestion.models.PageLog;

import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_NAME_LENGTH;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_COUNT;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_ITEM_LENGTH;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(Theories.class)
public class AnalyticsValidatorTheoriesTest {

    private AnalyticsValidator mAnalyticsValidator = new AnalyticsValidator();

    @DataPoint
    public static EventLog eventLog() {
        return new EventLog();
    }

    @DataPoint
    public static PageLog pageLog() {
        return new PageLog();
    }

    @Theory
    public void shouldFilterNullName(LogWithNameAndProperties log) {
        assertTrue(mAnalyticsValidator.shouldFilter(log));
    }

    @Theory
    public void shouldFilterEmptyName(LogWithNameAndProperties log) {
        log.setName("");
        assertTrue(mAnalyticsValidator.shouldFilter(log));
    }

    @Theory
    public void shouldFilterWhitespaceName(LogWithNameAndProperties log) {
        log.setName(" ");
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(" ", log.getName());
    }

    @Theory
    public void shouldFilterMaxLengthName(LogWithNameAndProperties log) {
        final String maxName = generateString(MAX_NAME_LENGTH, '*');
        log.setName(maxName);
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(maxName, log.getName());
        assertNull(log.getProperties());
    }

    @Theory
    public void shouldFilterTooLongName(LogWithNameAndProperties log) {
        final String maxName = generateString(MAX_NAME_LENGTH, '*');
        log.setName(maxName + '*');
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(maxName, log.getName());
        assertNull(log.getProperties());
    }

    @Theory
    public void shouldFilterInvalidPropertyKeys(LogWithNameAndProperties log) {
        final String validEventName = "eventName";
        log.setName(validEventName);
        log.setProperties(new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(validEventName, log.getName());
        assertEquals(0, log.getProperties().size());
    }

    @Theory
    public void shouldFilterTooManyProperties(LogWithNameAndProperties log) {
        final String validEventName = "eventName";
        log.setName(validEventName);
        final String validMapItem = "valid";
        log.setProperties(new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put(validMapItem + i, validMapItem);
            }
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(validEventName, log.getName());
        assertEquals(MAX_PROPERTY_COUNT, log.getProperties().size());
    }

    @Theory
    public void shouldFilterTooLongProperty(LogWithNameAndProperties log) {
        final String validEventName = "eventName";
        log.setName(validEventName);
        final String longerMapItem = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        log.setProperties(new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(log));
        assertEquals(validEventName, log.getName());
        assertEquals(1, log.getProperties().size());
        Map.Entry<String, String> entry = log.getProperties().entrySet().iterator().next();
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, entry.getKey().length());
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, entry.getValue().length());
    }
}