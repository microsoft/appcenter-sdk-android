package com.microsoft.appcenter.storage;

import android.content.Context;
import com.google.gson.Gson;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.storage.models.TokenResult;
import com.microsoft.appcenter.storage.models.TokensResponse;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
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
    public void canGetToken() throws Exception {
        spy(HttpUtils.class);
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        doReturn(mHttpClient).when(HttpUtils.class, "createHttpClient", any(Context.class));
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
            ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
            return mock(ServiceCall.class);
            }
        });
        Storage.getInstance().getDbToken(fakePartitionName, tokenResultFuture);
        Assert.assertEquals(fakeToken, tokenResultFuture.get().token());
    }

    @Test
    public void canReadTokenFromCacheWhenTokenValid() {
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withTTL(new Date().getTime() + 100000000 ).withToken(fakeToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        Storage.getInstance().getDbToken(fakePartitionName, tokenResultFuture);
        Assert.assertEquals(fakeToken, tokenResultFuture.get().token());
    }

    @Test
    public void canGetTokenWhenCacheInvalid() throws Exception {
        String inValidToken = "invalid";
        String tokenResult = new Gson().toJson(new TokenResult().withPartition(fakePartitionName).withTTL(new Date().getTime() - 100000000 ).withToken(inValidToken));
        when(SharedPreferencesManager.getString(fakePartitionName)).thenReturn(tokenResult);
        spy(HttpUtils.class);
        TokensResponse tokensResponse = new TokensResponse().withTokens(new ArrayList<>(Arrays.asList(new TokenResult().withToken(fakeToken))));
        final String expectedResponse = new Gson().toJson(tokensResponse);
        doReturn(mHttpClient).when(HttpUtils.class, "createHttpClient", any(Context.class));
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(expectedResponse, null);
                return mock(ServiceCall.class);
            }
        });
        Storage.getInstance().getDbToken(fakePartitionName, tokenResultFuture);
        Assert.assertEquals(fakeToken, tokenResultFuture.get().token());
    }
}
