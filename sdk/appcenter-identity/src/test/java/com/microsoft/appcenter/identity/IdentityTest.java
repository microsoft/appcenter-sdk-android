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
import com.microsoft.appcenter.utils.NetworkStateHelper;
import com.microsoft.appcenter.utils.UUIDUtils;
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
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doThrow;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNew;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({AuthTokenContext.class, NetworkStateHelper.class})
public class IdentityTest extends AbstractIdentityTest {

    @Captor
    private ArgumentCaptor<Map<String, String>> mHeadersCaptor;

    @NonNull
    private Channel start(Identity identity) {
        Channel channel = mock(Channel.class);
        identity.onStarting(mAppCenterHandler);
        identity.onStarted(mock(Context.class), channel, "", null, true);
        return channel;
    }

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
        verify(mPreferenceTokenStorage).cacheToken();
        verify(channel).removeGroup(eq(identity.getGroupName()));
        verify(channel).addGroup(eq(identity.getGroupName()), anyInt(), anyLong(), anyInt(), isNull(Ingestion.class), any(Channel.GroupListener.class));

        /* Now we can see the service enabled. */
        assertTrue(Identity.isEnabled().get());

        /* Disable. Testing to wait setEnabled to finish while we are at it. */
        Identity.setEnabled(false).get();
        assertFalse(Identity.isEnabled().get());
        verify(mPreferenceTokenStorage).removeToken();
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

        /* Mock authentication result. */
        String mockIdToken = UUIDUtils.randomUUID().toString();
        String mockAccountId = UUIDUtils.randomUUID().toString();
        String mockHomeAccountId = UUIDUtils.randomUUID().toString();
        IAuthenticationResult mockResult = mockAuthResult(mockIdToken, mockAccountId, mockHomeAccountId);

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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(publicClientApplication.getAccount(eq(mockHomeAccountId), anyString())).thenReturn(mockAccount);doAnswer(new Answer<Void>() {

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
        verify(mPreferenceTokenStorage).saveToken(eq(idToken), eq(homeAccountId));

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
        verify(mPreferenceTokenStorage).saveToken(eq(idToken), eq(homeAccountId));
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
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
        verify(mPreferenceTokenStorage).saveToken(eq(mockIdToken), eq(mockHomeAccountId));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

        /* Verify interactions - should succeed silent sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class),
                any(String.class), eq(true), notNull(AuthenticationCallback.class));
        verify(mPreferenceTokenStorage, times(2)).saveToken(eq(mockIdToken), eq(mockHomeAccountId));
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(mockHomeAccountId);
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
        verify(mPreferenceTokenStorage).saveToken(eq(mockIdToken), eq(mockHomeAccountId));
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
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
        verify(mPreferenceTokenStorage).saveToken(eq(mockIdToken), eq(mockHomeAccountId));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

        /* Verify interactions - should succeed silent sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class), any(String.class),
                eq(true), notNull(AuthenticationCallback.class));
        verify(mPreferenceTokenStorage).saveToken(eq(mockIdToken), eq(mockHomeAccountId));
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
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
        verify(mPreferenceTokenStorage).saveToken(eq(mockIdToken), eq(mockHomeAccountId));

        /* Call signIn again to trigger silent sign-in. */
        Identity.signIn();

        /* Verify interactions - should fail silent and fallback to interactive sign-in. */
        verify(publicClientApplication).acquireTokenSilentAsync(notNull(String[].class), any(IAccount.class),
                any(String.class), eq(true), notNull(AuthenticationCallback.class));
        verify(publicClientApplication, times(2)).acquireToken(same(activity), notNull(String[].class), notNull(AuthenticationCallback.class));
        verify(mPreferenceTokenStorage, times(2)).saveToken(eq(mockIdToken), eq(mockHomeAccountId));
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(null).thenReturn(mockHomeAccountId);
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
        assertTrue(future.get().getException() instanceof IllegalThreadStateException);
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
        start(identity);
        when(mPreferenceTokenStorage.getToken()).thenReturn("42");

        /* Sign out should clear token. */
        Identity.signOut();
        verify(mPreferenceTokenStorage).removeToken();
    }

    @Test
    public void verifyTokenStorageIsEmpty() {
        Identity identity = Identity.getInstance();
        start(identity);
        Identity.signOut();
        when(AppCenter.getLogLevel()).thenReturn(Log.WARN);
        verify(mPreferenceTokenStorage, never()).removeToken();
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(UUIDUtils.randomUUID().toString());
        when(mPreferenceTokenStorage.getToken()).thenReturn(UUIDUtils.randomUUID().toString());
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(null);
        when(mPreferenceTokenStorage.getToken()).thenReturn(UUIDUtils.randomUUID().toString());
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
        start(identity);
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn(mockHomeAccountId);
        when(mPreferenceTokenStorage.getToken()).thenReturn(UUIDUtils.randomUUID().toString());
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
        when(mPreferenceTokenStorage.getHomeAccountId()).thenReturn("5");
        when(mPreferenceTokenStorage.getToken()).thenReturn(UUIDUtils.randomUUID().toString());
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

    private static void mockSuccessfulHttpCall(JSONObject jsonConfig, HttpClientRetryer httpClient) throws JSONException {

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
