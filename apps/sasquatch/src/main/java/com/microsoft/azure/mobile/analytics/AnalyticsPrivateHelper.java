package com.microsoft.azure.mobile.analytics;

import java.util.Map;

public final class AnalyticsPrivateHelper {

    private AnalyticsPrivateHelper() {
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
