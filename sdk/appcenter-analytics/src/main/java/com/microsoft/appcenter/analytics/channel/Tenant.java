package com.microsoft.appcenter.analytics.channel;

import com.microsoft.appcenter.analytics.Analytics;

import java.util.Map;

public class Tenant extends Object {

    public String tenantId;

    public Tenant(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Track a custom event with name.
     *
     * @param name An event name.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue"})
    public void trackEvent(String name) {
        Analytics.trackEvent(name, this);
    }

    /**
     * Track a custom event with name and optional properties.
     * The name parameter can not be null or empty. Maximum allowed length = 256.
     * The properties parameter maximum item count = 5.
     * The properties keys can not be null or empty, maximum allowed key length = 64.
     * The properties values can not be null, maximum allowed value length = 64.
     * Any length of name/keys/values that are longer than each limit will be truncated.
     *
     * @param name       An event name.
     * @param properties Optional properties.
     */
    @SuppressWarnings("WeakerAccess")
    public void trackEvent(String name, Map<String, String> properties) {
        Analytics.trackEvent(name, properties, this);
    }
}
