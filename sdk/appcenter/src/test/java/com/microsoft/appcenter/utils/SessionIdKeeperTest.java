package com.microsoft.appcenter.utils;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class SessionIdKeeperTest {

    @Before
    public void setUp() {
        SessionIdKeeper.unsetInstance();
    }

    @Test
    public void singleton() {
        assertNotNull(SessionIdKeeper.getInstance());
        assertSame(SessionIdKeeper.getInstance(), SessionIdKeeper.getInstance());
    }

    @Test
    public void sessionIdStartsNull() {
        assertNull(SessionIdKeeper.getInstance().getSessionId());
    }

    @Test
    public void refreshSessionId() {
        UUID initialId = SessionIdKeeper.getInstance().refreshSessionId();
        UUID refreshedId = SessionIdKeeper.getInstance().refreshSessionId();
        assertNotEquals(initialId, refreshedId);
    }

    @Test
    public void getSessionId() {
        UUID initialId = SessionIdKeeper.getInstance().refreshSessionId();
        UUID retrievedId = SessionIdKeeper.getInstance().getSessionId();
        assertEquals(initialId, retrievedId);
    }
}
