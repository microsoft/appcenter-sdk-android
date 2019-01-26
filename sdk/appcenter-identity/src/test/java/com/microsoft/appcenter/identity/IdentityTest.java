package com.microsoft.appcenter.identity;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static com.microsoft.appcenter.identity.Constants.PREFERENCE_E_TAG_KEY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class IdentityTest extends AbstractIdentityTest {

    @NonNull
    private Channel start(Identity identity) {
        Channel channel = mock(Channel.class);
        identity.onStarting(mAppCenterHandler);
        identity.onStarted(mock(Context.class), channel, "", null, true);
        return channel;
    }

    @Test
    public void singleton() {
        Assert.assertSame(Identity.getInstance(), Identity.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Identity.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Identity.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {

        /* Before start it does not work to change state, it's disabled. */
        Identity identity = Identity.getInstance();
        Identity.setEnabled(true);
        assertFalse(Identity.isEnabled().get());
        Identity.setEnabled(false);
        assertFalse(Identity.isEnabled().get());

        /* Start. */
        Channel channel = start(identity);
        verify(channel).removeGroup(eq(identity.getGroupName()));
        verify(channel).addGroup(eq(identity.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Identity.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Identity.setEnabled(false).get();
        assertFalse(Identity.isEnabled().get());
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(IDENTITY_ENABLED_KEY, true)).thenReturn(false);
        Identity identity = Identity.getInstance();

        /* Start. */
        Channel channel = start(identity);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void downloadFullInvalidConfiguration() throws Exception {

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        start(Identity.getInstance());

        /* When we get an invalid payload. */
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallSucceeded("invalid", new HashMap<String, String>());

        /* We didn't attempt to even save. */
        verifyStatic();
        FileManager.write(any(File.class), anyString());
    }

    private void testInvalidConfig(JSONObject jsonConfig) throws Exception {

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Make not configured the only reason login is delayed => mock foreground. */
        identity.onActivityResumed(mock(Activity.class));

        /* Login, will be delayed until configuration ready. */
        Identity.login();

        /* When we get a payload valid for AppCenter fields but invalid for msal ones. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* We didn't attempt to even save. */
        verifyStatic();
        FileManager.write(any(File.class), anyString());

        /* Check we didn't try login after configuration attempt. */
        assertTrue(identity.isLoginDelayed());
    }

    @Test
    public void downloadInvalidForMSALConfiguration() throws Exception {
        testInvalidConfig(mockValidForAppCenterConfig());
    }

    @Test
    public void downloadConfigurationWithoutUrl() throws Exception {
        JSONObject jsonConfig = mock(JSONObject.class);
        when(jsonConfig.toString()).thenReturn("mockConfig");
        whenNew(JSONObject.class).withArguments("mockConfig").thenReturn(jsonConfig);
        JSONArray authorities = mock(JSONArray.class);
        when(jsonConfig.getJSONArray("authorities")).thenReturn(authorities);
        when(authorities.length()).thenReturn(1);
        JSONObject authority = mock(JSONObject.class);
        when(authorities.getJSONObject(0)).thenReturn(authority);
        when(authority.optBoolean("default")).thenReturn(true);
        when(authority.getString("type")).thenReturn("B2C");
        testInvalidConfig(jsonConfig);
    }

    @Test
    public void downloadConfigurationWithoutDefault() throws Exception {
        JSONObject jsonConfig = mock(JSONObject.class);
        when(jsonConfig.toString()).thenReturn("mockConfig");
        whenNew(JSONObject.class).withArguments("mockConfig").thenReturn(jsonConfig);
        JSONArray authorities = mock(JSONArray.class);
        when(jsonConfig.getJSONArray("authorities")).thenReturn(authorities);
        when(authorities.length()).thenReturn(1);
        JSONObject authority = mock(JSONObject.class);
        when(authorities.getJSONObject(0)).thenReturn(authority);
        when(authority.getString("type")).thenReturn("B2C");
        testInvalidConfig(jsonConfig);
    }

    @Test
    public void downloadConfigurationWithoutB2C() throws Exception {
        JSONObject jsonConfig = mock(JSONObject.class);
        when(jsonConfig.toString()).thenReturn("mockConfig");
        whenNew(JSONObject.class).withArguments("mockConfig").thenReturn(jsonConfig);
        JSONArray authorities = mock(JSONArray.class);
        when(jsonConfig.getJSONArray("authorities")).thenReturn(authorities);
        when(authorities.length()).thenReturn(1);
        JSONObject authority = mock(JSONObject.class);
        when(authorities.getJSONObject(0)).thenReturn(authority);
        when(authority.optBoolean("default")).thenReturn(true);
        testInvalidConfig(jsonConfig);
    }

    @Test
    public void downloadConfigurationWithEmptyAuthorities() throws Exception {
        JSONObject jsonConfig = mock(JSONObject.class);
        when(jsonConfig.toString()).thenReturn("mockConfig");
        whenNew(JSONObject.class).withArguments("mockConfig").thenReturn(jsonConfig);
        when(jsonConfig.getJSONArray("authorities")).thenReturn(mock(JSONArray.class));
        testInvalidConfig(jsonConfig);
    }

    @Test
    public void loginThenDownloadValidConfigurationThenForeground() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock foreground then background again. */
        identity.onActivityResumed(mock(Activity.class));
        identity.onActivityPaused(mock(Activity.class));

        /* Login, will be delayed until configuration ready. */
        Identity.login();

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration is cached. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_E_TAG_KEY, "mockETag");

        /* Verify login still delayed in background. */
        assertTrue(identity.isLoginDelayed());

        /* Go foreground. */
        Activity activity = mock(Activity.class);
        identity.onActivityResumed(activity);
        assertFalse(identity.isLoginDelayed());
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), callbackCaptor.capture());

        /* For now our callback does not do much except logging. */
        AuthenticationCallback callback = callbackCaptor.getValue();
        assertNotNull(callback);

        /* Just call back and nothing to verify. TODO update tests when callbacks implemented. */
        callback.onCancel();
        callback.onSuccess(mock(IAuthenticationResult.class));
        callback.onError(mock(MsalException.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenLogin() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock storage to fail caching configuration, this does not prevent login. */
        doThrow(new IOException()).when(FileManager.class);
        FileManager.write(any(File.class), anyString());

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* ETag not saved as file write failed. */
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_E_TAG_KEY, "mockETag");

        /* Go foreground. */
        Activity activity = mock(Activity.class);
        identity.onActivityResumed(activity);
        assertFalse(identity.isLoginDelayed());

        /* Login, will work now. */
        Identity.login();

        /* Verify login still delayed in background. */
        assertFalse(identity.isLoginDelayed());
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
    }

    @Test
    public void downloadConfigurationFailed() throws Exception {

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock foreground. */
        identity.onActivityResumed(mock(Activity.class));

        /* Mock http call fails. */
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallFailed(new HttpException(404));

        /* If we login. */
        Identity.login();

        /* Then nothing happens, we are delayed. */
        assertTrue(identity.isLoginDelayed());
    }

    private void mockSuccessfulHttpCall(JSONObject jsonConfig, HttpClientRetryer httpClient) throws JSONException {

        /* Intercept parameters. */
        ArgumentCaptor<HttpClient.CallTemplate> templateArgumentCaptor = ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), templateArgumentCaptor.capture(), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);

        /* Verify call template. */
        assertNull(templateArgumentCaptor.getValue().buildRequestBody());

        /* Verify no logging if verbose log not enabled (default). */
        try {
            templateArgumentCaptor.getValue().onBeforeCalling(new URL("https://mock"), new HashMap<String, String>());
        } catch (MalformedURLException e) {
            fail("test url should always be valid " + e.getMessage());
        }
        verifyStatic(never());
        AppCenterLog.verbose(anyString(), anyString());

        /* Simulate response. */
        HashMap<String, String> headers = new HashMap<>();
        headers.put("ETag", "mockETag");
        serviceCallback.onCallSucceeded(jsonConfig.toString(), headers);
    }

    @NonNull
    private JSONObject mockValidForAppCenterConfig() throws Exception {
        JSONObject jsonConfig = mock(JSONObject.class);
        when(jsonConfig.toString()).thenReturn("mockConfig");
        whenNew(JSONObject.class).withArguments("mockConfig").thenReturn(jsonConfig);
        JSONArray authorities = mock(JSONArray.class);
        when(jsonConfig.getJSONArray("authorities")).thenReturn(authorities);
        when(authorities.length()).thenReturn(1);
        JSONObject authority = mock(JSONObject.class);
        when(authorities.getJSONObject(0)).thenReturn(authority);
        when(authority.optBoolean("default")).thenReturn(true);
        when(authority.getString("type")).thenReturn("B2C");
        when(authority.getString("authority_url")).thenReturn("https://mock");
        return jsonConfig;
    }
}
