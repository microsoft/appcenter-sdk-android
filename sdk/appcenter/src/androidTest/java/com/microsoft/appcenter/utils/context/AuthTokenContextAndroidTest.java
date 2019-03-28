package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.PREFERENCE_KEY_TOKEN_HISTORY;
import static com.microsoft.appcenter.utils.context.AuthTokenContext.TOKEN_HISTORY_LIMIT;
import static org.junit.Assert.*;

public class AuthTokenContextAndroidTest {

    private static final String AUTH_TOKEN = UUIDUtils.randomUUID().toString();

    private static final String ACCOUNT_ID = UUIDUtils.randomUUID().toString();

    private AuthTokenContext mAuthTokenContext;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getTargetContext();
        SharedPreferencesManager.initialize(context);
        AuthTokenContext.initialize(context);
        mAuthTokenContext = AuthTokenContext.getInstance();
    }

    @After
    public void tearDown() {
        SharedPreferencesManager.clear();
        AuthTokenContext.unsetInstance();
    }

    @Test
    public void saveToken() {

        /* Initial values are null. */
        assertNull( mAuthTokenContext.getAuthToken());
        assertNull( mAuthTokenContext.getHomeAccountId());

        /* Account id is still null we can't pass valid token. */
        mAuthTokenContext.setAuthToken(null, ACCOUNT_ID, new Date());
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());

        /* Save the token into storage. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());

        /* Assert that storage returns the same token. */
        assertEquals(AUTH_TOKEN, mAuthTokenContext.getAuthToken());
        assertEquals(ACCOUNT_ID, mAuthTokenContext.getHomeAccountId());

        /* Remove the token from storage. */
        mAuthTokenContext.setAuthToken(null, null, null);

        /* Assert that there's no token in storage. */
        assertNull(mAuthTokenContext.getAuthToken());
        assertNull(mAuthTokenContext.getHomeAccountId());

        /* The same token should't be in history twice in a row. */
        Calendar calendar = Calendar.getInstance();
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, calendar.getTime());
        calendar.add(Calendar.HOUR, 1);
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, null, calendar.getTime());

        /* Check history. */
        assertEquals(4, mAuthTokenContext.getHistory().size());

        /* History has empty array. */
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>());
        assertNull(AUTH_TOKEN, mAuthTokenContext.getAuthToken());
        assertNull(ACCOUNT_ID, mAuthTokenContext.getHomeAccountId());
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* History has empty string as encrypted token. */
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>() {{
            add(new AuthTokenHistoryEntry());
        }});
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(2, mAuthTokenContext.getHistory().size());
    }

    @Test
    public void removeToken() {

        /* History is empty (removing does nothing). */
        assertNull(mAuthTokenContext.getHistory());
        mAuthTokenContext.removeToken(null);
        assertNull(mAuthTokenContext.getHistory());

        /* Save the token into storage. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, ACCOUNT_ID, new Date());
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* Remove last one (not allowed). */
        mAuthTokenContext.removeToken(AUTH_TOKEN);
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* Remove second one (not allowed). */
        mAuthTokenContext.removeToken(AUTH_TOKEN);
        assertEquals(1, mAuthTokenContext.getHistory().size());

        /* History has empty array (removing does nothing). */
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>());
        mAuthTokenContext.removeToken(null);
        assertEquals(0, mAuthTokenContext.getHistory().size());
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

        /* History has empty array. */
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>());
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());

        /* History is null. */
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, null);
        assertEquals(0, mAuthTokenContext.getHistory().size());
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());
        authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());


        /* History is empty string. */
        mAuthTokenContext.setHistory(null);
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, "");
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());
        authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());

        /* History is invalid. */
        mAuthTokenContext.setHistory(null);
        SharedPreferencesManager.putString(PREFERENCE_KEY_TOKEN_HISTORY, "some bad value");
        history = mAuthTokenContext.getAuthTokenValidityList();
        assertEquals(1, history.size());
        authTokenInfo = history.get(0);
        assertNull(authTokenInfo.getAuthToken());
        assertNull(authTokenInfo.getStartTime());
        assertNull(authTokenInfo.getEndTime());
    }

    @Test
    public void tokenHistoryLimit() {
        for (int i = 0; i < TOKEN_HISTORY_LIMIT + 3; i++) {
            String mockToken = UUIDUtils.randomUUID().toString();
            String mockAccountId = UUIDUtils.randomUUID().toString();
            mAuthTokenContext.setAuthToken(mockToken, mockAccountId, new Date());
        }
        assertEquals(TOKEN_HISTORY_LIMIT, mAuthTokenContext.getHistory().size());
    }
}
