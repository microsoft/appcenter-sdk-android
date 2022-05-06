/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import static org.mockito.ArgumentMatchers.argThat;

import com.microsoft.appcenter.analytics.ingestion.models.LogWithNameAndProperties;
import com.microsoft.appcenter.ingestion.models.Log;

import org.mockito.ArgumentMatcher;

public class LogNameMatcher<T extends LogWithNameAndProperties> implements ArgumentMatcher<Log> {

    private final Class<T> mClazz;
    private final String mExpectedName;

    private LogNameMatcher(Class<T> clazz, String expectedName) {
        mClazz = clazz;
        mExpectedName = expectedName;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(Log log) {
        if (log != null && mClazz.isAssignableFrom(log.getClass())) {
            T pageLog = (T) log;
            return mExpectedName.equals(pageLog.getName());
        }
        return false;
    }

    public static <T extends LogWithNameAndProperties> Log logName(Class<T> clazz, String expectedName) {
        return argThat(new LogNameMatcher<T>(clazz, expectedName));
    }
}
