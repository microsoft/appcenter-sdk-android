/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.auth;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.storage.FileManager;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAccount;
import com.microsoft.identity.client.IAccountIdentifier;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;

import static com.microsoft.appcenter.auth.Constants.HEADER_IF_NONE_MATCH;
import static com.microsoft.appcenter.auth.Constants.PREFERENCE_E_TAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Matchers.notNull;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest
public class AuthTest extends AbstractAuthTest {

    private static final String APP_SECRET = "5c9edcf2-d8d8-426d-8c20-817eb9378b08";

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @NonNull
    private static JSONObject mockValidForAppCenterConfig() throws Exception {
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

    private static void mockSuccessfulHttpCall(JSONObject jsonConfig, HttpClient httpClient) throws JSONException {
        ServiceCallback serviceCallback = mockHttpCallStarted(httpClient);
        mockHttpCallSuccess(jsonConfig, serviceCallback);
    }

    private static ServiceCallback mockHttpCallStarted(HttpClient httpClient) throws JSONException {

        /* Intercept parameters. */
        ArgumentCaptor<HttpClient.CallTemplate> templateArgumentCaptor = ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        String expectedUrl = Constants.DEFAULT_CONFIG_URL + "/auth/" + APP_SECRET + ".json";
        verify(httpClient).callAsync(eq(expectedUrl), anyString(), anyMapOf(String.class, String.class), templateArgumentCaptor.capture(), callbackArgumentCaptor.capture());
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
        return serviceCallback;
    }

    private static void mockHttpCallSuccess(JSONObject jsonConfig, ServiceCallback serviceCallback) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("ETag", "mockETag");
        serviceCallback.onCallSucceeded(jsonConfig.toString(), headers);
    }

    @Test
    public void singleton() {
        assertSame(Auth.getInstance(), Auth.getInstance());
    }

    @Test
    public void isAppSecretRequired() {
        assertTrue(Auth.getInstance().isAppSecretRequired());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Auth.getInstance().getLogFactories();
        assertNull(factories);
    }

    @Test
    public void setEnabled() {

        /* Before start it does not work to change state, it's disabled. */
        Auth auth = Auth.getInstance();
        Auth.setEnabled(true);
        assertFalse(Auth.isEnabled().get());
        Auth.setEnabled(false);
        assertFalse(Auth.isEnabled().get());

        /* Start. */
        Channel channel = start(auth);
        verify(channel).removeGroup(eq(auth.getGroupName()));
        verify(channel).addGroup(eq(auth.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));
        verify(mNetworkStateHelper).addListener(any(NetworkStateHelper.Listener.class));

        /* Now we can see the service enabled. */
        assertTrue(Auth.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Auth.setEnabled(false).get();
        assertFalse(Auth.isEnabled().get());
        verify(mAuthTokenContext).removeListener(any(AuthTokenContext.Listener.class));
        verify(mNetworkStateHelper).removeListener(any(NetworkStateHelper.Listener.class));
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void disablePersisted() {
        when(SharedPreferencesManager.getBoolean(AUTH_ENABLED_KEY, true)).thenReturn(false);
        Auth auth = Auth.getInstance();

        /* Start. */
        Channel channel = start(auth);
        verify(channel, never()).removeListener(any(Channel.Listener.class));
        verify(channel, never()).addListener(any(Channel.Listener.class));
    }

    @Test
    public void downloadFullInvalidConfiguration() throws Exception {

        /* Start auth service. */
        start(Auth.getInstance());

        /* When we get an invalid payload. */
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallSucceeded("invalid", new HashMap<String, String>());

        /* We didn't attempt to even save. */
        verifyStatic();
        FileManager.write(any(File.class), anyString());
    }

    private void testInvalidConfig(JSONObject jsonConfig) throws Exception {

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* When we get a payload valid for AppCenter fields but invalid for msal ones. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* We didn't attempt to even save. */
        verifyStatic();
        FileManager.write(any(File.class), anyString());
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
    public void verifyConfigurationDownloaded() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration is cached. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));
    }

