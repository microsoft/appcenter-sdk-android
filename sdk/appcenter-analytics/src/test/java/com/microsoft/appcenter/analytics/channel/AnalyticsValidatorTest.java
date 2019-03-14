/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.ingestion.models.LogWithNameAndProperties;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * The full test logic for AnalyticsValidator is split into different test files.
 *
 * @see AnalyticsValidatorForEventLogTest
 * @see AnalyticsValidatorForPageLogTest
 */
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
