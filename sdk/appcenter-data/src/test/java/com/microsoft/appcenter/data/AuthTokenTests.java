/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.Context;

import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_NAMES;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.matches;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

public class AuthTokenTests extends AbstractDataTest {

    @Test
    public void tokenClearedOnSignOut() {

        /* Add partitions. */
        Set<String> partitionNames = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            partitionNames.add("partitionName" + i);
        }
        partitionNames.add(DefaultPartitions.APP_DOCUMENTS);
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(partitionNames);
        Data.setEnabled(true);
        AuthTokenContext.getInstance().setAuthToken(null, null, null);

        /* Verify. */
        verify(mLocalDocumentStorage).resetDatabase();
        verify(mLocalDocumentStorage, never()).createTableIfDoesNotExist(anyString());
        for (int i = 0; i < 10; i++) {
            verifyStatic();
            SharedPreferencesManager.remove(PREFERENCE_PARTITION_PREFIX + "partitionName" + i);
        }
    }

    @Test
    public void authTokenListenerNotCalledWhenDisabled() {
        Data.setEnabled(false);
        AuthTokenContext.getInstance().setAuthToken(null, null, null);
        verifyStatic(never());
        SharedPreferencesManager.remove(matches(PREFERENCE_PARTITION_PREFIX + "partitionName[0-9]"));
    }

    @Test
    public void authTokenListenerNotCalledWhenNewUser() {
        AuthTokenContext.getInstance().setAuthToken("someToken", "someId", new Date(Long.MAX_VALUE));
        AuthTokenContext.getInstance().setAuthToken(null, null, null);
        verifyStatic(never());
        SharedPreferencesManager.remove(matches(PREFERENCE_PARTITION_PREFIX + "partitionName[0-9]"));
    }

    @Test
    public void authTokenListenerNotRemoveTokenWhenNewUser() {

        /* Setup token manager. */
        mockStatic(TokenManager.class);
        TokenManager mTokenManager = mock(TokenManager.class);
        when(TokenManager.getInstance(notNull(Context.class))).thenReturn(mTokenManager);

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = mock(AuthTokenContext.Listener.class);

        /* Set new auth token. */
        AuthTokenContext.getInstance().addListener(mockListener);
        AuthTokenContext.getInstance().setAuthToken("mock-token", "mock-user", new Date(Long.MAX_VALUE));

        /* Verify. */
        verify(mTokenManager, never()).removeAllCachedTokens();
        verify(mLocalDocumentStorage).createTableIfDoesNotExist(anyString());
    }
}
