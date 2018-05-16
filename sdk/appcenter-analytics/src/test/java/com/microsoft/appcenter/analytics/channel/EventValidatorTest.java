package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.AnalyticsLog;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

public class EventValidatorTest {

    private EventValidator mEventValidator = new EventValidator();

    @Test
    public void shouldFilterInvalidType() {
        assertFalse(mEventValidator.shouldFilter(new AnalyticsLog() {

            @Override
            public String getType() {
                return "";
            }
        }));
    }
}
