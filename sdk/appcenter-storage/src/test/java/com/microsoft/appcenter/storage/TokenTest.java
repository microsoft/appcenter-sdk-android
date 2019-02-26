package com.microsoft.appcenter.storage;

import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.storage.client.TokenExchange;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        TokenExchange.TokenExchangeServiceCallback callBack = new TokenExchangeServiceCallbackMock();
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });
        TokenExchange.getDbToken(fakePartitionName, mHttpClient, "", "", callBack);
        Assert.assertEquals(fakeToken, ((TokenExchangeServiceCallbackMock) callBack).getTokenResult().token());
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withTTL(new Date().getTime() + 100000000 ).withToken(fakeToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        TokenExchange.TokenExchangeServiceCallback callBack = new TokenExchangeServiceCallbackMock();
        TokenExchange.getDbToken(fakePartitionName, null, "", "", callBack);
        Assert.assertEquals(fakeToken, ((TokenExchangeServiceCallbackMock) callBack).getTokenResult().token());
    }

    @Test
    public void canGetTokenWhenCacheInvalid() {
        String inValidToken = "invalid";
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withTTL(new Date().getTime() - 100000000 ).withToken(inValidToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        TokenExchange.TokenExchangeServiceCallback callBack = new TokenExchangeServiceCallbackMock();
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), eq(callBack))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((TokenExchange.TokenExchangeServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });
        TokenExchange.getDbToken(fakePartitionName, mHttpClient, "", "", callBack);
        Assert.assertEquals(fakeToken, ((TokenExchangeServiceCallbackMock) callBack).getTokenResult().token());
    }
}
