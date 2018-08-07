package com.microsoft.appcenter.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TicketCacheTest {

    public static void clearTicketCache() {
        TicketCache.getInstance().clearCache();
    }

    @Before
    public void setUp() {
        clearTicketCache();
    }

    @After
    public void tearDown() {
        clearTicketCache();
    }

    @Test
    public void storeTickets() {
        TicketCache cache = TicketCache.getInstance();
        assertNull(cache.getTicket("key1"));

        cache.putTicket("key1", "1");
        assertEquals("1", cache.getTicket("key1"));
        cache.putTicket("key1", "2");
        assertEquals("2", cache.getTicket("key1"));

        cache.putTicket("key2", "1");
        assertEquals("2", cache.getTicket("key1"));
        assertEquals("1", cache.getTicket("key2"));
    }
}
