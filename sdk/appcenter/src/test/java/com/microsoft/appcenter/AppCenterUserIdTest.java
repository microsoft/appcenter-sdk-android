/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter;

import com.microsoft.appcenter.utils.context.UserIdContext;

import org.junit.After;
import org.junit.Test;

import static com.microsoft.appcenter.AppCenter.PAIR_DELIMITER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AppCenterUserIdTest extends AbstractAppCenterTest {

    private static String longUserId() {
        StringBuilder userId = new StringBuilder();
        for (int i = 0; i <= UserIdContext.USER_ID_APP_CENTER_MAX_LENGTH; i++) {
            userId.append("x");
        }
        return userId.toString();
    }

    @After
    public void resetUserId() {
        UserIdContext.unsetInstance();
    }

    @Test
    public void setUserIdBeforeStart() {
        AppCenter.setUserId("test");
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setUserIdAfterStart() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("test");
        assertEquals("test", UserIdContext.getInstance().getUserId());

        /* Unset. */
        AppCenter.setUserId(null);
        assertNull(UserIdContext.getInstance().getUserId());

        /* Another value. */
        AppCenter.setUserId("test2");
        assertEquals("test2", UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setUserIdWithNoSecret() {
        AppCenter.configure(mApplication);
        AppCenter.setUserId("test");
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setUserIdFromLibrary() {
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setUserIdAfterStartedFromLibraryThenApplication() {
        AppCenter.startFromLibrary(mApplication, DummyService.class);
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        String userId = "test";
        AppCenter.setUserId(userId);
        assertEquals(userId, UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithAppSecret() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithTargetTokenSecret() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        String userId = "c:" + longUserId();
        AppCenter.setUserId(userId);
        assertEquals(userId, UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setLongUserIdWithBothSecrets() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        String userId = longUserId();
        AppCenter.setUserId(userId);
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithAppSecret() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("x:test");
        assertEquals("x:test", UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithTargetToken() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        AppCenter.setUserId("x:test");
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setInvalidOneCollectorUserIdWithBothSecrets() {
        String secret = DUMMY_TARGET_TOKEN_STRING + PAIR_DELIMITER +
                DUMMY_APP_SECRET;
        AppCenter.start(mApplication, secret, DummyService.class);
        AppCenter.setUserId("x:test");
        assertNull(UserIdContext.getInstance().getUserId());
    }

    @Test
    public void checkOneCollectorUserIdPatterns() {
        AppCenter.start(mApplication, DUMMY_TARGET_TOKEN_STRING, DummyService.class);
        AppCenter.setUserId("");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("c:");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("p:test");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("i:user@microsoft.com");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("d:adc44f2dc6915cc8823711a90dbe802a93845625cc3ad75bab6c033a230855c1");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("w:1BD8FC6E-98CE-E03D-B19D-BFD5A9BA712D");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId(":alice");
        assertNull(UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("alice");
        assertEquals("alice", UserIdContext.getInstance().getUserId());
        AppCenter.setUserId("c:alice");
        assertEquals("c:alice", UserIdContext.getInstance().getUserId());
    }

    @Test
    public void setInvalidUserIdDoesNotUpdatePreviousUserId() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("valid");
        AppCenter.setUserId(longUserId());
        assertEquals("valid", UserIdContext.getInstance().getUserId());
    }

    @Test
    public void unsetUserId() {
        AppCenter.start(mApplication, DUMMY_APP_SECRET, DummyService.class);
        AppCenter.setUserId("valid");
        assertEquals("valid", UserIdContext.getInstance().getUserId());
        AppCenter.setUserId(null);
        assertNull(UserIdContext.getInstance().getUserId());
    }
}
