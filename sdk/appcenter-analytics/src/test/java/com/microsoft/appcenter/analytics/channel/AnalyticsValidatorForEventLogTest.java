package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.EventLog;
import com.microsoft.appcenter.ingestion.models.properties.DoubleTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.StringTypedProperty;
import com.microsoft.appcenter.ingestion.models.properties.TypedProperty;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_NAME_LENGTH;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_COUNT;
import static com.microsoft.appcenter.analytics.channel.AnalyticsValidator.MAX_PROPERTY_ITEM_LENGTH;
import static com.microsoft.appcenter.test.TestUtils.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
    public void shouldFilterTooLongStringTypedProperty() {
        String validEventName = "eventName";
        mEventLog.setName(validEventName);
        String longerPropertyItem = generateString(MAX_PROPERTY_ITEM_LENGTH + 1, '*');
        StringTypedProperty property = new StringTypedProperty();
        property.setName(longerPropertyItem);
        property.setValue(longerPropertyItem);
        mEventLog.setTypedProperties(Collections.<TypedProperty>singletonList(property));
        assertFalse(mAnalyticsValidator.shouldFilter(mEventLog));
        assertEquals(validEventName, mEventLog.getName());
        assertEquals(1, mEventLog.getTypedProperties().size());
        StringTypedProperty stringProperty = (StringTypedProperty)mEventLog.getTypedProperties().iterator().next();
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, stringProperty.getName().length());
        assertEquals(MAX_PROPERTY_ITEM_LENGTH, stringProperty.getValue().length());
    }
}