package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Calendar;
import java.util.TimeZone;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class TokenTest extends AbstractStorageTest {

    private static final String FAKE_PARTITION_NAME = "read-only";

    private static final String FAKE_TOKEN = "mock";

    @Mock
    HttpClient mHttpClient;

    @Test
    public void canGetToken() {

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
        Assert.assertEquals(FAKE_TOKEN, tokenResultCapture.getValue().token());
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {

        /* Setup mock to get expiration token from cache. */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(FAKE_PARTITION_NAME).withExpirationTime(expirationDate.getTime()).withToken(FAKE_TOKEN));
        when(SharedPreferencesManager.getString(FAKE_PARTITION_NAME)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());

        /* Make the call. */
        TokenExchange.getDbToken(FAKE_PARTITION_NAME, null, null, null, callBack);

        /* Verify the token values. */
        Assert.assertEquals(FAKE_TOKEN, tokenResultCapture.getValue().token());
    }

    @Test
    public void canGetTokenWhenCacheInvalid() {

        /* Setup mock to get expiration token from cache with expired value. */
        String inValidToken = "invalid";
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, -1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(FAKE_PARTITION_NAME).withExpirationTime(expirationDate.getTime()).withToken(inValidToken));
        when(SharedPreferencesManager.getString(FAKE_PARTITION_NAME)).thenReturn(tokenResult);
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

        /* Verify the token values. */
        Assert.assertEquals(FAKE_TOKEN, tokenResultCapture.getValue().token());
    }
}
