package com.microsoft.appcenter.utils;

import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class SessionIdContextTest {

    @Before
    public void setUp() {
        SessionIdContext.unsetInstance();
    }

    @Test
    public void singleton() {
        assertNotNull(SessionIdContext.getInstance());
        assertSame(SessionIdContext.getInstance(), SessionIdContext.getInstance());
    }

    @Test
    public void sessionIdStartsNull() {
        assertNull(SessionIdContext.getInstance().getSessionId());
    }

    @Test
    public void refreshSessionId() {
        UUID initialId = SessionIdContext.getInstance().refreshSessionId();
        UUID refreshedId = SessionIdContext.getInstance().refreshSessionId();
        assertNotEquals(initialId, refreshedId);
    }

    @Test
    public void getSessionId() {
        UUID initialId = SessionIdContext.getInstance().refreshSessionId();
        UUID retrievedId = SessionIdContext.getInstance().getSessionId();
        assertEquals(initialId, retrievedId);
    }

    @Test
    public void invalidateSessionId() {
        SessionIdContext.getInstance().refreshSessionId();
        SessionIdContext.getInstance().invalidateSessionId();
        assertNull(SessionIdContext.getInstance().getSessionId());
    }
}
