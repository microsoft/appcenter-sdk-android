/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.ingestion.models.properties.BooleanTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DateTimeTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.LongTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_NAME_LENGTH;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_COUNT;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_ITEM_LENGTH;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AnalyticsValidatorForEventLogTest {

    private final AnalyticsValidator mAnalyticsValidator = new AnalyticsValidator();

    private EventLog mEventLog;

    @Before
    public void setUp() {
        mEventLog = new EventLog();
    }

    @Test
    public void shouldFilterNullName() {
        assertTrue(mAnalyticsValidator.shouldFilter(mEventLog));
    }

    @Test
    public void shouldFilterEmptyName() {
        mEventLog.setName("");
        assertTrue(mAnalyticsValidator.shouldFilter(mEventLog));
    }

    @Test
    public void shouldFilterWhitespaceName() {
        mEventLog.setName(" ");
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(" ", mEventLog.getName());
    }

    @Test
    public void shouldFilterMaxLengthName() {
        String maxName = generateString(MAX_NAME_LENGTH, '*');
        mEventLog.setName(maxName);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(maxName, mEventLog.getName());
        assertNull(mEventLog.getProperties());
    }

    @Test
    public void shouldFilterTooLongName() {
        String maxName = generateString(MAX_NAME_LENGTH, '*');
        mEventLog.setName(maxName + '*');
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(maxName, mEventLog.getName());
        assertNull(mEventLog.getProperties());
    }

    @Test
    public void shouldFilterInvalidPropertyKeys() {
        final String validEventName = "eventName";
        mEventLog.setName(validEventName);
        List<TypedProperty> properties = new ArrayList<>();

        /* null, null property. */
        StringTypedProperty property = new StringTypedProperty();
        property.setName(null);
        property.setValue(null);
        properties.add(property);

        /* Empty string, null property. */
        property = new StringTypedProperty();
        property.setName("");
        property.setValue(null);
        properties.add(property);

        /* Long string, null property. */
        property = new StringTypedProperty();
        property.setName(generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*'));
        property.setValue(null);
        properties.add(property);

        /* Invalid string, null property. */
        property = new StringTypedProperty();
        property.setName("1");
        property.setValue(null);
        properties.add(property);

        /* Set typed properties. */
        mEventLog.setTypedProperties(properties);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(0, mEventLog.getTypedProperties().size());
    }

    @Test
    public void shouldFilterTooManyProperties() {
        final String validEventName = "eventName";
        mEventLog.setName(validEventName);
        final String validPropertyItem = "valid";
        List<TypedProperty> properties = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            StringTypedProperty property = new StringTypedProperty();
            property.setName(validPropertyItem + i);
            property.setValue(validPropertyItem);
            properties.add(property);
        }
        mEventLog.setTypedProperties(properties);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(MAX_PROPERTY_COUNT, mEventLog.getTypedProperties().size());
    }

    @Test
    public void shouldFilterTooLongStringTypedPropertyStringValue() {
        String validEventName = "eventName";
        mEventLog.setName(validEventName);
        String longerPropertyItem = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        StringTypedProperty originalProperty = new StringTypedProperty();
        originalProperty.setName("regularName");
        originalProperty.setValue(longerPropertyItem);
        List<TypedProperty> typedProperties = new ArrayList<>();
        typedProperties.add(originalProperty);
        mEventLog.setTypedProperties(typedProperties);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(1, mEventLog.getTypedProperties().size());
        StringTypedProperty stringProperty = (StringTypedProperty) mEventLog.getTypedProperties().iterator().next();
        assertEquals("regularName", stringProperty.getName());
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, stringProperty.getValue().length());

        /* Verify original property value reference was not modified. */
        assertSame(longerPropertyItem, originalProperty.getValue());
    }

    @Test
    public void shouldFilterTooLongStringTypedPropertyKeyAndValue() {
        String validEventName = "eventName";
        mEventLog.setName(validEventName);
        String longerPropertyItem = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        StringTypedProperty originalProperty = new StringTypedProperty();
        originalProperty.setName(longerPropertyItem);
        originalProperty.setValue(longerPropertyItem);
        List<TypedProperty> typedProperties = new ArrayList<>();
        typedProperties.add(originalProperty);
        mEventLog.setTypedProperties(typedProperties);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(1, mEventLog.getTypedProperties().size());
        StringTypedProperty stringProperty = (StringTypedProperty) mEventLog.getTypedProperties().iterator().next();
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, stringProperty.getName().length());
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, stringProperty.getValue().length());

        /* Verify original property value references were not modified. */
        assertSame(longerPropertyItem, originalProperty.getName());
        assertSame(longerPropertyItem, originalProperty.getValue());
    }

    @Test
    public void shouldFilterTooLongPropertyKeysOfAnyType() {

        /* Set event name. */
        String validEventName = "eventName";
        mEventLog.setName(validEventName);

        /* Use a long key for every property type. */
        String longKey = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        StringTypedProperty originalStringProperty = new StringTypedProperty();
        originalStringProperty.setName(longKey);
        originalStringProperty.setValue("does not matter");
        BooleanTypedProperty originalBoolProperty = new BooleanTypedProperty();
        originalBoolProperty.setName(longKey);
        originalBoolProperty.setValue(true);
        DoubleTypedProperty originalDoubleProperty = new DoubleTypedProperty();
        originalDoubleProperty.setName(longKey);
        originalDoubleProperty.setValue(3.14);
        LongTypedProperty originalLongProperty = new LongTypedProperty();
        originalLongProperty.setName(longKey);
        originalLongProperty.setValue(-123);
        DateTimeTypedProperty originalDateProperty = new DateTimeTypedProperty();
        originalDateProperty.setName(longKey);
        originalDateProperty.setValue(new Date());

        /* Submit property list to validation, should pass with truncate. */
        List<TypedProperty> typedProperties = new ArrayList<>();
        typedProperties.add(originalStringProperty);
        typedProperties.add(originalBoolProperty);
        typedProperties.add(originalDoubleProperty);
        typedProperties.add(originalLongProperty);
        typedProperties.add(originalDateProperty);
        mEventLog.setTypedProperties(typedProperties);
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));

        /* Check name and number of property not modified. */
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(5, mEventLog.getTypedProperties().size());

        /* Verify all property names truncated. */
        for (TypedProperty property : mEventLog.getTypedProperties()) {
            assertEquals(MAX_PROPERTY_ITEM_LENGTH, property.getName().length());
        }

        /* Check all property changes were made by copy. */
        assertSame(longKey, originalStringProperty.getName());
        assertSame(longKey, originalBoolProperty.getName());
        assertSame(longKey, originalDoubleProperty.getName());
        assertSame(longKey, originalLongProperty.getName());
        assertSame(longKey, originalDateProperty.getName());
    }

    /**
     * This test case only makes up missing branch in validateProperties when TypedProperty isn't StringTypedProperty.
     */
    @Test
    public void shouldNotFilterNonStringTypedProperty() {
        String validEventName = "eventName";
        mEventLog.setName(validEventName);
        BooleanTypedProperty property = new BooleanTypedProperty();
        property.setName("name");
        property.setValue(true);
        mEventLog.setTypedProperties(Collections.<TypedProperty>singletonList(property));
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(1, mEventLog.getTypedProperties().size());
        BooleanTypedProperty booleanProperty = (BooleanTypedProperty) mEventLog.getTypedProperties().iterator().next();
        assertEquals("name", booleanProperty.getName());
        assertTrue(booleanProperty.getValue());
    }
}