/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity.storage;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.identity.storage.PreferenceTokenStorage.TOKEN_HISTORY_LIMIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PreferenceTokenStorageAndroidTest {

    private static final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ACCOUNT_ID = UUIDUtils.randomUUID().toString();

    private Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(mContext);
    }

    @After
    public void tearDown() {
        SharedPreferencesManager.clear();
    }

    @Test
    public void saveToken() {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

        /* Initial values are null. */
        assertNull(AUTH_TOKEN, tokenStorage.getToken());
        assertNull(ACCOUNT_ID, tokenStorage.getHomeAccountId());

        /* Account id is still null we can't pass valid token. */
        tokenStorage.saveToken(null, ACCOUNT_ID, new Date());
        assertNull(AUTH_TOKEN, tokenStorage.getToken());
        assertNull(ACCOUNT_ID, tokenStorage.getHomeAccountId());

        /* Save the token into storage. */
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());

        /* Assert that storage returns the same token. */
        assertEquals(AUTH_TOKEN, tokenStorage.getToken());
        assertEquals(ACCOUNT_ID, tokenStorage.getHomeAccountId());

        /* Remove the token from storage. */
        tokenStorage.saveToken(null, null, null);

        /* Assert that there's no token in storage. */
        assertNull(tokenStorage.getToken());
        assertNull(tokenStorage.getHomeAccountId());

        /* The same token should't be in history twice in a row. */
        Calendar calendar = Calendar.getInstance();
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, calendar.getTime());
        calendar.add(Calendar.HOUR, 1);
        tokenStorage.saveToken(AUTH_TOKEN, null, calendar.getTime());

        /* Check history. */
        assertEquals(4, tokenStorage.getHistory().size());

        /* History has empty array. */
        tokenStorage.setHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
        assertNull(AUTH_TOKEN, tokenStorage.getToken());
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, tokenStorage.getHistory().size());

        /* History has empty string as encrypted token. */
        tokenStorage.setHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>() {{
            add(new PreferenceTokenStorage.TokenStoreEntity("", null, null, null));
        }});
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(2, tokenStorage.getHistory().size());
    }

    @Test
    public void removeToken() {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

        /* History is empty (removing does nothing). */
        assertNull(tokenStorage.getHistory());
        tokenStorage.removeToken(null);
        assertNull(tokenStorage.getHistory());

        /* Save the token into storage adds 2 entries (null and AUTH_TOKEN to history. */
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(2, tokenStorage.getHistory().size());

        /* Remove last one (not allowed). */
        tokenStorage.removeToken(AUTH_TOKEN);
        assertEquals(2, tokenStorage.getHistory().size());

        /* Remove first one. */
        tokenStorage.removeToken(null);
        assertEquals(1, tokenStorage.getHistory().size());

        /* Remove second one (not allowed). */
        tokenStorage.removeToken(AUTH_TOKEN);
        assertEquals(1, tokenStorage.getHistory().size());

        /* History has empty array (removing does nothing). */
        tokenStorage.setHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
        tokenStorage.removeToken(null);
        assertEquals(0, tokenStorage.getHistory().size());
    }

    @Test
    public void getTokenHistory() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.HOUR, -1);
        Date beforeHour = calendar.getTime();
        calendar.add(Calendar.HOUR, 2);
        Date afterHour = calendar.getTime();

        /* Store some tokens. */
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
        tokenStorage.saveToken("1", ACCOUNT_ID, beforeHour);
        tokenStorage.saveToken("2", ACCOUNT_ID, afterHour);
        tokenStorage.saveToken("3", ACCOUNT_ID, afterHour);

        /* Check history. */
        List<AuthTokenInfo> history = tokenStorage.getTokenHistory();
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

        /* History has empty array. */
        tokenStorage.setHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
        assertNull(tokenStorage.getTokenHistory());
    }

    @Test
    public void tokenHistoryLimit() {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
        for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
            String mockToken = UUIDUtils.randomUUID().toString();
            String mockAccountId = UUIDUtils.randomUUID().toString();
            tokenStorage.saveToken(mockToken, mockAccountId, new Date());
        }
        assertEquals(TOKEN_HISTORY_LIMIT, tokenStorage.getHistory().size());
    }
}