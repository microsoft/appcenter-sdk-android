/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.analytics;

import com.microsoft.appcenter.analytics.channel.AnalyticsListener;

import java.lang.reflect.Method;
import java.util.Map;

public final class AnalyticsPrivateHelper {

    private AnalyticsPrivateHelper() {
    }

    public static void setListener(AnalyticsListener listener) {
        try {
            Analytics.setListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void trackPage(String name, Map<String, String> properties) {
        Analytics.trackPage(name, properties);
    }

    public static boolean isAutoPageTrackingEnabled() {
        return Analytics.isAutoPageTrackingEnabled();
    }

    public static void setAutoPageTrackingEnabled(boolean autoPageTrackingEnabled) {
        Analytics.setAutoPageTrackingEnabled(autoPageTrackingEnabled);
    }
}
