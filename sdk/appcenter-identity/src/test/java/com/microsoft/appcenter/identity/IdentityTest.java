/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.identity;

import android.accounts.NetworkErrorException;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientRetryer;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.Ingestion;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;
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
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

import static com.microsoft.appcenter.identity.Constants.HEADER_IF_NONE_MATCH;
import static com.microsoft.appcenter.identity.Constants.PREFERENCE_E_TAG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
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
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({NetworkStateHelper.class})
public class IdentityTest extends AbstractIdentityTest {

    private static final String APP_SECRET = "5c9edcf2-d8d8-426d-8c20-817eb9378b08";

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @Test
    public void singleton() {
        assertSame(Identity.getInstance(), Identity.getInstance());
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
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
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

        /* Make not configured the only reason signIn is delayed => mock foreground. */
        identity.onActivityResumed(mock(Activity.class));

        /* Sign in, will be delayed until configuration ready. */
        Identity.signIn();

        /* When we get a payload valid for AppCenter fields but invalid for msal ones. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

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
    public void signInThenDownloadValidConfiguration() throws Exception {

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

        /* Sign in, will be delayed until configuration ready. */
        Identity.signIn();

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration is cached. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));
        verifyStatic();
    }

    @Test
    public void signInThenDownloadValidConfigurationThenForegroundThenSilentSignIn() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication result. */
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();
        IAccount mockAccount = mock(IAccount.class);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);

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


        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Go foreground. */
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(null);
        Identity.signIn();
    }

    @Test
    public void downloadConfigurationThenForegroundThenSignIn() throws Exception {

        /* Mock JSON. */
        JSONObject jsonConfig = mockValidForAppCenterConfig();

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        Activity activity = mock(Activity.class);
        String idToken = UUIDUtils.randomUUID().toString();
        String accountId = UUIDUtils.randomUUID().toString();
        String homeAccountId = UUIDUtils.randomUUID().toString();
        final IAuthenticationResult mockResult = mockAuthResult(idToken, accountId, homeAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock storage to fail caching configuration, this does not prevent signIn. */
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
        identity.onActivityResumed(activity);

        /* Sign in, will work now. */
        AppCenterFuture<SignInResult> future = Identity.signIn();

        /* Check result. */
        SignInResult signInResult = future.get();
        assertNotNull(signInResult);
        assertNotNull(signInResult.getUserInformation());
        assertEquals(accountId, signInResult.getUserInformation().getAccountId());
        assertNull(signInResult.getException());

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(idToken), eq(homeAccountId), any(Date.class));

        /* Disable Identity. */
        Identity.setEnabled(false).get();

        /* Sign in with identity disabled. */
        future = Identity.signIn();

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
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);
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

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        identity.onActivityResumed(activity);

        /* Sign in, will work now. */
        Identity.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

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
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(null);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((AuthenticationCallback) invocationOnMock.getArguments()[2]).onSuccess(mockResult);
                return null;
            }
        }).when(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        identity.onActivityResumed(activity);

        /* Sign in, will work now. */
        Identity.signIn();

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
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();

        /* First time do interactive by returning empty cache then return saved token. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);
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

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        identity.onActivityResumed(activity);

        /* Sign in, will work now. */
        Identity.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

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
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();

        /* Always return empty cache. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);
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

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Verify configuration caching attempted. */
        verifyStatic();
        String configPayload = jsonConfig.toString();
        FileManager.write(notNull(File.class), eq(configPayload));

        /* Go foreground. */
        identity.onActivityResumed(activity);

        /* Sign in, will work now. */
        Identity.signIn();

        /* Verify interactions. */
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mAuthTokenContext).setAuthToken(eq(mockIdToken), eq(mockHomeAccountId), any(Date.class));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

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
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();

        /* Always return empty cache. */
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);
        final IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);
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

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(jsonConfig, httpClient);

        /* Go foreground. */
        identity.onActivityResumed(activity);

        /* Sign in. */
        Identity.signIn();

        /* Call signIn again to trigger silent sign-in. */
        AppCenterFuture<SignInResult> future = Identity.signIn();

        SignInResult signInResult = future.get();
        assertNotNull(signInResult);
        assertNull(signInResult.getUserInformation());
        assertNotNull(signInResult.getException());
        assertTrue(signInResult.getException() instanceof MsalClientException);
    }

    private void testDownloadFailed(Exception e) throws Exception {

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
        serviceCallback.onCallFailed(e);

        /* If we sign in. */
        Identity.signIn();

        /* Then nothing happens, we are delayed. */
        AppCenterLog.error(anyString(), anyString());
        verifyStatic();
    }

    @Test
    public void downloadConfigurationFailedHttp() throws Exception {
        testDownloadFailed(new HttpException(404));
    }

    @Test
    public void downloadConfigurationFailedNetwork() throws Exception {
        testDownloadFailed(new IOException());
    }

    @Test
    public void testDoNotSignInWhenNoInternet() throws Exception {

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

        /* Mock no network and identity. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockStatic(NetworkStateHelper.class);
        NetworkStateHelper networkStateHelper = Mockito.mock(NetworkStateHelper.class, new Returns(true));
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(networkStateHelper);
        when(networkStateHelper.isNetworkConnected()).thenReturn(false);
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock foreground. */
        identity.onActivityResumed(mock(Activity.class));

        /* If we sign in. */
        AppCenterFuture<SignInResult> future = Identity.signIn();

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

        /* Mock no network and identity. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockStatic(NetworkStateHelper.class);
        NetworkStateHelper networkStateHelper = Mockito.mock(NetworkStateHelper.class, new Returns(true));
        when(NetworkStateHelper.getSharedInstance(any(Context.class))).thenReturn(networkStateHelper);
        when(networkStateHelper.isNetworkConnected()).thenReturn(true);
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock foreground. */
        identity.onActivityPaused(mock(Activity.class));

        /* If we sign in. */
        AppCenterFuture<SignInResult> future = Identity.signIn();

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
        AppCenterFuture<SignInResult> future = Identity.signIn();
        Identity.setEnabled(false).get();

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

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Mock foreground. */
        Activity activity = mock(Activity.class);
        identity.onActivityResumed(activity);

        /* We can signIn right away even when http call has not yet finished. */
        Identity.signIn();
        verify(publicClientApplication).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));

        /* Check http call. */
        ArgumentCaptor<HttpClient.CallTemplate> templateArgumentCaptor = ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(httpClient).callAsync(anyString(), anyString(), mHeadersCaptor.capture(), templateArgumentCaptor.capture(), callbackArgumentCaptor.capture());

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
        AppCenterFuture<SignInResult> future = Identity.signIn();

        /* Simulate cancel. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onCancel();

        /* Verify error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof CancellationException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void signInFails() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future = Identity.signIn();

        /* Simulate failure. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onError(mock(MsalException.class));

        /* Verify error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof MsalException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void signInTwiceFails() throws Exception {

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();

        /* Sign in. */
        AppCenterFuture<SignInResult> future1 = Identity.signIn();
        AppCenterFuture<SignInResult> future2 = Identity.signIn();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockAuthResult("idToken", "accountId", "homeAccountId"));

        /* Verify success on first call. */
        assertNotNull(future1.get());
        assertNull(future1.get().getException());
        assertNotNull(future1.get().getUserInformation());
        assertEquals("accountId", future1.get().getUserInformation().getAccountId());

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
        AppCenterFuture<SignInResult> future = Identity.signIn();

        /* Disable SDK before it succeeds. */
        Identity.setEnabled(false).get();

        /* Simulate success. */
        ArgumentCaptor<AuthenticationCallback> callbackCaptor = ArgumentCaptor.forClass(AuthenticationCallback.class);
        verify(publicClientApplication).acquireToken(notNull(Activity.class), notNull(String[].class), callbackCaptor.capture());
        callbackCaptor.getValue().onSuccess(mockAuthResult("idToken", "accountId", "homeAccountId"));

        /* Verify disabled error. */
        assertNotNull(future.get());
        assertTrue(future.get().getException() instanceof IllegalStateException);
        assertNull(future.get().getUserInformation());
    }

    @Test
    public void signOutRemovesToken() {
        Identity identity = Identity.getInstance();
        when(mAuthTokenContext.getAuthToken()).thenReturn("42");
        start(identity);

        /* Sign out should clear token. */
        Identity.signOut();
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void testListenerCalledOnTokenRefreshAccountIsNull() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        mockReadyToSignIn();
        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));
        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("accountId");
        List<AuthTokenInfo> tokenInfoList = Collections.singletonList(new AuthTokenInfo("token", null, new Date()));
        AuthTokenInfo tokenInfo = tokenInfoList.get(0);
        when(publicClientApplication.getAccount(anyString(), anyString())).thenReturn(null);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(tokenInfo);

        /* Check token is cleared when account is null. */
        verify(mAuthTokenContext).setAuthToken(isNull(String.class), isNull(String.class), isNull(Date.class));
    }

    @Test
    public void testListenerCalledOnTokenRefreshAccount() throws Exception {
        ArgumentCaptor<AuthTokenContext.Listener> listenerArgumentCaptor = ArgumentCaptor.forClass(AuthTokenContext.Listener.class);
        doNothing().when(mAuthTokenContext).addListener(listenerArgumentCaptor.capture());

        /* Mock authentication lib. */
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();

        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(mockValidForAppCenterConfig(), httpClient);

        /* Mock foreground. */
        identity.onActivityResumed(mock(Activity.class));

        verify(mAuthTokenContext).addListener(any(AuthTokenContext.Listener.class));
        IAccount account = mock(IAccount.class);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn("accountId");
        when(account.getHomeAccountIdentifier()).thenReturn(accountIdentifier);
        when(publicClientApplication.getAccount(eq("accountId"), anyString())).thenReturn(account);
        when(publicClientApplication.getAccount(anyString(), anyString())).thenReturn(account);

        listenerArgumentCaptor.getValue().onTokenRequiresRefresh("randomAsccountId");
        List<AuthTokenInfo> tokenInfoList = Collections.singletonList(new AuthTokenInfo("token", null, new Date()));
        AuthTokenInfo tokenInfo = tokenInfoList.get(0);
        mAuthTokenContext.checkIfTokenNeedsToBeRefreshed(tokenInfo);

        /* Check token is cleared when account is null. */
        PowerMockito.verifyPrivate(identity).invoke("silentSignIn", account);
    }

    @Test
    public void verifyTokenStorageIsEmpty() {
        Identity identity = Identity.getInstance();
        start(identity);
        Identity.signOut();
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
        Identity identity = Identity.getInstance();
        start(identity);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(UUIDUtils.randomUUID().toString());
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUIDUtils.randomUUID().toString());
        Identity.signOut();
        verify(publicClientApplication, never()).getAccounts(any(PublicClientApplication.AccountsLoadedListener.class));
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
        Identity identity = Identity.getInstance();
        start(identity);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(null);
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUIDUtils.randomUUID().toString());
        Identity.signOut();
        verify(publicClientApplication, never()).getAccounts(any(PublicClientApplication.AccountsLoadedListener.class));
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
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();
        PublicClientApplication publicClientApplication = mock(PublicClientApplication.class);
        whenNew(PublicClientApplication.class).withAnyArguments().thenReturn(publicClientApplication);

        /* Check removing account */
        Identity identity = Identity.getInstance();
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUIDUtils.randomUUID().toString());
        start(identity);
        final List<IAccount> accountsList = new ArrayList<>();
        IAccount account = mock(IAccount.class);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn(mockHomeAccountId);
        when(account.getHomeAccountIdentifier()).thenReturn(accountIdentifier);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(account);
        accountsList.add(account);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((PublicClientApplication.AccountsLoadedListener) invocationOnMock.getArguments()[0]).onAccountsLoaded(accountsList);
                return null;
            }
        }).when(publicClientApplication).getAccounts(any(PublicClientApplication.AccountsLoadedListener.class));
        Identity.signOut();
        verify(publicClientApplication).removeAccount(eq(account));
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
        Identity identity = Identity.getInstance();
        when(mAuthTokenContext.getHomeAccountId()).thenReturn(UUIDUtils.randomUUID().toString());
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUIDUtils.randomUUID().toString());
        start(identity);
        Identity.signOut();
        verify(publicClientApplication, never()).removeAccount(eq(account));
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
        Identity identity = Identity.getInstance();
        start(identity);
        when(mAuthTokenContext.getHomeAccountId()).thenReturn("5");
        when(mAuthTokenContext.getAuthToken()).thenReturn(UUIDUtils.randomUUID().toString());
        final List<IAccount> accountsList = new ArrayList<>();
        IAccount account = mock(IAccount.class);
        IAccountIdentifier accountIdentifier = mock(IAccountIdentifier.class);
        when(accountIdentifier.getIdentifier()).thenReturn("10");
        when(account.getHomeAccountIdentifier()).thenReturn(accountIdentifier);
        accountsList.add(account);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocationOnMock) {
                ((PublicClientApplication.AccountsLoadedListener) invocationOnMock.getArguments()[0]).onAccountsLoaded(accountsList);
                return null;
            }
        }).when(publicClientApplication).getAccounts(any(PublicClientApplication.AccountsLoadedListener.class));
        Identity.signOut();
        verify(publicClientApplication, never()).removeAccount(eq(account));
    }

    private void mockReadyToSignIn() throws Exception {

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        Identity identity = Identity.getInstance();
        start(identity);

        /* Download configuration. */
        mockSuccessfulHttpCall(mockValidForAppCenterConfig(), httpClient);

        /* Mock foreground. */
        identity.onActivityResumed(mock(Activity.class));
    }

    @Test
    public void setConfigUrl() throws Exception {

        /* Set url before start. */
        String configUrl = "https://config.contoso.com";
        Identity.setConfigUrl(configUrl);

        /* Mock http and start identity service. */
        HttpClientRetryer httpClient = mock(HttpClientRetryer.class);
        whenNew(HttpClientRetryer.class).withAnyArguments().thenReturn(httpClient);
        start(Identity.getInstance());

        /* Check call. */
        String expectedUrl = configUrl + "/identity/" + APP_SECRET + ".json";
        verify(httpClient).callAsync(eq(expectedUrl), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @NonNull
    private Channel start(Identity identity) {
        Channel channel = mock(Channel.class);
        identity.onStarting(mAppCenterHandler);
        identity.onStarted(mock(Context.class), channel, APP_SECRET, null, true);
        return channel;
    }

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

    private static void mockSuccessfulHttpCall(JSONObject jsonConfig, HttpClientRetryer httpClient) throws JSONException {

        /* Intercept parameters. */
        ArgumentCaptor<HttpClient.CallTemplate> templateArgumentCaptor = ArgumentCaptor.forClass(HttpClient.CallTemplate.class);
        ArgumentCaptor<ServiceCallback> callbackArgumentCaptor = ArgumentCaptor.forClass(ServiceCallback.class);
        String expectedUrl = Constants.DEFAULT_CONFIG_URL + "/identity/" + APP_SECRET + ".json";
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

        /* Simulate response. */
        HashMap<String, String> headers = new HashMap<>();
        headers.put("ETag", "mockETag");
        serviceCallback.onCallSucceeded(jsonConfig.toString(), headers);
    }
}
