package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import org.json.JSONException;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import static org.mockito.Mockito.when;
import static org.mockito.Matchers.any;

@RunWith(PowerMockRunner.class)
public class TokenTest extends AbstractStorageTest {

    @Mock
    HttpClient mHttpClient;

    static final String fakePartitionName = "read-only";

    static final String fakeToken = "mock";

    @Test
    public void canGetToken() {

        /* Mock http call to get token */
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken).withStatus(Constants.SUCCEED))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doCallRealMethod().when(callBack).onCallSucceeded(anyString(), anyMap());
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());
        when(mHttpClient.callAsync(anyString(), anyString(), anyMap(), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call */
        TokenExchange.getDbToken(fakePartitionName, mHttpClient, null, null, callBack);

        /* Get and verify token */
        Assert.assertEquals(fakeToken, tokenResultCapture.getValue().token());
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {

        /* Setup mock to get expiration token from cache */
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, 1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withExpirationTime(expirationDate.getTime()).withToken(fakeToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());

        /* Make the call */
        TokenExchange.getDbToken(fakePartitionName, null, null, null, callBack);

        /* Verify the token values */
        Assert.assertEquals(fakeToken, tokenResultCapture.getValue().token());
    }

    @Test
    public void canGetTokenWhenCacheInvalid() {

        /* Setup mock to get expiration token from cache with expired value*/
        String inValidToken = "invalid";
        Calendar expirationDate = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        expirationDate.add(Calendar.SECOND, -1000);
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withExpirationTime(expirationDate.getTime()).withToken(inValidToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken).withStatus(Constants.SUCCEED))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        TokenExchange.TokenExchangeServiceCallback callBack = mock(TokenExchange.TokenExchangeServiceCallback.class);
        ArgumentCaptor<TokenResult> tokenResultCapture = ArgumentCaptor.forClass(TokenResult.class);
        doCallRealMethod().when(callBack).onCallSucceeded(anyString(), anyMap());
        doNothing().when(callBack).callCosmosDb(tokenResultCapture.capture());
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });

        /* Make the call */
        TokenExchange.getDbToken(fakePartitionName, mHttpClient, null, null, callBack);

        /* Verify the token values */
        Assert.assertEquals(fakeToken, tokenResultCapture.getValue().token());
    }
}
