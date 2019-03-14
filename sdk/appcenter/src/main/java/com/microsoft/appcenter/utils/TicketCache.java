/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

/**
 * Ticket cache for One Collector protocol.
 */
public class TicketCache {

    /**
     * Cache backed by a simple map.
     */
    private static final Map<String, String> sTickets = new HashMap<>();

    /**
     * Get cached ticket.
     *
     * @param key ticket key.
     * @return token value or null if not in cache.
     */
    public static String getTicket(String key) {
        return sTickets.get(key);
    }

    /**
     * Insert or update ticket.
     *
     * @param key   ticket key.
     * @param value ticket value.
     */
    public static void putTicket(String key, String value) {
        sTickets.put(key, value);
    }

    @VisibleForTesting
    public static void clear() {
        sTickets.clear();
    }
}
