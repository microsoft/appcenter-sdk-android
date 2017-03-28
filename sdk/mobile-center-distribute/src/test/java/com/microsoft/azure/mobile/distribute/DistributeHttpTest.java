package com.microsoft.azure.mobile.distribute;

import android.content.Context;
import android.util.Log;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpUtils;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.NetworkStateHelper;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.azure.mobile.distribute.DistributeConstants.HEADER_API_TOKEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
public class DistributeHttpTest extends AbstractDistributeTest {

    @Test
    public void onBeforeCalling() throws Exception {

        /* Mock instances. */
        String urlFormat = "http://mock/path/%s/path/file";
        String appSecret = UUID.randomUUID().toString();
        String obfuscatedSecret = HttpUtils.hideSecret(appSecret);
        String apiToken = UUID.randomUUID().toString();
        String obfuscatedToken = HttpUtils.hideSecret(apiToken);
        URL url = new URL(String.format(urlFormat, appSecret));
        String obfuscatedUrlString = String.format(urlFormat, obfuscatedSecret);
        Map<String, String> headers = new HashMap<>();
        headers.put("Another-Header", "Another-Value");
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret, apiToken);
        when(MobileCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);
        mockStatic(MobileCenterLog.class);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic();
        MobileCenterLog.verbose(anyString(), contains(obfuscatedUrlString));

        /* Verify header log. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic();
            MobileCenterLog.verbose(anyString(), contains(header.getValue()));
        }

        /* Put api token to header. */
        headers.put(HEADER_API_TOKEN, apiToken);
        callTemplate.onBeforeCalling(url, headers);

        /* Verify app secret is in log. */
        verifyStatic();
        MobileCenterLog.verbose(anyString(), contains(obfuscatedToken));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onBeforeCallingWithAnotherLogLevel() throws Exception {

        /* Mock instances. */
        String appSecret = UUID.randomUUID().toString();
        String apiToken = UUID.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret, apiToken);

        /* Change log level. */
        when(MobileCenterLog.getLogLevel()).thenReturn(Log.WARN);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(mock(URL.class), mock(Map.class));

        /* Verify. */
        verifyStatic(never());
        MobileCenterLog.verbose(anyString(), anyString());
    }

    @Test
    public void buildRequestBody() throws Exception {

        /* Mock instances. */
        String appSecret = UUID.randomUUID().toString();
        String apiToken = UUID.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret, apiToken);

        /* Distribute don't have request body. Verify it. */
        Assert.assertNull(callTemplate.buildRequestBody());
    }

    private HttpClient.CallTemplate getCallTemplate(String appSecret, String apiToken) throws Exception {

        /* Configure mock HTTP to get an instance of IngestionCallTemplate. */
        Distribute.getInstance().onStarted(mContext, appSecret, mock(Channel.class));
        final ServiceCall call = mock(ServiceCall.class);
        final AtomicReference<HttpClient.CallTemplate> callTemplate = new AtomicReference<>();
        mockStatic(NetworkStateHelper.class);
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(mock(NetworkStateHelper.class));
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).then(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                callTemplate.set((HttpClient.CallTemplate) invocation.getArguments()[3]);
                return call;
            }
        });
        Distribute.getInstance().getLatestReleaseDetails(apiToken);
        return callTemplate.get();
    }
}
