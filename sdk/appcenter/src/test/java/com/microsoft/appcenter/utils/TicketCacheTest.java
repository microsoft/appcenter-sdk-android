package com.microsoft.appcenter.utils;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TicketCacheTest {

    private TicketCache mCache = TicketCache.getInstance();

    @Before
    public void setUp() {
        mCache.clearCache();
    }

    @Test
    public void storeTickets() {
        assertNull(mCache.getTicket("key1"));

        mCache.putTicket("key1", "1");
        assertEquals("1", mCache.getTicket("key1"));
        mCache.putTicket("key1", "2");
        assertEquals("2", mCache.getTicket("key1"));

        mCache.putTicket("key2", "1");
        assertEquals("2", mCache.getTicket("key1"));
        assertEquals("1", mCache.getTicket("key2"));
    }
}
