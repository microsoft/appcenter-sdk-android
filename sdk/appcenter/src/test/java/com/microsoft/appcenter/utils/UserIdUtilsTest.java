package com.microsoft.appcenter.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unused")
public class UserIdUtilsTest {

    @Test
    public void userIdInvalid() {
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector(null));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector(""));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector(":alice"));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector("c:"));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector("x:alice"));
    }

    @Test
    public void userIdValid() {
        assertTrue(UserIdUtils.checkUserIdValidForOneCollector("c:alice"));
    }

    @Test
    public void prefixedInvalidUserId() {
        assertNull(UserIdUtils.getPrefixedUserId(null));
        assertEquals("c:alice", UserIdUtils.getPrefixedUserId("alice"));
        assertEquals(":", UserIdUtils.getPrefixedUserId(":"));
    }

    @Test
    public void prefixedValidUserId() {
        assertEquals("c:alice", UserIdUtils.getPrefixedUserId("c:alice"));
    }
}
