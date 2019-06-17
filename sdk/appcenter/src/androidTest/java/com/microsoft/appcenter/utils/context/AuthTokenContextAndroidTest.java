/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.ACCOUNT_ID_LENGTH;
import static com.microsoft.appcenter.utils.context.AuthTokenContext.PREFERENCE_KEY_TOKEN_HISTORY;
import static com.microsoft.appcenter.utils.context.AuthTokenContext.TOKEN_HISTORY_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthTokenContextAndroidTest {

    private static final String AUTH_TOKEN = UUID.randomUUID().toString();

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private AuthTokenContext mAuthTokenContext;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(context);
        AuthTokenContext.initialize(context);
        mAuthTokenContext = AuthTokenContext.getInstance();
        mAuthTokenContext.setHistory(null);
    }

    @After
    public void tearDown() {
        SharedPreferencesManager.clear();
        AuthTokenContext.unsetInstance();
    }

    @Test
    public void setAuthToken() {

        /* Initial values are null. */
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());
        assertNull(mAuthTokenContext.getHistory());
        List<AuthTokenInfo> history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());

        /* Account id is still null we can't pass valid token. */
        mAuthTokenContext.setAuthToken(null, ACCOUNT_ID, new Date());
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());
        assertEquals(1, mAuthTokenContext.getHistory().size());
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());

        /* Save the token into storage. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());

        /* Assert that storage returns the same token. */
        assertEquals(AUTH_TOKEN, mAuthTokenContext.getAuthToken());
        assertEquals(ACCOUNT_ID, mAuthTokenContext.getHomeAccountId());
        assertEquals(ACCOUNT_ID.substring(0, Math.min(ACCOUNT_ID_LENGTH, ACCOUNT_ID.length())), mAuthTokenContext.getAccountId());

        /* Remove the token from storage. */
        mAuthTokenContext.setAuthToken(null, null, null);

        /* Assert that there's no token in storage. */
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());
        assertNull(mAuthTokenContext.getAccountId());

        /* The same token should't be in history twice in a row. */
        Calendar calendar = Calendar.getInstance();
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, calendar.getTime());
        calendar.add(Calendar.HOUR, 1);
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, null, calendar.getTime());

        /* Check history. */
        assertEquals(4, mAuthTokenContext.getHistory().size());
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(4, history.size());
    }

    @Test
    public void historyIsNull() {
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());
        assertNull(mAuthTokenContext.getHistory());

        /* Removing does nothing. */
        mAuthTokenContext.removeOldestTokenIfMatching(null);
        assertNull(mAuthTokenContext.getHistory());

        /* Check validity list. */
        List<AuthTokenInfo> history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());
        AuthTokenInfo authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* Add some token to empty history. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void historyIsEmptyArray() {

        /* History has empty array. */
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>());
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());
        assertEquals(0, mAuthTokenContext.getHistory().size());

        /* Removing does nothing. */
        mAuthTokenContext.removeOldestTokenIfMatching(null);
        assertEquals(0, mAuthTokenContext.getHistory().size());

        /* Check validity list. */
        List<AuthTokenInfo> history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());
        AuthTokenInfo authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* Add some token to empty history. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void historyIsEmptyInfo() {
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>() {{
            add(new AuthTokenHistoryEntry());
        }});
        assertEquals(1, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void historyIsValidJson() {
        Context context = InstrumentationRegistry.getTargetContext();
        String data = CryptoUtils.getInstance(context).encrypt("[{},{" +
                "\"authToken\":\"f8950b6a-2f63-49f2-8b02-2dc5577ec4bf\"," +
                "\"homeAccountId\":\"fa45f2a0-9634-46ff-9c09-36224542e1fc\"," +
                "\"time\":\"2019-03-28T18:41:48.247Z\"," +
                "\"expiresOn\":\"2019-03-28T18:41:37.276Z\"}]");
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, data);
        assertEquals(2, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void historyIsInvalidJson() {
        Context context = InstrumentationRegistry.getTargetContext();
        String data = CryptoUtils.getInstance(context).encrypt("bad json");
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, data);
        assertNull(mAuthTokenContext.getHistory());
    }

    @Test
    public void removeToken() {

        /* History is empty (removing does nothing). */
        assertNull(mAuthTokenContext.getHistory());
        mAuthTokenContext.removeOldestTokenIfMatching(null);
        assertNull(mAuthTokenContext.getHistory());

        /* Save the token into storage. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* Remove current token (not allowed). */
        mAuthTokenContext.removeOldestTokenIfMatching(AUTH_TOKEN);
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* Add second token. */
        mAuthTokenContext.setAuthToken("42", ACCOUNT_ID, new Date(Long.MAX_VALUE));
        assertEquals(2, mAuthTokenContext.getHistory().size());

        /* Remove current token (not allowed). */
        mAuthTokenContext.removeOldestTokenIfMatching("42");
        assertEquals(2, mAuthTokenContext.getHistory().size());

        /* Remove oldest token. */
        mAuthTokenContext.removeOldestTokenIfMatching(AUTH_TOKEN);
        assertEquals(1, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void getAuthTokenValidityList() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR, -1);
        Date beforeHour = calendar.getTime();
        calendar.add(Calendar.HOUR, 2);
        Date afterHour = calendar.getTime();

        /* Store some tokens. */
        mAuthTokenContext.setAuthToken("1", ACCOUNT_ID, beforeHour);
        mAuthTokenContext.setAuthToken("2", ACCOUNT_ID, afterHour);
        mAuthTokenContext.setAuthToken("3", ACCOUNT_ID, afterHour);

        /* Check history. */
        List<AuthTokenInfo> history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(4, history.size());

        /* Check first entry. */
        AuthTokenInfo authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNotNull(authTokenInfo.getEndTime());

        /* Check second entry. */
        authTokenInfo = history.get(1);
        assertEquals("1", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertEquals(beforeHour, authTokenInfo.getEndTime());

        /* Check third entry. */
        authTokenInfo = history.get(2);
        assertEquals("2", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertTrue(authTokenInfo.getEndTime().before(afterHour));

        /* Check fourth entry. */
        authTokenInfo = history.get(3);
        assertEquals("3", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertEquals(afterHour, authTokenInfo.getEndTime());
    }

    @Test
    public void tokenHistoryLimit() {
        for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
            String mockToken = UUID.randomUUID().toString();
            String mockAccountId = UUID.randomUUID().toString();
            mAuthTokenContext.setAuthToken(mockToken, mockAccountId, new Date());
        }
        assertEquals(TOKEN_HISTORY_LIMIT, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void preventResetAuthTokenAfterStart() {
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        List<AuthTokenHistoryEntry> listBeforeFinishInitialization = mAuthTokenContext.getHistory();
        mAuthTokenContext.doNotResetAuthAfterStart();
        mAuthTokenContext.finishInitialization();
        List<AuthTokenHistoryEntry> listAfterFinishInitialization = mAuthTokenContext.getHistory();
        assertEquals(listBeforeFinishInitialization.size(), listAfterFinishInitialization.size());
    }
}