    @Test
    public void disableServiceDuringDownloadingConfiguration() throws Exception {

        /* Mock config call. */
        ServiceCall getConfigCall = mock(ServiceCall.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class),
                any(ServiceCallback.class))).thenReturn(getConfigCall);

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        ServiceCallback serviceCallback = mockHttpCallStarted(mHttpClient);

        /* Disable Auth. */
        Auth.setEnabled(false);

        /* Simulate response. */
        mockHttpCallSuccess(jsonConfig, serviceCallback);

        /* Configuration is not cached. */
        verifyStatic(never());
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));
    }

    @Test
    public void signInIsCalledBeforeStart() {
        Auth auth = Auth.getInstance();

        @SuppressWarnings("unchecked")
        AppCenterConsumer<SignInResult> callback = (AppCenterConsumer<SignInResult>) Mockito.mock(AppCenterConsumer.class);

        /* Call twice for multiple callbacks before initialize. */
        Auth.signIn().thenAccept(callback);
        Auth.signIn().thenAccept(callback);
        start(auth);

        SignInResult result = Auth.signIn().get();
        assertNotNull(result.getException());
        assertTrue(result.getException() instanceof IllegalStateException);
        verify(callback, times(2)).accept(any(SignInResult.class));
    }

    @Test
    public void silentSignIn() throws Exception {

        /* Mock JSON. */
        mockValidForAppCenterConfig();

        /* Mock authentication result. */
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(any(Activity.class), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId));

        /* Verify. */
        assertNotNull(future);
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());
        assertEquals(mockIdToken, future.get().getUserInformation().getIdToken());
        assertEquals(mockAccessToken, future.get().getUserInformation().getAccessToken());
        assertEquals(mockAccountId, future.get().getUserInformation().getAccountId());
    }

    @Test
    public void silentSignInWithMissingIdToken() throws Exception {

        /* Mock JSON. */
        mockValidForAppCenterConfig();

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(null, mockAccessToken, mockHomeAccountId, mockAccountId);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);

        /* Start auth service. */
        mockReadyToSignIn();

        /* Silent sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockResult);

        /* Verify. */
        assertNotNull(future);
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());

        /* IdToken is missing in MSAL so accessToken will be filled. */
        assertEquals(mockAccessToken, future.get().getUserInformation().getIdToken());
        assertEquals(mockAccessToken, future.get().getUserInformation().getAccessToken());
        assertEquals(mockAccountId, future.get().getUserInformation().getAccountId());
        verify(mAuthTokenContext).setAuthToken(eq(mockAccessToken), eq(mockHomeAccountId), notNull(Date.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignIn() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        String idToken = UUID.randomUUID().toString();
        String accessToken = UUID.randomUUID().toString();
        String accountId = UUID.randomUUID().toString();
        String homeAccountId = UUID.randomUUID().toString();
        final IAuthenticationResult mockResult = mockAuthResult(idToken, accessToken, homeAccountId, accountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock storage to fail caching configuration, this does not prevent signIn. */
        doThrow(new IOException()).when(FileManager.class);
        FileManager.write(any(File.class), anyString());

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* ETag not saved as file write failed. */
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_E_TAG_KEY, "mockETag");

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in, will work now. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Check result. */
        SignInResult signInResult = future.get();
        assertNotNull(signInResult);
        assertNotNull(signInResult.getUserInformation());
        assertEquals(accountId, signInResult.getUserInformation().getAccountId());
        assertEquals(idToken, signInResult.getUserInformation().getIdToken());
        assertEquals(accessToken, signInResult.getUserInformation().getAccessToken());
        assertNull(signInResult.getException());

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(idToken), eq(homeAccountId), any(Date.class));

        /* Disable Auth. */
        Auth.setEnabled(false).get();

        /* Sign in with auth disabled. */
        future = Auth.signIn();

        /* Verify operation failed after disabling. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof IllegalStateException);
        assertNull(future.get().getUserInformation());

        /* Verify no more interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(idToken), eq(homeAccountId), any(Date.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignInThenSilentSignIn() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        IAccount mockAccount = mock(IAccount.class);
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[4]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireTokenSilentAsync(
                notNull(String[].class), any(IAccount.class), any(String.class), eq(true), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in, will work now. */
        Auth.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Auth.signIn();

        /* Verify interactions - should succeed silent sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class),
                any(String.class), eq(true), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext, times(2)).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignInButFailToRetrieveAccount() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(null);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in, will work now. */
        Auth.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignInThenCancelSilentSignIn() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        IAccount mockAccount = mock(IAccount.class);
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[4]).onCancel();
                return null;
            }
        }).when(publicClientApplication).acquireTokenSilentAsync(
                notNull(String[].class), any(IAccount.class), any(String.class), eq(true), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in, will work now. */
        Auth.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Auth.signIn();

        /* Verify interactions - should succeed silent sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class), any(String.class),
                eq(true), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignInThenFailSilentSignInWithMissingToken() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        IAccount mockAccount = mock(IAccount.class);
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();

        /* Always return empty cache. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[4]).onError(new MsalUiRequiredException("error"));
                return null;
            }
        }).when(publicClientApplication).acquireTokenSilentAsync(
                notNull(String[].class), any(IAccount.class), any(String.class), eq(true), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in, will work now. */
        Auth.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Auth.signIn();

        /* Verify interactions - should fail silent and fallback to interactive sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class),
                any(String.class), eq(true), notNull(AuthenticationCallback.class));
        verify(publicClientApplication, times(2)).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext, times(2)).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));
    }

    @Test
    public void silentSignFailsWithException() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        IAccount mockAccount = mock(IAccount.class);
        String mockIdToken = UUID.randomUUID().toString();
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();

        /* Always return empty cache. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccessToken, mockHomeAccountId, mockAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[4]).onError(new MsalClientException("error"));
                return null;
            }
        }).when(publicClientApplication).acquireTokenSilentAsync(
                notNull(String[].class), any(IAccount.class), any(String.class), eq(true), notNull(AuthenticationCallback.class));

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, mHttpClient);

        /* Go foreground. */
        auth.onActivityResumed(activity);

        /* Sign in. */
        Auth.signIn();

        /* Call signIn again to trigger silent sign-in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();
        SignInResult signInResult = future.get();
        assertNotNull(signInResult);
        assertNull(signInResult.getUserInformation());
        assertNotNull(signInResult.getException());
        assertTrue(signInResult.getException() instanceof MsalClientException);
    }

    private void testDownloadFailed(Exception e) {

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock foreground. */
        auth.onActivityResumed(mock(Activity.class));

        /* Mock http call fails. */
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), callbackArgumentCaptor.capture());
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallFailed(e);

        /* If we sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Then it fails due to configuration issue. */
        assertTrue(future.get().getException() instanceof IllegalStateException);
    }

    @Test
    public void downloadConfigurationFailedHttp() {
        testDownloadFailed(new HttpException(404));
    }

    @Test
    public void downloadConfigurationFailedNetwork() {
        testDownloadFailed(new IOException());
    }

    @Test
    public void doNotSignInWhenNoInternet() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Mock no network and auth. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock foreground. */
        auth.onActivityResumed(mock(Activity.class));

        /* If we sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Check result. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof NetworkErrorException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void testDoNotSignInInBackground() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Mock auth. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock foreground. */
        auth.onActivityPaused(mock(Activity.class));

        /* If we sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Check result. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof IllegalStateException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void doNotSignInInteractivelyWhenDisablingBeforeRunningInUiThread() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Hold UI thread callback. */
        doNothing().when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();
        Auth.setEnabled(false).get();

        /* Verify UI code that ran too late is canceled. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic();
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();
        verifyZeroInteractions(publicClientApplication);

        /* Check result. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof IllegalStateException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void readCacheAndRefreshNotModified() throws Exception {

        /* Mock verbose logs. */
        when(AppCenter.getLogLevel()).thenReturn(Log.VERBOSE);

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Mock ETag. */
        when(SharedPreferencesManager.getString(PREFERENCE_E_TAG_KEY)).thenReturn("cachedETag");

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock foreground. */
        Activity activity = mock(Activity.class);
        auth.onActivityResumed(activity);

        /* We can signIn right away even when http call has not yet finished. */
        Auth.signIn();
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Check http call. */
        ArgumentCaptor<HttpClient.CallTemplate> templateArgumentCaptor = ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), mHeadersCaptor.capture(), templateArgumentCaptor.capture(), callbackArgumentCaptor.capture());

        /* Check ETag was used. */
        Map<String, String> headers = mHeadersCaptor.getValue();
        assertNotNull(headers);
        assertEquals("cachedETag", headers.get(HEADER_IF_NONE_MATCH));

        /* Check headers url/headers was logged. */
        templateArgumentCaptor.getValue().onBeforeCalling(new URL("https://mock"), headers);
        verifyStatic(atLeastOnce());
        AppCenterLog.verbose(anyString(), anyString());

        /* Simulate response 304 not modified. */
        ServiceCallback serviceCallback = callbackArgumentCaptor.getValue();
        assertNotNull(serviceCallback);
        serviceCallback.onCallFailed(new HttpException(304));

        /* Configuration not refreshed. */
        verifyNew(PublicClientApplication.class, times(1));
    }

    @Test
    public void cancelSignIn() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Simulate cancel. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onCancel();

        /* Verify error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof CancellationException);
        assertNull(future.get().getUserInformation());
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void signInFails() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Simulate failure. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onError(mock(MsalException.class));

        /* Verify error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof MsalException);
        assertNull(future.get().getUserInformation());
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void signInTwiceFails() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future1 = Auth.signIn();
        AppCenterFuture<SignInResult> future2 = Auth.signIn();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockAuthResult("idToken", "accessToken", "homeAccountId", "accountId"));

        /* Verify success on first call. */
        assertNotNull(future1.get());
        assertNull(future1.get().getException());
        assertNotNull(future1.get().getUserInformation());
        assertEquals("accountId", future1.get().getUserInformation().getAccountId());
        assertEquals("idToken", future1.get().getUserInformation().getIdToken());
        assertEquals("accessToken", future1.get().getUserInformation().getAccessToken());

        /* Verify error on second one. */
        assertNotNull(future2.get());
        assertTrue(future2.get().getException() instanceof IllegalStateException);
        assertNull(future2.get().getUserInformation());
    }

    @Test
    public void signInSucceedsInMSALButDisableSdkBeforeProcessingResult() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Disable SDK before it succeeds. */
        Auth.setEnabled(false).get();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockAuthResult("idToken", "accessToken", "homeAccountId", "accountId"));

        /* Verify disabled error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof IllegalStateException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void signInReturnsNullIdToken() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Simulate success but with null id token. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        IAuthenticationResult authenticationResult = mockAuthResult(null, "accessToken", "homeAccountId", "accountId");
        callbackCaptor.getValue().onSuccess(authenticationResult);

        /* Verify result and behavior. */
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());
        assertEquals("accessToken", future.get().getUserInformation().getIdToken());
        assertEquals("accessToken", future.get().getUserInformation().getAccessToken());
        assertEquals("accountId", future.get().getUserInformation().getAccountId());
        verify(mAuthTokenContext).setAuthToken(eq("accessToken"), eq("homeAccountId"), notNull(Date.class));
    }

    @Test
    public void signOutRemovesToken() {
        Auth auth = Auth.getInstance();
        when(mAuthTokenContext.getAuthToken()).thenReturn("42");
        start(auth);

        /* Sign out should clear token. */
        Auth.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void signOutCancelsCanceledSignIn() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        mockReadyToSignIn();

        /* Sign in. */
        Auth.signIn();
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));

        /* Sign out. */
        Auth.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Simulate cancel. */
        callbackCaptor.getValue().onCancel();

        /* Verify signIn was cancelled. */
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), notNull(Date.class));
    }

    @Test
    public void signOutCancelsFailedSignIn() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        mockReadyToSignIn();

        /* Sign in. */
        Auth.signIn();
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));

        /* Sign out. */
        Auth.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Simulate error. */
        callbackCaptor.getValue().onError(mock(MsalException.class));

        /* Verify signIn was cancelled. */
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), notNull(Date.class));
    }

    @Test
    public void signOutCancelsSignIn() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult("idToken", mockAccessToken, mockHomeAccountId, "accountId");

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        mockReadyToSignIn();

        /* Sign in. */
        Auth.signIn();
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));

        /* Sign out. */
        Auth.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Simulate success. */
        callbackCaptor.getValue().onSuccess(mockResult);

        /* Verify signIn was cancelled. */
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), notNull(Date.class));
    }

    @Test
    public void signOutWithoutNetworkCancelsPendingRefresh() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> authTokenContextListenerCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(authTokenContextListenerCaptor.capture());
        ArgumentCaptor<NetworkStateHelper.Listener> networkStateListenerCaptor = ArgumentCaptor.forClass(NetworkStateHelper.Listener.class);
        doNothing().when(mNetworkStateHelper).addListener(networkStateListenerCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();

        /* Mock no network and auth. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(mock(IAccount.class));
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));
        verify(mNetworkStateHelper).addListener(any(NetworkStateHelper.Listener.class));

        /* Simulate offline. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        networkStateListenerCaptor.getValue().onNetworkStateUpdated(false);

        /* Request token refresh. */
        authTokenContextListenerCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check that we don't try to update it without network and don't clear the current one. */
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Sign out. */
        Auth.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Come back online. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        networkStateListenerCaptor.getValue().onNetworkStateUpdated(true);

        /* Check that signOut() canceled token refresh. */
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), notNull(Date.class));
    }

    @Test
    public void refreshTokenWithoutAccount() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check token is cleared when account is null. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void refreshToken() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(mock(IAccount.class));
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check that we acquire new token silently and don't clear the current one. */
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void refreshTokenWithoutNetwork() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> authTokenContextListenerCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(authTokenContextListenerCaptor.capture());
        ArgumentCaptor<NetworkStateHelper.Listener> networkStateListenerCaptor = ArgumentCaptor.forClass(NetworkStateHelper.Listener.class);
        doNothing().when(mNetworkStateHelper).addListener(networkStateListenerCaptor.capture());

        /* Mock no network and auth. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(mock(IAccount.class));
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));
        verify(mNetworkStateHelper).addListener(any(NetworkStateHelper.Listener.class));

        /* Request token refresh. */
        authTokenContextListenerCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check that we don't try to update it without network and don't clear the current one. */
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Come back online. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        networkStateListenerCaptor.getValue().onNetworkStateUpdated(true);

        /* Check that we acquire new token silently. */
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
    }

    @Test
    public void refreshTokenDoesNotHaveUiFallback() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> authTokenContextListenerCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(authTokenContextListenerCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(mock(IAccount.class));
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[4]).onError(new MsalUiRequiredException("error"));
                return null;
            }
        }).when(publicClientApplication).acquireTokenSilentAsync(
                notNull(String[].class), any(IAccount.class), any(String.class), eq(true), notNull(AuthenticationCallback.class));
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));

        /* Request token refresh. */
        authTokenContextListenerCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check that we don't try to show UI as fallback. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
        verify(publicClientApplication, never()).acquireToken(any(Activity.class), any(String[].class), any(AuthenticationCallback.class));
    }

    @Test
    public void refreshTokenMultipleTimes() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(mock(IAccount.class));
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));

        /* Request token refresh multiple times. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Check that we acquire new token only once. */
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void refreshTokenWithoutConfig() {
        ArgumentCaptor<AuthTokenContext.Listener> authTokenContextListenerCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(authTokenContextListenerCaptor.capture());

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Mock foreground. */
        auth.onActivityResumed(mock(Activity.class));

        /* Connect to online with token to update. */
        authTokenContextListenerCaptor.getValue().onTokenRequiresRefresh("accountId");

        /* Verify sign out cleared token. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void refreshTokenDuringSignIn() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(null, mockAccessToken, mockHomeAccountId, mockAccountId);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), callbackCaptor.capture());

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh(mockHomeAccountId);

        /* Verify acquireTokenSilentAsync is not called twice since onTokenRequiresRefresh has been invoked. */
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), any(IAccount.class), anyString(), anyBoolean(), any(AuthenticationCallback.class));
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));

        /* Simulate success. */
        callbackCaptor.getValue().onSuccess(mockResult);

        /* Verify. */
        assertNotNull(future);
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());
        assertEquals(mockAccessToken, future.get().getUserInformation().getIdToken());
        assertEquals(mockAccessToken, future.get().getUserInformation().getAccessToken());
        assertEquals(mockAccountId, future.get().getUserInformation().getAccountId());
        verify(mAuthTokenContext).setAuthToken(eq(mockAccessToken), eq(mockHomeAccountId), notNull(Date.class));
    }

    @Test
    public void signInDuringRefreshToken() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(null, mockAccessToken, mockHomeAccountId, mockAccountId);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        mockReadyToSignIn();

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh(mockHomeAccountId);
        ArgumentCaptor<AuthenticationCallback> tokenRefreshCallbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), tokenRefreshCallbackCaptor.capture());

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();
        ArgumentCaptor<AuthenticationCallback> signInCallbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);

        /* Verify acquireTokenSilentAsync called both from refreshToken() and Auth.signIn(). */
        verify(publicClientApplication, times(2)).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), signInCallbackCaptor.capture());
        verify(mAuthTokenContext, never()).setAuthToken(anyString(), anyString(), any(Date.class));

        /* Simulate Sign-In success. */
        signInCallbackCaptor.getValue().onSuccess(mockResult);
        tokenRefreshCallbackCaptor.getValue().onSuccess(mockResult);

        /* Verify. */
        assertNotNull(future);
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());
        assertEquals(mockAccessToken, future.get().getUserInformation().getIdToken());
        assertEquals(mockAccessToken, future.get().getUserInformation().getAccessToken());
        assertEquals(mockAccountId, future.get().getUserInformation().getAccountId());

        /* Verify called only once. */
        verify(mAuthTokenContext).setAuthToken(eq(mockAccessToken), eq(mockHomeAccountId), notNull(Date.class));
    }

    @Test
    public void signOutDuringRefreshToken() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock auth token. */
        String mockAccessToken = UUID.randomUUID().toString();
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);

        /* Mock authentication result. */
        String mockHomeAccountId = UUID.randomUUID().toString();
        final IAuthenticationResult mockResult = mockAuthResult("idToken", "accessToken", "homeAccountId", "accountId");

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mock(IAccount.class));
        mockReadyToSignIn();

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh(mockHomeAccountId);
        ArgumentCaptor<AuthenticationCallback> tokenRefreshCallbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), tokenRefreshCallbackCaptor.capture());

        /* Sign out. */
        Auth.signOut();

        /* Verify sign out cleared token. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Simulate Sign-In success. */
        tokenRefreshCallbackCaptor.getValue().onSuccess(mockResult);

        /* Verify not called second time, cause sign out should cancel refreshToken(). */
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), any(Date.class));
    }

    @Test
    public void disableDuringRefreshToken() throws Exception {

        /* Capture Listener to call onTokenRequiresRefresh later. */
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock auth token. */
        String mockAccessToken = UUID.randomUUID().toString();
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);

        /* Mock authentication result. */
        String mockHomeAccountId = UUID.randomUUID().toString();
        final IAuthenticationResult mockResult = mockAuthResult("idToken", "accessToken", "homeAccountId", "accountId");

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mock(IAccount.class));
        mockReadyToSignIn();

        /* Request token refresh. */
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh(mockHomeAccountId);
        ArgumentCaptor<AuthenticationCallback> tokenRefreshCallbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), tokenRefreshCallbackCaptor.capture());

        /* Disable Auth. */
        Auth.setEnabled(false);

        /* Verify sign out cleared token. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));

        /* Simulate Sign-In success. */
        tokenRefreshCallbackCaptor.getValue().onSuccess(mockResult);

        /* Verify not called second time, cause sign out should cancel refreshToken(). */
        verify(mAuthTokenContext, never()).setAuthToken(notNull(String.class), notNull(String.class), any(Date.class));
    }

    @Test
    public void verifyTokenStorageIsEmpty() {
        Auth auth = Auth.getInstance();
        start(auth);
        Auth.signOut();
        when(AppCenter.getLogLevel()).thenReturn(Log.WARN);
        verify(mAuthTokenContext, never()).setAuthToken(any(String.class), any(String.class), any(Date.class));
    }

    @Test
    public void testRemoveAccountWhenClientAppIsNull() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(null);
        Auth auth = Auth.getInstance();
        start(auth);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(UUID.randomUUID().toString());
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUID.randomUUID().toString());
        Auth.signOut();
        verify(publicClientApplication, never()).getAccount(anyString(), anyString());
    }

    @Test
    public void testRemoveAccountWhenAccountIdentifierIsNull() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Auth auth = Auth.getInstance();
        start(auth);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null);
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUID.randomUUID().toString());
        Auth.signOut();
        verify(publicClientApplication, never()).getAccount(anyString(), anyString());
    }

    @Test
    public void removeAccount() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Mock MSAL client. */
        String mockHomeAccountId = UUID.randomUUID().toString();
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Check removing account */
        Auth auth = Auth.getInstance();
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUID.randomUUID().toString());
        start(auth);
        IAccount account = mock(IAccount.class);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn(mockHomeAccountId);
        when(account.getHomeAccountIdentifier()).thenReturn(accountIdentifier);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(account);
        Auth.signOut();
        ArgumentCaptor<PublicClientApplication.AccountsRemovedCallback> accountsRemovedCallbackArgumentCaptor = ArgumentCaptor.forClass(PublicClientApplication.AccountsRemovedCallback.class);
        verify(publicClientApplication).removeAccount(eq(account), accountsRemovedCallbackArgumentCaptor.capture());
        assertNotNull(accountsRemovedCallbackArgumentCaptor.getValue());

        /* Invoke the removed account callback, it just logs so we just need to test it does not crash. */
        accountsRemovedCallbackArgumentCaptor.getValue().onAccountsRemoved(true);
    }

    @Test
    public void removeAccountTestAccountNull() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Mock MSAL client. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(publicClientApplication.getAccount(anyString(), anyString())).thenReturn(null);
        IAccount account = mock(IAccount.class);
        Auth auth = Auth.getInstance();
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(UUID.randomUUID().toString());
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUID.randomUUID().toString());
        start(auth);
        Auth.signOut();
        verify(publicClientApplication, never()).removeAccount(eq(account), any(PublicClientApplication.AccountsRemovedCallback.class));
    }

    @Test
    public void removeAccountWithWrongIdentifier() throws Exception {

        /* Mock valid config. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock cached config file. */
        File file = mock(File.class);
        whenNew(File.class)
                .withParameterTypes(File.class, String.class)
                .withArguments(any(File.class), eq(Constants.FILE_PATH))
                .thenReturn(file);
        when(file.exists()).thenReturn(true);
        String config = jsonConfig.toString();
        when(FileManager.read(file)).thenReturn(config);

        /* Check removing account with different identifiers */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Auth auth = Auth.getInstance();
        start(auth);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn("5");
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUID.randomUUID().toString());
        IAccount account = mock(IAccount.class);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn("10");
        when(account.getHomeAccountIdentifier()).thenReturn(accountIdentifier);
        Auth.signOut();
        verify(publicClientApplication, never()).removeAccount(eq(account), any(PublicClientApplication.AccountsRemovedCallback.class));
    }

    @Test
    public void signInWithoutConfigBeingCalled() {

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();
        assertNotNull(future);
        assertNotNull(future.get().getException());
    }

    @Test
    public void signInContinuesAfterConfigDownloaded() throws Exception {
        ArgumentCaptor<AuthenticationCallback> signInCallbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);

        /* Mock config call. */
        ServiceCall getConfigCall = mock(ServiceCall.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class),
                any(ServiceCallback.class))).thenReturn(getConfigCall);

        /* Mock authentication result. */
        String mockAccessToken = UUID.randomUUID().toString();
        String mockAccountId = UUID.randomUUID().toString();
        String mockHomeAccountId = UUID.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(null, mockAccessToken, mockHomeAccountId, mockAccountId);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        when(mAuthTokenContext.getAuthToken()).thenReturn(mockAccessToken);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Start config downloading. */
        ServiceCallback serviceCallback = mockHttpCallStarted(mHttpClient);

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Verify that future is not completed, while config is being downloaded. */
        assertNotNull(future);
        assertFalse(future.isDone());
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), signInCallbackCaptor.capture());

        /* Simulate download configuration response. */
        mockHttpCallSuccess(mockValidForAppCenterConfig(), serviceCallback);
        verify(publicClientApplication).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), signInCallbackCaptor.capture());

        /* Simulate Sign-In success. */
        signInCallbackCaptor.getValue().onSuccess(mockResult);

        /* Verify Sign-in success. */
        assertNotNull(future);
        assertNotNull(future.get());
        assertNull(future.get().getException());
        assertNotNull(future.get().getUserInformation());
        assertEquals(mockAccountId, future.get().getUserInformation().getAccountId());
        assertEquals(mockAccessToken, future.get().getUserInformation().getAccessToken());
    }

    @Test
    public void signInFailedAfterConfigDownloadingFailedNoNetwork() throws Exception {
        signInFailedAfterConfigDownloadingFailed(new IOException());
    }

    @Test
    public void signInFailedAfterConfigDownloadingHttpFailed() throws Exception {
        signInFailedAfterConfigDownloadingFailed(new HttpException(304));
    }

    private void signInFailedAfterConfigDownloadingFailed(Exception e) throws Exception {

        /* Mock config call. */
        ServiceCall getConfigCall = mock(ServiceCall.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class),
                any(ServiceCallback.class))).thenReturn(getConfigCall);

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Start config downloading. */
        ServiceCallback serviceCallback = mockHttpCallStarted(mHttpClient);

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Auth.signIn();

        /* Verify that future is not completed, while config is being downloaded. */
        assertNotNull(future);
        assertFalse(future.isDone());
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), any(AuthenticationCallback.class));

        /* Simulate download configuration response. */
        serviceCallback.onCallFailed(e);

        /* Verify that sign in failed, cause config downloading failed. */
        assertNotNull(future);
        assertTrue(future.isDone());
        assertNotNull(future.get().getException());
        verify(publicClientApplication, never()).acquireTokenSilentAsync(any(String[].class), notNull(IAccount.class), isNull(String.class), eq(true), any(AuthenticationCallback.class));
    }

    private void mockReadyToSignIn() throws Exception {

        /* Start auth service. */
        Auth auth = Auth.getInstance();
        start(auth);

        /* Download configuration. */
        mockSuccessfulHttpCall(mockValidForAppCenterConfig(), mHttpClient);

        /* Mock foreground. */
        auth.onActivityResumed(mock(Activity.class));
    }

    @Test
    public void setConfigUrl() {

        /* Set url before start. */
        String configUrl = "https://config.contoso.com";
        Auth.setConfigUrl(configUrl);

        /* Start auth service. */
        start(Auth.getInstance());

        /* Check call. */
        String expectedUrl = configUrl + "/auth/" + APP_SECRET + ".json";
        verify(mHttpClient).callAsync(eq(expectedUrl), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @NonNull
    private Channel start(Auth auth) {
        Channel channel = mock(Channel.class);
        auth.onStarting(mAppCenterHandler);
        auth.onStarted(mock(Context.class), channel, APP_SECRET, null, true);
        return channel;
    }
}
