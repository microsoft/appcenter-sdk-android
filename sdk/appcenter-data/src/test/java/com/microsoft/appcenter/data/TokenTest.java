/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.data;

import android.content.Context;

import com.google.gson.Gson;
import com.microsoft.appcenter.data.client.TokenExchange;
import com.microsoft.appcenter.data.models.TokenResult;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
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

import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_NAMES;
import static com.microsoft.appcenter.data.Constants.PREFERENCE_PARTITION_PREFIX;
import static com.microsoft.appcenter.data.DefaultPartitions.APP_DOCUMENTS;
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

public class TokenTest extends AbstractDataTest {

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
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if the partition name already exists, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(new HashSet<>(Collections.singleton(APP_DOCUMENTS)));
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if read the partition name list file returns null, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(null);
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);
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
        TokenExchange.getDbToken(DefaultPartitions.USER_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if the partition name already exists, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(new HashSet<>(Collections.singleton(DefaultPartitions.USER_DOCUMENTS)));
        TokenExchange.getDbToken(DefaultPartitions.USER_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);

        /* Verify, if read the partition name list file returns null, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(PREFERENCE_PARTITION_NAMES)).thenReturn(null);
        TokenExchange.getDbToken(DefaultPartitions.USER_DOCUMENTS, mHttpClient, null, null, sTokenExchangeServiceCallback);
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
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setPartition(APP_DOCUMENTS)
                .setExpirationDate(expirationDate.getTime())
                .setDbName("db")
                .setDbAccount("dbAccount")
                .setDbCollectionName("collection")
                .setToken(TOKEN));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + APP_DOCUMENTS)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());

        /* Make the call. */
        Data.getInstance().getTokenAndCallCosmosDbApi(APP_DOCUMENTS, new DefaultAppCenterFuture(), callBack);

        /* Verify the token values. */
        assertEquals(TOKEN, tokenResultCapture.getValue().getToken());
    }

    @Test
    public void canGetTokenWhenCacheExpired() {

        /* Setup mock to get expiration token from cache with expired value. */
        String inValidToken = "invalid";
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, -1000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("lemmings-01-8f37d78902")
                .setDbCollectionName("collection")
                .setStatus("Succeed")
                .setPartition(APP_DOCUMENTS)
                .setExpirationDate(expirationDate.getTime())
                .setToken(inValidToken));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + APP_DOCUMENTS)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback mTokenExchangeServiceCallback = mock(TokenExchange.TokenExchangeServiceCallback.class);
        doNothing().when(mTokenExchangeServiceCallback).callCosmosDb(mock(TokenResult.class));

        /* Make the call. */
        Data.getInstance()
                .getTokenAndCallCosmosDbApi(APP_DOCUMENTS, new DefaultAppCenterFuture(), mTokenExchangeServiceCallback);

        /* Verify. */
        verify(mTokenExchangeServiceCallback, times(0)).callCosmosDb(any(TokenResult.class));
    }

    @Test
    public void callTokenExchangeServiceWhenCacheInvalid() {

        /* Setup mock, create an invalid token result which the collection name is missing. */
        String inValidToken = "invalid";
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 10000);
        String tokenResult = Utils.getGson().toJson(new TokenResult()
                .setDbAccount("lemmings-01-8f37d78902")
                .setStatus("Succeed")
                .setPartition(APP_DOCUMENTS)
                .setExpirationDate(expirationDate.getTime())
                .setToken(inValidToken));
        when(SharedPreferencesManager.getString(PREFERENCE_PARTITION_PREFIX + APP_DOCUMENTS)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback mTokenExchangeServiceCallback = mock(TokenExchange.TokenExchangeServiceCallback.class);
        doNothing().when(mTokenExchangeServiceCallback).callCosmosDb(mock(TokenResult.class));

        /* Make the call. */
        Data.getInstance()
                .getTokenAndCallCosmosDbApi(APP_DOCUMENTS, new DefaultAppCenterFuture(), mTokenExchangeServiceCallback);

        /* Verify. */
        verify(mTokenExchangeServiceCallback, times(0)).callCosmosDb(any(TokenResult.class));
    }

    @Test
    public void getTokenExceptionWhenResponseInvalid() {

        /* Setup the call. */
        final String nullResponseAppUrl = "nullAppUrl";
        final String emptyTokensAppUrl = "emptyTokensAppUrl";
        final String multipleTokensAppUrl = "multipleTokensUrl";
        final String malFormedTokenUrl = "malformedTokensUrl";
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
                } else if (url.getValue().contains(malFormedTokenUrl)) {
                    ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded("{\"tokens\":[{\"status\": \"succeed\"}]}", null);
                }
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, nullResponseAppUrl, null, callBack);
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, emptyTokensAppUrl, null, callBack);
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, multipleTokensAppUrl, null, callBack);
        TokenExchange.getDbToken(APP_DOCUMENTS, mHttpClient, malFormedTokenUrl, null, callBack);

        /* Get and verify token. */
        assertEquals(4, exception.getAllValues().size());
    }

    @Test
    public void cachedTokenPartitionKeyDoesNotContainUserId() {

        /* Create a partition and corresponding TokenResult. */
        String partition = "partition";
        String accountId = "accountId";
        String partitionWithAccountId = partition + "-" + accountId;
        Gson gson = Utils.getGson();
        mockStatic(Utils.class);
        when(Utils.removeAccountIdFromPartitionName(partitionWithAccountId)).thenReturn(partition);
        when(Utils.getGson()).thenReturn(gson);
        TokenResult result = new TokenResult().setPartition(partitionWithAccountId).setAccountId(accountId);
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
