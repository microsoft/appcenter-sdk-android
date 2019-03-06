package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.async.DefaultAppCenterFuture;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
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

public class TokenTest extends AbstractStorageTest {

    private static final String FAKE_PARTITION_NAME = "read-only";

    private static final String FAKE_TOKEN = "mock";

    @Mock
    HttpClient mHttpClient;

    @Test
    public void canGetAndSetToken() {

        /* Mock http call to get token. */
        final String expectedResponse = String.format("{\n" +
                "    \"tokens\": [\n" +
                "        {\n" +
                "            \"partition\": \"%s\",\n" +
                "            \"dbAccount\": \"lemmings-01-8f37d78902\",\n" +
                "            \"dbName\": \"db\",\n" +
                "            \"dbCollectionName\": \"collection\",\n" +
                "            \"token\": \"%s\",\n" +
                "            \"status\": \"Succeed\"\n" +
                "        }\n" +
                "    ]\n" +
                "}", FAKE_PARTITION_NAME, FAKE_TOKEN);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doCallRealMethod().when(callBack).onCallSucceeded(anyString(), anyMapOf(String.class, String.class));
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, null, null, callBack);

        /* Get and verify token. */
        assertEquals(FAKE_TOKEN, tokenResultCapture.getValue().token());

        /* Verify, if the partition name already exists, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(eq(Constants.PARTITION_NAMES))).thenReturn(new HashSet<>(Collections.singleton(FAKE_PARTITION_NAME)));
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, null, null, callBack);

        /* Verify, if read the partition name list file returns null, it did not throw when set token. */
        when(SharedPreferencesManager.getStringSet(eq(Constants.PARTITION_NAMES))).thenReturn(null);
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, null, null, callBack);
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult()
                .withPartition(FAKE_PARTITION_NAME)
                .withExpirationTime(expirationDate.getTime())
                .withDbName("db")
                .withDbAccount("dbAccount")
                .withDbCollectionName("collection")
                .withToken(FAKE_TOKEN));
        when(SharedPreferencesManager.getString(FAKE_PARTITION_NAME)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());

        /* Make the call. */
        Storage.getInstance().getTokenAndCallCosmosDbApi(FAKE_PARTITION_NAME, new DefaultAppCenterFuture(), callBack);

        /* Verify the token values. */
        assertEquals(FAKE_TOKEN, tokenResultCapture.getValue().token());
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
                .withPartition(FAKE_PARTITION_NAME)
                .withExpirationTime(expirationDate.getTime())
                .withToken(inValidToken));
        when(SharedPreferencesManager.getString(FAKE_PARTITION_NAME)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback mTokenExchangeServiceCallback = mock(TokenExchange.TokenExchangeServiceCallback.class);
        doNothing().when(mTokenExchangeServiceCallback).callCosmosDb(mock(TokenResult.class));

        /* Make the call. */
        Storage.getInstance()
                .getTokenAndCallCosmosDbApi(FAKE_PARTITION_NAME, new DefaultAppCenterFuture(), mTokenExchangeServiceCallback);

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
                } else if (url.getValue().contains(multipleTokensAppUrl)){
                    ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded("{\"tokens\":[{}, {}]}", null);
                }
                return mock(ServiceCall.class);
            }
        });

        /* Make the call. */
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, nullResponseAppUrl, null, callBack);
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, emptyTokensAppUrl, null, callBack);
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, mHttpClient, multipleTokensAppUrl, null, callBack);

        /* Get and verify token. */
        assertEquals(3, exception.getAllValues().size());
    }

    @Test
    public void canHandleWhenExpiresOnInvalidFormat() {
        TokenResult result = new TokenResult();
        assertEquals(new Date(0), result.expiresOn());
        result.withExpirationTime(null);
    }
}
