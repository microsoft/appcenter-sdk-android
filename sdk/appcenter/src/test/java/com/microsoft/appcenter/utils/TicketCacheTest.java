/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TicketCacheTest {

    @Before
    public void setUp() {
        TicketCache.clear();
    }

    @After
    public void tearDown() {
        TicketCache.clear();
    }

    @Test
    public void coverInit() {
        new TicketCache();
    }

    @Test
    public void storeTickets() {
        assertNull(TicketCache.getTicket("key1"));
        TicketCache.putTicket("key1", "1");
        assertEquals("1", TicketCache.getTicket("key1"));
        TicketCache.putTicket("key1", "2");
        assertEquals("2", TicketCache.getTicket("key1"));
        TicketCache.putTicket("key2", "1");
        assertEquals("2", TicketCache.getTicket("key1"));
        assertEquals("1", TicketCache.getTicket("key2"));
    }
}
