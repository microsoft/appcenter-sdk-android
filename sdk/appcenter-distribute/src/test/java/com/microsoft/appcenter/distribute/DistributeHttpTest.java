package com.microsoft.appcenter.distribute;

import android.content.Context;
import android.util.Log;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.NetworkStateHelper;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.appcenter.distribute.DistributeConstants.HEADER_API_TOKEN;
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
    public void onBeforeCallingWithToken() throws Exception {

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
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);
        mockStatic(AppCenterLog.class);

        /* Put api token to header. */
        headers.put(HEADER_API_TOKEN, apiToken);

         /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), contains(obfuscatedUrlString));

        /* Verify header logs. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic();
            if (header.getKey().equals(HEADER_API_TOKEN)) {
                AppCenterLog.verbose(anyString(), contains(obfuscatedToken));
            } else {
                AppCenterLog.verbose(anyString(), contains(header.getValue()));
            }
        }
    }

    @Test
    public void onBeforeCallingWithoutToken() throws Exception {

        /* Mock instances. */
        String urlFormat = "http://mock/path/%s/path/file";
        String appSecret = UUID.randomUUID().toString();
        String obfuscatedSecret = HttpUtils.hideSecret(appSecret);
        URL url = new URL(String.format(urlFormat, appSecret));
        String obfuscatedUrlString = String.format(urlFormat, obfuscatedSecret);
        Map<String, String> headers = new HashMap<>();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret, null);
        when(AppCenterLog.getLogLevel()).thenReturn(Log.VERBOSE);
        mockStatic(AppCenterLog.class);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(url, headers);

        /* Verify url log. */
        verifyStatic();
        AppCenterLog.verbose(anyString(), contains(obfuscatedUrlString));

        /* Verify header log. */
        for (Map.Entry<String, String> header : headers.entrySet()) {
            verifyStatic();
            AppCenterLog.verbose(anyString(), contains(header.getValue()));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void onBeforeCallingWithAnotherLogLevel() throws Exception {

        /* Mock instances. */
        String appSecret = UUID.randomUUID().toString();
        String apiToken = UUID.randomUUID().toString();
        HttpClient.CallTemplate callTemplate = getCallTemplate(appSecret, apiToken);

        /* Change log level. */
        when(AppCenterLog.getLogLevel()).thenReturn(Log.WARN);

        /* Call onBeforeCalling with parameters. */
        callTemplate.onBeforeCalling(mock(URL.class), mock(Map.class));

        /* Verify. */
        verifyStatic(never());
        AppCenterLog.verbose(anyString(), anyString());
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
        Distribute.getInstance().onStarting(mAppCenterHandler);
        Distribute.getInstance().onStarted(mContext, appSecret, null, mock(Channel.class));
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
        Distribute.getInstance().getLatestReleaseDetails("mockGroup", apiToken);
        return callTemplate.get();
    }
}
