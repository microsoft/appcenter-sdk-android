package com.microsoft.appcenter.utils;

import com.microsoft.appcenter.Constants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserIdUtilsTest {

    @Test
    public void utilsCoverage() {
        new UserIdUtils();
    }

    @Test
    public void userIdInvalidForOneCollector() {
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector(""));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector(":alice"));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector("c:"));
        assertFalse(UserIdUtils.checkUserIdValidForOneCollector("x:alice"));
    }

    @Test
    public void userIdValidForOneCollector() {
        assertTrue(UserIdUtils.checkUserIdValidForOneCollector(null));
        assertTrue(UserIdUtils.checkUserIdValidForOneCollector("c:alice"));
        assertTrue(UserIdUtils.checkUserIdValidForOneCollector("alice"));
    }

    @Test
    public void userIdInvalidForAppCenter() {
        StringBuilder userId = new StringBuilder();
        for (int i = 0; i <= UserIdUtils.USER_ID_APP_CENTER_MAX_LENGTH; i++) {
            userId.append("x");
        }
        assertFalse(UserIdUtils.checkUserIdValidForAppCenter(userId.toString()));
    }

    @Test
    public void userIdValidForAppCenter() {
        assertTrue(UserIdUtils.checkUserIdValidForAppCenter("alice"));
        assertTrue(UserIdUtils.checkUserIdValidForAppCenter(null));
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
