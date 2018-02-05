package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;

import org.json.JSONException;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_SCHEME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.UPDATE_SETUP_PATH_FORMAT;
import static com.microsoft.appcenter.utils.storage.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Cover scenarios that are happening before we see an API call success for latest release.
 */
@PrepareForTest({ErrorDetails.class, DistributeUtils.class})
public class DistributeBeforeApiSuccessTest extends AbstractDistributeTest {

    /**
     * Shared code to mock a restart of an activity considered to be the launcher.
     */
    private void restartResumeLauncher(Activity activity) {
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(activity.getClass().getName());
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityStopped(activity);
        Distribute.getInstance().onActivityDestroyed(activity);
        Distribute.getInstance().onActivityCreated(activity, mock(Bundle.class));
        Distribute.getInstance().onActivityResumed(activity);
    }

    private void testDistributeInactive() throws Exception {

        /* Check browser not opened. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /*
         * Even if we had a token on a previous install that was not from store
         * (if package name and signature matches an APK on Google Play you can upgrade to
         * Google Play version without losing data.
         */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient, never()).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    private void showUpdateSetupFailedDialog() {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message");
        when(mDialog.isShowing()).thenReturn(false);
        when(mDialogBuilder.create()).thenReturn(mDialog);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setCancelable(false);
        verify(mDialogBuilder).setTitle(R.string.appcenter_distribute_update_failed_dialog_title);
        verify(mDialogBuilder).setMessage(R.string.appcenter_distribute_update_failed_dialog_message);
        verify(mDialog).show();
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
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
    public void doNothingIfUpdateSetupFailedMessageExist() throws Exception {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");
        testDistributeInactive();
    }

    @Test
    public void doNothingIfReleaseHashEqualsToFailedPackageHash() throws Exception {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY)).thenReturn("some_hash");
        mockStatic(DistributeUtils.class);

