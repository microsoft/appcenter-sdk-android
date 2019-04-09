/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UserIdContextTest {

    @Test
    public void userIdInvalidForOneCollector() {
        assertFalse(UserIdContext.checkUserIdValidForOneCollector(""));
        assertFalse(UserIdContext.checkUserIdValidForOneCollector(":alice"));
        assertFalse(UserIdContext.checkUserIdValidForOneCollector("c:"));
        assertFalse(UserIdContext.checkUserIdValidForOneCollector("x:"));
        assertFalse(UserIdContext.checkUserIdValidForOneCollector("x:alice"));
    }

    @Test
    public void userIdValidForOneCollector() {
        assertTrue(UserIdContext.checkUserIdValidForOneCollector(null));
        assertTrue(UserIdContext.checkUserIdValidForOneCollector("c:alice"));
        assertTrue(UserIdContext.checkUserIdValidForOneCollector("alice"));
    }

    @Test
    public void userIdInvalidForAppCenter() {
        StringBuilder userId = new StringBuilder();
        for (int i = 0; i <= UserIdContext.USER_ID_APP_CENTER_MAX_LENGTH; i++) {
            userId.append("x");
        }
        assertFalse(UserIdContext.checkUserIdValidForAppCenter(userId.toString()));
    }

    @Test
    public void userIdValidForAppCenter() {
        assertTrue(UserIdContext.checkUserIdValidForAppCenter("alice"));
        assertTrue(UserIdContext.checkUserIdValidForAppCenter(null));
    }

    @Test
    public void prefixedInvalidUserId() {
        assertNull(UserIdContext.getPrefixedUserId(null));
        assertEquals("c:alice", UserIdContext.getPrefixedUserId("alice"));
        assertEquals(":", UserIdContext.getPrefixedUserId(":"));
    }

    @Test
    public void prefixedValidUserId() {
        assertEquals("c:alice", UserIdContext.getPrefixedUserId("c:alice"));
    }

    @Test
    public void setUserIdUserEquals() {
        String mockUserId = "userId";
        UserIdContext uic = new UserIdContext();
        final int[] countInvoke = {0};
        uic.addListener(new UserIdContext.Listener() {
            @Override
            public void onNewUserId(String userId) {
                countInvoke[0]++;
            }
        });
        uic.setUserId(mockUserId);
        assertEquals(countInvoke[0], 1);
        assertEquals(uic.getUserId(), mockUserId);
        uic.setUserId(mockUserId);
        assertEquals(countInvoke[0], 1);
        assertEquals(uic.getUserId(), mockUserId);
    }

    @Test
    public void setUserIdUserNotEquals() {
        String mockUserId1 = "userId1";
        String mockUserId2 = "userId2";
        UserIdContext uic = new UserIdContext();
        final int[] countInvoke = {0};
        uic.addListener(new UserIdContext.Listener() {
            @Override
            public void onNewUserId(String userId) {
                countInvoke[0]++;
            }
        });
        uic.setUserId(mockUserId1);
        assertEquals(countInvoke[0], 1);
        assertEquals(uic.getUserId(), mockUserId1);
        uic.setUserId(mockUserId2);
        assertEquals(countInvoke[0], 2);
        assertEquals(uic.getUserId(), mockUserId2);
    }

    @Test
    public void setUserIdUserNull() {
        String mockUserId = null;
        UserIdContext uic = new UserIdContext();
        final int[] countInvoke = {0};
        uic.addListener(new UserIdContext.Listener() {
            @Override
            public void onNewUserId(String userId) {
                countInvoke[0]++;
            }
        });
        uic.setUserId(mockUserId);
        assertEquals(countInvoke[0], 1);
        assertEquals(uic.getUserId(), mockUserId);
        uic.setUserId(mockUserId);
        assertEquals(uic.getUserId(), mockUserId);
    }
}
