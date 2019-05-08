/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.text.TextUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest(TextUtils.class)
@RunWith(PowerMockRunner.class)
public class UserIdContextTest {

    @Before
    public void setUp() {
        mockStatic(TextUtils.class);
        when(TextUtils.equals(any(CharSequence.class), any(CharSequence.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                CharSequence b = (CharSequence) invocation.getArguments()[1];
                return Objects.equals(a, b);
            }
        });
    }

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

    @SuppressWarnings("ConstantConditions")
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

        /* When we add a listener then set userId. */
        String mockUserId = "userId";
        UserIdContext userIdContext = new UserIdContext();
        UserIdContext.Listener listener = mock(UserIdContext.Listener.class);
        userIdContext.addListener(listener);
        userIdContext.setUserId(mockUserId);

        /* Listener is called with a userId that is the same as current value. */
        verify(listener).onNewUserId(mockUserId);
        assertEquals(userIdContext.getUserId(), mockUserId);

        /* Listener is not called again on setting the same value. */
        userIdContext.setUserId(mockUserId);
        verify(listener, times(1)).onNewUserId(mockUserId);
        assertEquals(userIdContext.getUserId(), mockUserId);

        /* Setting to null is an update "sign out". */
        userIdContext.setUserId(null);
        verify(listener).onNewUserId(isNull(String.class));
        assertNull(userIdContext.getUserId());
    }

    @Test
    public void setUserIdUserNotEquals() {

        /* When we add a listener then set userId. */
        String mockUserId1 = "userId1";
        String mockUserId2 = "userId2";
        UserIdContext userIdContext = new UserIdContext();
        UserIdContext.Listener listener = mock(UserIdContext.Listener.class);
        userIdContext.addListener(listener);

        /* Listener is called with a userId that is the same as current value. */
        userIdContext.setUserId(mockUserId1);
        verify(listener).onNewUserId(mockUserId1);
        assertEquals(userIdContext.getUserId(), mockUserId1);

        /* When we update userId, listener is called again and current value changes. */
        userIdContext.setUserId(mockUserId2);
        verify(listener).onNewUserId(mockUserId2);
        assertEquals(userIdContext.getUserId(), mockUserId2);

        /* Setting to null is an update "sign out". */
        userIdContext.setUserId(null);
        verify(listener).onNewUserId(isNull(String.class));
        assertNull(userIdContext.getUserId());
    }

    @Test
    public void addAndRemoveListeners() {

        /* When we add a listener then set userId. */
        String mockUserId = "userId";
        UserIdContext userIdContext = new UserIdContext();
        UserIdContext.Listener listener = mock(UserIdContext.Listener.class);
        userIdContext.addListener(listener);
        userIdContext.setUserId(mockUserId);

        /* Listener is called with a userId that is the same as current value. */
        verify(listener).onNewUserId(mockUserId);

        /* When we remove listener and update userId. */
        userIdContext.removeListener(listener);
        String mockUserId2 = "userId2";
        userIdContext.setUserId(mockUserId2);

        /* Listener not called since remove. */
        verify(listener, never()).onNewUserId(mockUserId2);

        /* Restore listener and update to same value: listener not called (by design limitation). */
        userIdContext.setUserId(mockUserId2);
        verify(listener, never()).onNewUserId(mockUserId2);

        /* Restore listener. */
        userIdContext.addListener(listener);

        /* Verify we now get called with update, here is a null update example. */
        userIdContext.setUserId(null);
        verify(listener).onNewUserId(isNull(String.class));
        assertNull(userIdContext.getUserId());
    }

    @Test
    public void noListeners() {
        String mockUserId = "userId";
        UserIdContext.Listener listener = mock(UserIdContext.Listener.class);
        UserIdContext userIdContext = new UserIdContext();
        userIdContext.setUserId(mockUserId);
        verify(listener, never()).onNewUserId(mockUserId);
    }
}