        /* Mock the computeReleaseHash to some_hash value. */
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("some_hash");
        testDistributeInactive();
    }

    @Test
    public void continueIfReleaseHashNotEqualsToFailedPackageHash() throws Exception {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY)).thenReturn("some_hash");
        mockStatic(DistributeUtils.class);

        /* Mock the computeReleaseHash to other_hash value. */
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("other_hash");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    @SuppressWarnings("WrongConstant")
    public void doNothingIfGetPackageInfoFails() throws Exception {
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenThrow(new PackageManager.NameNotFoundException());
        testDistributeInactive();
    }

    @Test
    public void storeUpdateSetupFailedParameterBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        start();
        Distribute.getInstance().storeUpdateSetupFailedParameter("r", "error_message");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storeUpdateSetupFailedParameterWithIncorrectRequestIdBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        start();
        Distribute.getInstance().storeUpdateSetupFailedParameter("r2", "error_message");
        verifyStatic(never());
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storePrivateRedirectionBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);

        /* Store token before start, start in background, no storage access. */
        Distribute.getInstance().storeRedirectionParameters("r", "g", "some token");
        start();
        verifyStatic(never());
        PreferencesStorage.putString(anyString(), anyString());
        verifyStatic(never());
        PreferencesStorage.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void storePublicRedirectionBeforeStart() throws Exception {

        /* Setup mock. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);

        /* Store token before start, start in background, no storage access. */
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);
        start();
        verifyStatic(never());
        PreferencesStorage.putString(anyString(), anyString());
        verifyStatic(never());
        PreferencesStorage.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void postponeBrowserIfNoNetwork() throws Exception {

        /* Check browser not opened if no network. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* If network comes back, we don't open network unless we restart app. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /* Restart should open browser if still have network. */
        when(UUIDUtils.randomUUID()).thenReturn(UUID.randomUUID());
        restartResumeLauncher(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void resumeWhileStartingAndDisableWhileRunningBrowserCodeOnUI() throws InterruptedException {
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                runnable.set((Runnable) invocation.getArguments()[0]);
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        Distribute.getInstance().onStarting(mAppCenterHandler);
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.getInstance().onStarted(mContext, "a", mock(Channel.class));

        /* Disable and test async behavior of setEnabled. */
        final CountDownLatch latch = new CountDownLatch(1);
        Distribute.setEnabled(false).thenAccept(new AppCenterConsumer<Void>() {

            @Override
            public void accept(Void aVoid) {
                latch.countDown();
            }
        });
        runnable.get().run();
        assertTrue(latch.await(0, TimeUnit.MILLISECONDS));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void disableBeforeHandleUpdateSetupFailureDialogIgnoreAction() throws Exception {
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        showUpdateSetupFailedDialog();
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_failed_dialog_ignore), clickListener.capture());

        /* Disable. */
        Distribute.setEnabled(false);
        clickListener.getValue().onClick(mock(Dialog.class), DialogInterface.BUTTON_POSITIVE);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();
    }

    @Test
    public void disableBeforeHandleUpdateSetupFailureDialogReinstallAction() throws Exception {
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        showUpdateSetupFailedDialog();
        verify(mDialogBuilder).setNegativeButton(eq(R.string.appcenter_distribute_update_failed_dialog_reinstall), clickListener.capture());

        /* Disable. */
        Distribute.setEnabled(false);
        clickListener.getValue().onClick(mock(Dialog.class), DialogInterface.BUTTON_NEGATIVE);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();
    }

    @Test
    public void handleFailedUpdateSetupDialogReinstallAction() throws URISyntaxException {
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        showUpdateSetupFailedDialog();
        verify(mDialogBuilder).setNegativeButton(eq(R.string.appcenter_distribute_update_failed_dialog_reinstall), clickListener.capture());

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        verifyStatic();
        BrowserUtils.appendUri(anyString(), anyString());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
    }

    @Test
    public void handleFailedUpdateSetupDialogReinstallActionWithException() throws URISyntaxException {
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        showUpdateSetupFailedDialog();
        when(BrowserUtils.appendUri(anyString(), anyString())).thenThrow(new URISyntaxException("Ex", "Reason"));
        verify(mDialogBuilder).setNegativeButton(eq(R.string.appcenter_distribute_update_failed_dialog_reinstall), clickListener.capture());

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        verifyStatic();
        BrowserUtils.appendUri(anyString(), anyString());
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
    }

    @Test
    public void handleFailedUpdateSetupDialogIgnoreAction() throws URISyntaxException {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");
        when(mDialog.isShowing()).thenReturn(false);
        when(mDialogBuilder.create()).thenReturn(mDialog);
        mockStatic(DistributeUtils.class);
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("some_hash");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setCancelable(false);
        verify(mDialogBuilder).setTitle(R.string.appcenter_distribute_update_failed_dialog_title);
        verify(mDialogBuilder).setMessage(R.string.appcenter_distribute_update_failed_dialog_message);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_failed_dialog_ignore), clickListener.capture());
        verify(mDialog).show();

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY, "some_hash");
    }

    @Test
    public void verifyFailedUpdateSetupDialogIsAlreadyShownInSameActivity() throws Exception {
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");

        /* Trigger call. */
        start();
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        Distribute.getInstance().onActivityPaused(activity);

        /* Go foreground. */
        Distribute.getInstance().onActivityResumed(activity);

        /* Verify dialog now shown. */
        verify(mDialogBuilder).create();
        verify(mDialog).show();
    }

    @Test
    public void happyPathUntilHangingCallWithToken() throws Exception {

        /* Setup mock. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());

        /* Start and resume: open browser. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* If browser already opened, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If call already made, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Call is still in progress. If we restart app, nothing happens we still wait. */
        restartResumeLauncher(mActivity);

        /* Verify behavior not changed. */
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If process is restarted, a new call will be made. Need to mock storage for that. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("g");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(httpClient, times(2)).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void happyPathUntilHangingCallWithoutToken() throws Exception {

        /* Setup mock. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        UUID requestId = UUID.randomUUID();
        when(UUIDUtils.randomUUID()).thenReturn(requestId);
        when(PreferencesStorage.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());

        /* Start and resume: open browser. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* If browser already opened, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", null);

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If call already made, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify behavior. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Call is still in progress. If we restart app, nothing happens we still wait. */
        restartResumeLauncher(mActivity);

        /* Verify behavior not changed. */
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_ID);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(httpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If process is restarted, a new call will be made. Need to mock storage for that. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("g");
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(httpClient, times(2)).callAsync(argThat(new ArgumentMatcher<String>() {

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
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = "http://mock";
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");
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
        when(mPackageManager.getPackageInfo("com.contoso", 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify only tried once. */
        verify(mPackageManager).getPackageInfo("com.contoso", 0);

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
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Disable. */
        Distribute.setEnabled(false);
        assertFalse(Distribute.isEnabled().get());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

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
        assertTrue(Distribute.isEnabled().get());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

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
        start();
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

    private void checkReleaseFailure(final Exception exception, VerificationMode deleteTokenVerificationMode) throws Exception {

        /* Mock we already have token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(exception);
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Check token kept or not depending on the test. */
        verifyStatic(deleteTokenVerificationMode);
        PreferencesStorage.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkReleaseFailsRecoverable503() throws Exception {
        checkReleaseFailure(new HttpException(503), never());
    }

    @Test
    public void checkReleaseFailsWith403() throws Exception {
        checkReleaseFailure(new HttpException(403), times(1));

    }

    @Test
    public void checkReleaseFailsWithSomeSSL() throws Exception {
        checkReleaseFailure(new SSLPeerUnverifiedException("unsecured connection"), times(1));
    }

    @Test
    public void checkReleaseFailsWithSome404urlNotFound() throws Exception {

        /* Mock error parsing. */
        mockStatic(ErrorDetails.class);
        final String errorPayload = "<html>Not Found</html>";
        when(ErrorDetails.parse(errorPayload)).thenThrow(new JSONException("Expected {"));
        final Exception exception = new HttpException(404, errorPayload);
        checkReleaseFailure(exception, times(1));
    }

    @Test
    public void checkReleaseFailsWithSome404noRelease() throws Exception {

        /* Mock error parsing. */
        ErrorDetails errorDetails = mock(ErrorDetails.class);
        when(errorDetails.getCode()).thenReturn(ErrorDetails.NO_RELEASES_FOR_USER_CODE);
        mockStatic(ErrorDetails.class);
        String errorPayload = "{code: 'no_releases_for_user'}";
        when(ErrorDetails.parse(errorPayload)).thenReturn(errorDetails);
        checkReleaseFailure(new HttpException(404, errorPayload), never());
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
        start();
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
        start();
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
        start();
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
        when(mCryptoUtils.decrypt(eq("some encrypted token"), anyBoolean())).thenReturn(new CryptoUtils.DecryptedData("some token", "some better encrypted token"));
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify storage was updated with new encrypted value. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some better encrypted token");
    }

    @Test
    public void useMobileCenterFailOverForDecryptAndReleaseDetailsPrivate() throws Exception {
        when(mMobileCenterPreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN, null)).thenReturn("some token MC");
        when(mMobileCenterPreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, null)).thenReturn("some group MC");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token MC");

        /* Primary storage will be missing data. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify the new strings were put into PreferencesStorage */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token MC");
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "some group MC");
        verify(mDistributeInfoTracker).updateDistributionGroupId("some group MC");
    }

    @Test
    public void useMobileCenterFailOverForDecryptAndReleaseDetailsPublic() throws Exception {
        when(mMobileCenterPreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN, null)).thenReturn(null);
        when(mMobileCenterPreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, null)).thenReturn("some group MC");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        HashMap<String, String> headers = new HashMap<>();

        /* Primary storage will be missing data. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify the group was saved into new storage. */
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_UPDATE_TOKEN), anyString());
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "some group MC");
        verify(mDistributeInfoTracker).updateDistributionGroupId("some group MC");
    }
}
