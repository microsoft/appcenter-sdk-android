package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.LogWithNameAndProperties;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class AnalyticsValidatorTest {

    private final AnalyticsValidator mAnalyticsValidator = new AnalyticsValidator();

    @Test
    public void shouldFilterInvalidType() {
        assertFalse(mAnalyticsValidator.shouldFilter(new LogWithNameAndProperties() {

            @Override
            public String getType() {
                return "";
            }
        }));
    }
}
