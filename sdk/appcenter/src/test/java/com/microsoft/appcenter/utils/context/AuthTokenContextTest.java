/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.utils.context;

import android.content.Context;
import android.text.TextUtils;

import com.microsoft.appcenter.ingestion.models.json.JSONUtils;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.json.JSONStringer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import static com.microsoft.appcenter.utils.context.AuthTokenContext.PREFERENCE_KEY_TOKEN_HISTORY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({
        CryptoUtils.class,
        JSONUtils.class,
        TextUtils.class,
        SharedPreferencesManager.class
})
@RunWith(PowerMockRunner.class)
public class AuthTokenContextTest {

    private final String AUTH_TOKEN = UUID.randomUUID().toString();

    private AuthTokenContext mAuthTokenContext;

    @Mock
    private CryptoUtils mCryptoUtils;

    @Before
    public void setUp() {
        mockStatic(CryptoUtils.class);
        when(CryptoUtils.getInstance(any(Context.class))).thenReturn(mCryptoUtils);
        mockStatic(SharedPreferencesManager.class);
        mockStatic(TextUtils.class);
        when(TextUtils.equals(any(CharSequence.class), any(CharSequence.class))).then(new Answer<Boolean>() {

            @Override
            public Boolean answer(InvocationOnMock invocation) {
                CharSequence a = (CharSequence) invocation.getArguments()[0];
                CharSequence b = (CharSequence) invocation.getArguments()[1];
                return a == b || (a != null && a.equals(b));
            }
        });

        /* JSON library is part of Android SDK and doesn't available here. */
        mockStatic(JSONUtils.class);
        mAuthTokenContext = AuthTokenContext.getInstance();
    }

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }

    @Test
    public void setAuthTokenTest() {

        /* Mock context listener. */
        AuthTokenContext.Listener mockListener = spy(AbstractTokenContextListener.class);
        String accountId = UUID.randomUUID().toString();
        String homeAccountId = accountId + "-other_user_information";

        /* Set new auth token. */
        mAuthTokenContext.addListener(mockListener);
        mAuthTokenContext.setAuthToken("42", homeAccountId, mock(Date.class));
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, homeAccountId, mock(Date.class));

        /* Verify the value is stored. */
        verifyStatic(times(2));
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_TOKEN_HISTORY), anyString());

        /* Verify that listener is called. */
        verify(mockListener, times(2)).onNewAuthToken(notNull(String.class));
        ArgumentCaptor<String> captorArg = ArgumentCaptor.forClass(String.class);
        verify(mockListener).onNewUser(captorArg.capture());
        assertNotNull(captorArg.getValue());
        assertEquals(accountId, captorArg.getValue());

        /* Verify that the returned token is the same. */
        assertEquals(mAuthTokenContext.getAuthToken(), AUTH_TOKEN);

        /* Clear token data. */
        mAuthTokenContext.setAuthToken(null, null, null);

        /* Verify that listener is called on empty token. */
        verify(mockListener).onNewAuthToken(isNull(String.class));
        ArgumentCaptor<String> captorArgNull = ArgumentCaptor.forClass(String.class);
        verify(mockListener, times(2)).onNewUser(captorArgNull.capture());
        assertNull(captorArgNull.getValue());
        assertNull(mAuthTokenContext.getAuthToken());

        /* Remove listener. */
        mAuthTokenContext.removeListener(mockListener);

        /* Update token without listener attached. */
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, homeAccountId, mock(Date.class));

        /* Verify that listener is called only once on a new token (i.e. before we removed listener). */
        verify(mockListener, times(1)).onNewAuthToken(AUTH_TOKEN);
    }

    @Test
    public void historyNull() {
        mAuthTokenContext.setHistory(null);
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_TOKEN_HISTORY));
    }

    @Test
    public void historySerializeException() throws JSONException {
        PowerMockito.doThrow(new JSONException("")).when(JSONUtils.class);
        JSONUtils.write(any(JSONStringer.class), anyString(), any());
        mAuthTokenContext.setHistory(new ArrayList<AuthTokenHistoryEntry>() {{
            add(new AuthTokenHistoryEntry());
        }});
    }

    @Test
    public void historyEmptyString() {
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("");
        assertNull(mAuthTokenContext.getHistory());
    }

    @Test
    public void tokenRefreshOnNullToken() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -60);
        mAuthTokenContext.setAuthToken("authToken1", "accountId1", calendar.getTime());
        AuthTokenContext.Listener listener = spy(AbstractTokenContextListener.class);
        mAuthTokenContext.addListener(listener);
        AuthTokenInfo mockTokenInfo = mock(AuthTokenInfo.class);
        when(mockTokenInfo.getAuthToken()).thenReturn(null);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(mockTokenInfo);
        verify(listener, never()).onTokenRequiresRefresh(notNull(String.class));
    }

    @Test
    public void tokenRefreshCheck() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, -60);
        mAuthTokenContext.setAuthToken("authToken1", "accountId1", calendar.getTime());
        calendar.add(Calendar.SECOND, 1);
        mAuthTokenContext.setAuthToken("authToken2", "accountId2", calendar.getTime());
        List<AuthTokenInfo> tokenInfoList = mAuthTokenContext.getAuthTokenValidityList();
        AuthTokenInfo authTokenInfo = tokenInfoList.get(tokenInfoList.size() - 1);
        AuthTokenContext.Listener listener = spy(AbstractTokenContextListener.class);
        mAuthTokenContext.addListener(listener);

        /* Check that we receive callback call. */
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfo);
        verify(listener).onTokenRequiresRefresh(eq("accountId2"));
    }

    @Test
    public void tokenRefreshCheckNotExpiresOrNotLast() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR, 24);
        mAuthTokenContext.setAuthToken("authToken1", "accountId", calendar.getTime());
        calendar.add(Calendar.SECOND, 1);
        mAuthTokenContext.setAuthToken("authToken2", "accountId", calendar.getTime());
        List<AuthTokenInfo> tokenInfoList = mAuthTokenContext.getAuthTokenValidityList();
        AuthTokenInfo authTokenInfo = tokenInfoList.get(tokenInfoList.size() - 1);
        AuthTokenContext.Listener listener = spy(AbstractTokenContextListener.class);
        mAuthTokenContext.addListener(listener);

        /* Token not expired check. */
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfo);

        /* Token is not last check. */
        authTokenInfo = tokenInfoList.get(tokenInfoList.size() - 2);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfo);
        verify(listener, never()).onTokenRequiresRefresh(notNull(String.class));
    }

    @Test
    public void tokenRefreshCheckWhenExpiresIsNull() {
        mAuthTokenContext.setAuthToken("authToken2", "accountId", null);
        List<AuthTokenInfo> tokenInfoList = mAuthTokenContext.getAuthTokenValidityList();
        AuthTokenInfo authTokenInfo = tokenInfoList.get(tokenInfoList.size() - 1);
        AuthTokenContext.Listener listener = spy(AbstractTokenContextListener.class);

        /* If expires date is null, we should not be able to reach that method. */
        mAuthTokenContext.addListener(listener);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfo);
        verify(listener, never()).onTokenRequiresRefresh(notNull(String.class));
    }

    @Test
    public void tokenRefreshCheckNoHistoryOrHistoryIsNull() {
        AuthTokenInfo authTokenInfoMock = mock(AuthTokenInfo.class);
        AuthTokenContext.Listener listener = spy(AbstractTokenContextListener.class);
        mAuthTokenContext.addListener(listener);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfoMock);
        List<AuthTokenHistoryEntry> dummyList = new ArrayList<>(0);
        mAuthTokenContext.setHistory(dummyList);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(authTokenInfoMock);

        /* If we have null or empty history, we should not be able to reach that method. */
        verify(authTokenInfoMock, never()).isAboutToExpire();
        verify(listener, never()).onTokenRequiresRefresh(notNull(String.class));
    }

    @Test
    public void historyEmptyJson() {
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn("");
        when(mCryptoUtils.decrypt(eq("empty"), eq(false))).thenReturn(decryptedData);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("empty");
        assertNull(mAuthTokenContext.getHistory());
    }

    @Test
    public void historyEmptyArray() {
        CryptoUtils.DecryptedData decryptedData = mock(CryptoUtils.DecryptedData.class);
        when(decryptedData.getDecryptedData()).thenReturn("[]");
        when(mCryptoUtils.decrypt(eq("secret"), eq(false))).thenReturn(decryptedData);
        when(SharedPreferencesManager.getString(eq(PREFERENCE_KEY_TOKEN_HISTORY), isNull(String.class))).thenReturn("secret");
        assertEquals(0, mAuthTokenContext.getHistory().size());
    }

    @Test(timeout = 5000)
    public void listenerDeadlock() {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        mAuthTokenContext.addListener(new AbstractTokenContextListener() {

            @Override
            public void onNewAuthToken(String authToken) {
                latch1.countDown();
                try {
                    latch2.await();
                } catch (InterruptedException ignored) {
                }
            }
        });
        new Thread(new Runnable() {

            @Override
            public void run() {

                /* Wait for listener call. */
                try {
                    latch1.await();
                } catch (InterruptedException ignored) {
                }

                /* Call something synchronized. */
                assertEquals(AUTH_TOKEN, mAuthTokenContext.getAuthToken());
                latch2.countDown();
            }
        }).start();
        mAuthTokenContext.setAuthToken(AUTH_TOKEN, "some-id", null);
    }

    @Test(timeout = 5000)
    public void listenerDeadlockCheckIfTokenNeedsToBeRefreshed() {
        final CountDownLatch latch1 = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        mAuthTokenContext.addListener(new AbstractTokenContextListener() {

            @Override
            public void onTokenRequiresRefresh(String homeAccountId) {
                latch1.countDown();
                try {
                    latch2.await();
                } catch (InterruptedException ignored) {
                }
            }
        });
        new Thread(new Runnable() {

            @Override
            public void run() {

                /* Wait for listener call. */
                try {
                    latch1.await();
                } catch (InterruptedException ignored) {
                }

                /* Call something synchronized. */
                assertEquals(AUTH_TOKEN, mAuthTokenContext.getAuthToken());
                latch2.countDown();
            }
        }).start();
        AuthTokenInfo info = new AuthTokenInfo(AUTH_TOKEN, mock(Date.class), mock(Date.class));
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(info);
    }
}
