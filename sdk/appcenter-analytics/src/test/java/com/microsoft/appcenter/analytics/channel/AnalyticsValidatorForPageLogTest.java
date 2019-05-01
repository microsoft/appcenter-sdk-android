/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.PageLog;

import org.junit.Before;
import org.junit.Test;

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

public class AnalyticsValidatorForPageLogTest {

    private final AnalyticsValidator mAnalyticsValidator = new AnalyticsValidator();

    private PageLog mPageLog;

    @Before
    public void setUp() {
        mPageLog = new PageLog();
    }

    @Test
    public void shouldFilterNullName() {
        assertTrue(mAnalyticsValidator.shouldFilter(mPageLog));
    }

    @Test
    public void shouldFilterEmptyName() {
        mPageLog.setName("");
        assertTrue(mAnalyticsValidator.shouldFilter(mPageLog));
    }

    @Test
    public void shouldFilterWhitespaceName() {
        mPageLog.setName(" ");
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(" ", mPageLog.getName());
    }

    @Test
    public void shouldFilterMaxLengthName() {
        final String maxName = generateString(MAX_NAME_LENGTH, '*');
        mPageLog.setName(maxName);
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(maxName, mPageLog.getName());
        assertNull(mPageLog.getProperties());
    }

    @Test
    public void shouldFilterTooLongName() {
        final String maxName = generateString(MAX_NAME_LENGTH, '*');
        mPageLog.setName(maxName + '*');
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(maxName, mPageLog.getName());
        assertNull(mPageLog.getProperties());
    }

    @Test
    public void shouldFilterInvalidPropertyKeys() {
        final String validEventName = "eventName";
        mPageLog.setName(validEventName);
        mPageLog.setProperties(new HashMap<String, String>() {{
            put(null, null);
            put("", null);
            put(generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*'), null);
            put("1", null);
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(validEventName, mPageLog.getName());
        assertEquals(0, mPageLog.getProperties().size());
    }

    @Test
    public void shouldFilterTooManyProperties() {
        final String validEventName = "eventName";
        mPageLog.setName(validEventName);
        final String validMapItem = "valid";
        mPageLog.setProperties(new HashMap<String, String>() {{
            for (int i = 0; i < 30; i++) {
                put(validMapItem + i, validMapItem);
            }
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(validEventName, mPageLog.getName());
        assertEquals(MAX_PROPERTY_COUNT, mPageLog.getProperties().size());
    }

    @Test
    public void shouldFilterTooLongProperty() {
        final String validEventName = "eventName";
        mPageLog.setName(validEventName);
        final String longerMapItem = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        mPageLog.setProperties(new HashMap<String, String>() {{
            put(longerMapItem, longerMapItem);
        }});
        assertFalse(mAnalyticsValidator.shouldFilter(mPageLog));
        assertEquals(validEventName, mPageLog.getName());
        assertEquals(1, mPageLog.getProperties().size());
        Map.Entry<String, String> entry = mPageLog.getProperties().entrySet().iterator().next();
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, entry.getKey().length());
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, entry.getValue().length());
    }
}