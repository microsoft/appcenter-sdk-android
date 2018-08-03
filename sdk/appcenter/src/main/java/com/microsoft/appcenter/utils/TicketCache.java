package com.microsoft.appcenter.utils;

import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

public class TicketCache {

    private static final TicketCache sInstance = new TicketCache();

    private Map<String, String> mTickets = new HashMap<>();

    public static TicketCache getInstance() {
        return sInstance;
    }

    public String getTicket(String key) {
        return mTickets.get(key);
    }

    public void putTicket(String key, String value) {
        mTickets.put(key, value);
    }

    @VisibleForTesting
    void clearCache() {
        mTickets.clear();
    }
}
