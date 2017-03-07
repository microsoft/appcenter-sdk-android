package com.microsoft.azure.mobile.distribute;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.HttpException;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.UUIDUtils;
import com.microsoft.azure.mobile.utils.crypto.CryptoUtils;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.microsoft.azure.mobile.distribute.DistributeConstants.PARAMETER_PLATFORM;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.UPDATE_SETUP_PATH_FORMAT;
import static com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Cover scenarios that are happening before we see an API call success for latest release.
 */
public class DistributeBeforeApiSuccessTest extends AbstractDistributeTest {

    /**
     * Shared code to mock a restart of an activity considered to be the launcher.
     */
    private static void restartResumeLauncher(Activity activity) {
        Intent intent = mock(Intent.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(activity.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(packageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(activity.getClass().getName());
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityStopped(activity);
        Distribute.getInstance().onActivityDestroyed(activity);
        Distribute.getInstance().onActivityCreated(activity, mock(Bundle.class));
        Distribute.getInstance().onActivityResumed(activity);
    }

    private void testDistributeInactive() throws Exception {

        /* Check browser not opened. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /*
         * Even if we had a token on a previous install that was not from store
         * (if package name and signature matches an APK on Google Play you can upgrade to
         * Google Play version without losing data.
         */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        Distribute.unsetInstance();
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient, never()).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void doNothingIfDebug() throws Exception {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        testDistributeInactive();
    }

    @Test
    public void doNothingIfInstallComesFromStore() throws Exception {
        when(InstallerUtils.isInstalledFromAppStore(anyString(), any(Context.class))).thenReturn(true);
        testDistributeInactive();
    }

    @Test
    public void storeTokenBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);

        /* Store token before start, start in background, no storage access. */
        Distribute.getInstance().storeUpdateToken("some token", "r");
        Distribute.getInstance().onStarted(mContext, "", mock(Channel.class));
        verifyStatic(never());
        PreferencesStorage.putString(anyString(), anyString());
        verifyStatic(never());
        PreferencesStorage.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void postponeBrowserIfNoNetwork() throws Exception {

        /* Check browser not opened if no network. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* If network comes back, we don't open network unless we restart app. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* Restart should open browser if still have network. */
        when(UUIDUtils.randomUUID()).thenReturn(UUID.randomUUID());
        Activity activity = mock(Activity.class);
        restartResumeLauncher(activity);
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void happyPathUntilHangingCall() throws Exception {

        /* Setup mock. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());

        /* Start and resume: open browser. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* If browser already opened, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityResumed(activity);
        verifyStatic();
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeUpdateToken("some token", requestId.toString());

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If call already made, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityResumed(activity);

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Call is still in progress. If we restart app, nothing happens we still wait. */
        restartResumeLauncher(activity);
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityStopped(activity);
        Distribute.getInstance().onActivityDestroyed(activity);
        Distribute.getInstance().onActivityCreated(activity, mock(Bundle.class));
        Distribute.getInstance().onActivityResumed(activity);

        /* Verify behavior not changed. */
        verifyStatic();
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void setUrls() throws Exception {

        /* Setup mock. */
        Distribute.setInstallUrl("http://mock");
        Distribute.setApiUrl("https://mock2");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());

        /* Start and resume: open browser. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        verifyStatic();
        String url = "http://mock";
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeUpdateToken("some token", requestId.toString());
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith("https://mock2");
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void computeHashFailsWhenOpeningBrowser() throws Exception {

        /* Mock package manager. */
        Context context = mock(Context.class);
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageName()).thenReturn("com.contoso");
        when(context.getPackageManager()).thenReturn(packageManager);
        when(context.getApplicationInfo()).thenReturn(mApplicationInfo);
        when(packageManager.getPackageInfo("com.contoso", 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        Distribute.getInstance().onStarted(context, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify only tried once. */
        verify(packageManager).getPackageInfo("com.contoso", 0);

        /* And verify we didn't open browser. */
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
        verifyStatic(never());
        PreferencesStorage.putString(anyString(), anyString());
    }

    @Test
    public void disableBeforeStoreToken() {

        /* Start and resume: open browser. */
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        BrowserUtils.openBrowser(url, activity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Disable. */
        Distribute.setEnabled(false);
        assertFalse(Distribute.isEnabled());

        /* Store token. */
        Distribute.getInstance().storeUpdateToken("some token", requestId.toString());

        /* Verify behavior. */
        verifyStatic(never());
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Since after disabling once, the request id was deleted we can enable/disable it will also ignore the request. */
        Distribute.setEnabled(true);
        assertTrue(Distribute.isEnabled());

        /* Store token. */
        Distribute.getInstance().storeUpdateToken("some token", requestId.toString());

        /* Verify behavior. */
        verifyStatic(never());
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
    }

    @Test
    public void disableWhileCheckingRelease() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        ServiceCall firstCall = mock(ServiceCall.class);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenReturn(firstCall).thenReturn(mock(ServiceCall.class));
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* The call is only triggered when app is resumed. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        verify(httpClient, never()).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify cancel on disabling. */
        verify(firstCall, never()).cancel();
        Distribute.setEnabled(false);
        verify(firstCall).cancel();

        /* No more call on that one. */
        Distribute.setEnabled(true);
        Distribute.setEnabled(false);
        verify(firstCall).cancel();
    }

    @Test
    public void checkReleaseFailsRecoverable() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(503));
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* The error was recoverable, keep token. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkReleaseFailsNotRecoverable() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(403));
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* The error was unrecoverable, get rid of token. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkReleaseFailsParsing() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        when(ReleaseDetails.parse(anyString())).thenThrow(new JSONException("mock"));

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void disableBeforeCheckReleaseFails() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final Semaphore beforeSemaphore = new Semaphore(0);
        final Semaphore afterSemaphore = new Semaphore(0);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(403));
                        afterSemaphore.release();
                    }
                }.start();
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Disable before it fails. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        beforeSemaphore.release();
        afterSemaphore.acquireUninterruptibly();

        /* Verify complete workflow call ignored. i.e. no more call to delete the state. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void disableBeforeCheckReleaseSucceed() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final Semaphore beforeSemaphore = new Semaphore(0);
        final Semaphore afterSemaphore = new Semaphore(0);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocation) throws Throwable {
                new Thread() {

                    @Override
                    public void run() {
                        beforeSemaphore.acquireUninterruptibly();
                        ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                        afterSemaphore.release();
                    }
                }.start();
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Disable before it succeeds. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        beforeSemaphore.release();
        afterSemaphore.acquireUninterruptibly();

        /* Verify complete workflow call skipped. i.e. no more call to delete the state. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verify(mDialog, never()).show();
    }

    @Test
    public void storeBetterEncryptedToken() throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some encrypted token");
        when(mCryptoUtils.decrypt("some encrypted token")).thenReturn(new CryptoUtils.DecryptedData("some token", "some better encrypted token"));
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify storage was updated with new encrypted value. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some better encrypted token");
    }
}
