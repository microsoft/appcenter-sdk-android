/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.storage;

import android.content.Context;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import static com.microsoft.appcenter.storage.Constants.PREFERENCE_PARTITION_NAMES;
import static com.microsoft.appcenter.storage.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.storage.Constants.READONLY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

public class TokenTest extends AbstractStorageTest {

    private static final TokenExchange.TokenExchangeServiceCallback sTokenExchangeServiceCallback =
            new TokenExchange.TokenExchangeServiceCallback(TokenManager.getInstance(PowerMockito.mock(Context.class))) {

                @Override
                public void completeFuture(Exception e) {
                }

                @Override
                public void callCosmosDb(TokenResult tokenResult) {

                    /* Get and verify token. */
                    assertEquals(TOKEN, tokenResult.getToken());

                    /* Get and verify the account id. */
                    assertEquals(ACCOUNT_ID, tokenResult.getAccountId());
                }
            };

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @Mock
    HttpClient mHttpClient;

    @After
    public void tearDown() {
        AuthTokenContext.unsetInstance();
    }

    @Test
    public void canGetAndSetTokenReadonly() {
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(sTokenExchangeServiceCallback))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(TOKEN_EXCHANGE_USER_PAYLOAD, null);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(READONLY, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if the partition name already exists, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(new HashSet<>(Collections.singleton(READONLY)));
        TokenExchange.getDbToken(READONLY, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if read the partition name list file returns null, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(null);
        TokenExchange.getDbToken(READONLY, mHttpClient, null, null, sTokenExchangeServiceCallback);
        verify(mHttpClient, times(3)).callAsync(anyString(), anyString(), mHeadersCaptor.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        for (Map<String, String> headers : mHeadersCaptor.getAllValues()) {
            assertNotNull(headers);
            assertNull(headers.get(com.microsoft.appcenter.Constants.AUTHORIZATION_HEADER));
        }
    }

    @Test
    public void canGetAndSetTokenPartition() {

        /* Mock http call to get token. */
        String authToken = "auth-token";
        AuthTokenContext.getInstance().setAuthToken(authToken, "account id", new Date(Long.MAX_VALUE));
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(sTokenExchangeServiceCallback))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(TOKEN_EXCHANGE_USER_PAYLOAD, null);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(Constants.USER, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if the partition name already exists, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(new HashSet<>(Collections.singleton(Constants.USER)));
        TokenExchange.getDbToken(Constants.USER, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if read the partition name list file returns null, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(null);
        TokenExchange.getDbToken(Constants.USER, mHttpClient, null, null, sTokenExchangeServiceCallback);
        verify(mHttpClient, times(3)).callAsync(anyString(), anyString(), mHeadersCaptor.capture(), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        for (Map<String, String> headers : mHeadersCaptor.getAllValues()) {
            assertNotNull(headers);
            assertEquals(String.format(com.microsoft.appcenter.Constants.AUTH_TOKEN_FORMAT, authToken), headers.get(com.microsoft.appcenter.Constants.AUTHORIZATION_HEADER));
        }
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult()
                .withPartition(READONLY)
                .withExpirationDate(expirationDate.getTime())
                .withDbName("db")
                .withDbAccount("dbAccount")
                .withDbCollectionName("collection")
                .withToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + READONLY)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());

        /* Make the call. */
        Storage.getInstance().getTokenAndCallCosmosDbApi(READONLY, new DefaultAppCenterFuture(), callBack);

        /* Verify the token values. */
        assertEquals(TOKEN, tokenResultCapture.getValue().getToken());
    }

    @Test
    public void canGetTokenWhenCacheInvalid() {

        /* Setup mock to get expiration token from cache with expired value. */
        String inValidToken = "invalid";
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, -1000);
        String tokenResult = new Gson().toJson(new TokenResult()
                .withDbAccount("lemmings-01-8f37d78902")
                .withDbCollectionName("collection")
                .withStatus("Succeed")
                .withPartition(READONLY)
                .withExpirationDate(expirationDate.getTime())
                .withToken(inValidToken));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + READONLY)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback mTokenExchangeServiceCallback = mock(TokenExchange.TokenExchangeServiceCallback.class);
        doNothing().when(mTokenExchangeServiceCallback).callCosmosDb(mock(TokenResult.class));

        /* Make the call. */
        Storage.getInstance()
                .getTokenAndCallCosmosDbApi(READONLY, new DefaultAppCenterFuture(), mTokenExchangeServiceCallback);

        /* Verify. */
        verify(mTokenExchangeServiceCallback, times(0)).callCosmosDb(any(TokenResult.class));
    }

    @Test
    public void getTokenExceptionWhenResponseInvalid() {

        /* Setup the call. */
        final String nullResponseAppUrl = "nullAppUrl";
        final String emptyTokensAppUrl = "emptyTokensAppUrl";
        final String multipleTokensAppUrl = "multipleTokensUrl";
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        final ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<Exception> exception = ArgumentCaptor.forClass(Exception.class);
        doCallRealMethod().when(callBack).onCallSucceeded(anyString(), anyMapOf(String.class, String.class));
        doNothing().when(callBack).onCallFailed(exception.capture());
        when(mHttpClient.callAsync(url.capture(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                if (url.getValue().contains(nullResponseAppUrl)) {
                    ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(null, null);
                } else if (url.getValue().contains(emptyTokensAppUrl)) {
                    ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded("{\"tokens\": null}", null);
                } else if (url.getValue().contains(multipleTokensAppUrl)) {
                    ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded("{\"tokens\":[{}, {}]}", null);
                }
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(READONLY, mHttpClient, nullResponseAppUrl, null, callBack);
        TokenExchange.getDbToken(READONLY, mHttpClient, emptyTokensAppUrl, null, callBack);
        TokenExchange.getDbToken(READONLY, mHttpClient, multipleTokensAppUrl, null, callBack);

        /* Get and verify token. */
        assertEquals(3, exception.getAllValues().size());
    }

    @Test
    public void cachedTokenPartitionKeyDoesNotContainUserId() {

        /* Create a partition and corresponding TokenResult. */
        String partition = "partition";
        String accountId = "accountId";
        String partitionWithAccountId = partition + "-" + accountId;
        Gson gson = new Gson();
        mockStatic(Utils.class);
        when(Utils.removeAccountIdFromPartitionName(partitionWithAccountId)).thenReturn(partition);
        when(Utils.getGson()).thenReturn(gson);
        TokenResult result = new TokenResult().withPartition(partitionWithAccountId).withAccountId(accountId);
        Set<String> partitions = new HashSet<>();
        partitions.add(partition);

        /* Attempt to cache the token result. */
        TokenManager.getInstance(mock(Context.class)).setCachedToken(result);

        /* Verify that the cached partition name does not contain the account ID. */
        verifyStatic();
        SharedPreferencesManager.putStringSet(PREFERENCE_PARTITION_NAMES, partitions);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_PARTITION_PREFIX + partition, gson.toJson(result));
    }
}
