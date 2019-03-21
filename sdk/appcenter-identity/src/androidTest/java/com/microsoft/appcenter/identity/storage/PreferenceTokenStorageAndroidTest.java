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
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());

        /* Check history. */
        assertEquals(4, tokenStorage.getTokenHistory().size());
        AuthTokenInfo authTokenInfo = tokenStorage.getOldestToken();
        assertNotNull(authTokenInfo);
        assertNull(authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getEndTime());

        /* History has empty array. */
        tokenStorage.setTokenHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>());
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, tokenStorage.getTokenHistory().size());

        /* History has empty string as encrypted token. */
        tokenStorage.setTokenHistory(new ArrayList<PreferenceTokenStorage.TokenStoreEntity>() {{
            add(new PreferenceTokenStorage.TokenStoreEntity("", null, null));
        }});
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(2, tokenStorage.getTokenHistory().size());
    }

    @Test
    public void removeToken() {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);

        /* History is empty. */
        assertNull(tokenStorage.getTokenHistory());

        /* Removing does nothing. */
        tokenStorage.removeToken(null);

        /* Save the token into storage adds 2 entries (null and AUTH_TOKEN to history. */
        tokenStorage.saveToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(2, tokenStorage.getTokenHistory().size());

        /* Remove last one (not allowed). */
        tokenStorage.removeToken(AUTH_TOKEN);
        assertEquals(2, tokenStorage.getTokenHistory().size());

        /* Remove first one. */
        tokenStorage.removeToken(null);
        assertEquals(1, tokenStorage.getTokenHistory().size());

        /* Remove second one (not allowed). */
        tokenStorage.removeToken(AUTH_TOKEN);
        assertEquals(1, tokenStorage.getTokenHistory().size());
    }

    @Test
    public void getOldestToken() {
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

        /* Check the oldest token. */
        AuthTokenInfo authTokenInfo = tokenStorage.getOldestToken();
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNotNull(authTokenInfo.getEndTime());

        /* Remove the oldest token. */
        tokenStorage.removeToken(null);

        /* Check the oldest token. */
        authTokenInfo = tokenStorage.getOldestToken();
        assertEquals("1", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertEquals(beforeHour, authTokenInfo.getEndTime());

        /* Remove the oldest token. */
        tokenStorage.removeToken("1");

        /* Check the oldest token. */
        authTokenInfo = tokenStorage.getOldestToken();
        assertEquals("2", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertTrue(authTokenInfo.getEndTime().before(afterHour));

        /* Remove the oldest token. */
        tokenStorage.removeToken("2");

        /* Check the oldest token. */
        authTokenInfo = tokenStorage.getOldestToken();
        assertEquals("3", authTokenInfo.getAuthToken());
        assertNotNull(authTokenInfo.getStartTime());
        assertEquals(afterHour, authTokenInfo.getEndTime());
    }

    @Test
    public void tokenHistoryLimit() {
        PreferenceTokenStorage tokenStorage = new PreferenceTokenStorage(mContext);
        for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
            String mockToken = UUIDUtils.randomUUID().toString();
            String mockAccountId = UUIDUtils.randomUUID().toString();
            tokenStorage.saveToken(mockToken, mockAccountId, new Date());
        }
        assertEquals(TOKEN_HISTORY_LIMIT, tokenStorage.getTokenHistory().size());
    }
}