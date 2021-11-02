/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.HandlerUtils;
import com.microsoft.appcenter.utils.async.AppCenterConsumer;
import com.microsoft.appcenter.utils.context.SessionContext;
import com.microsoft.appcenter.utils.crypto.CryptoUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.verification.VerificationMode;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLPeerUnverifiedException;

import static com.microsoft.appcenter.Flags.DEFAULTS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_INSTALL_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_PLATFORM_VALUE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REDIRECT_SCHEME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PARAMETER_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_POSTPONE_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_REQUEST_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.appcenter.distribute.DistributeConstants.PRIVATE_UPDATE_SETUP_PATH_FORMAT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
 * Cover scenarios that are happening before we see an API call success for latest release.
 */
@PrepareForTest({ErrorDetails.class, DistributeUtils.class, SessionContext.class, UUID.class})
@RunWith(PowerMockRunner.class)
public class DistributeBeforeApiSuccessTest extends AbstractDistributeTest {

    private void testDistributeInactiveOnPrivateTrack() throws PackageManager.NameNotFoundException {

        /* Check browser not opened. */
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        BrowserUtils.openBrowser(anyString(), any(Activity.class));

        /*
         * Even if we had a token on a previous install that was not from store
         * (if package name and signature matches an APK on Google Play you can upgrade to
         * Google Play version without losing data.
         */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        Distribute.unsetInstance();
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    private void showUpdateSetupFailedDialog() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message");
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
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void doNothingIfEnabledForDebuggableBuildNotSet() throws PackageManager.NameNotFoundException {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        testDistributeInactiveOnPrivateTrack();
    }

    @Test
    public void doNothingWhenEnabledForDebuggableBuildSetToFalse() throws PackageManager.NameNotFoundException {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        Distribute.setEnabledForDebuggableBuild(false);
        testDistributeInactiveOnPrivateTrack();
    }

    @Test
    public void continueWhenEnabledForDebuggableBuildSetToTrue() throws Exception {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        Distribute.setEnabledForDebuggableBuild(true);

        /* Setup mock. */
        Distribute.setInstallUrl("http://mock");
        Distribute.setApiUrl("https://mock2");
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = "http://mock";
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith("https://mock2");
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void doNothingIfInstallComesFromStore() throws PackageManager.NameNotFoundException {
        when(InstallerUtils.isInstalledFromAppStore(anyString(), any(Context.class))).thenReturn(true);
        testDistributeInactiveOnPrivateTrack();
    }

    @Test
    public void doNothingIfUpdateSetupFailedMessageExist() throws PackageManager.NameNotFoundException {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");
        testDistributeInactiveOnPrivateTrack();
    }

    @Test
    public void doNothingIfReleaseHashEqualsToFailedPackageHash() throws PackageManager.NameNotFoundException {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY)).thenReturn("some_hash");
        mockStatic(DistributeUtils.class);

        /* Mock the computeReleaseHash to some_hash value. */
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("some_hash");
        testDistributeInactiveOnPrivateTrack();
    }

    @Test
    public void checkForUpdateIfIgnoredSideLoadingButSwitchedToPublicTrack() throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY)).thenReturn("some_hash");
        mockStatic(DistributeUtils.class);

        /* Mock the computeReleaseHash to some_hash value. */
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("some_hash");

        /* When we start SDK (public track by default). */
        start();
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);

        /* Then we check for update directly via API call. */
        verify(mHttpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic(never());
        DistributeUtils.updateSetupUsingBrowser(same(activity), anyString(), anyString(), any(PackageInfo.class));

        /* Check we don't reset private track ignore state. */
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void continueIfReleaseHashNotEqualsToFailedPackageHash() throws PackageManager.NameNotFoundException {
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY)).thenReturn("some_hash");
        mockStatic(DistributeUtils.class);

        /* Mock the computeReleaseHash to other_hash value. */
        PowerMockito.when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("other_hash");

        /* Trigger call. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        verifyStatic();
        DistributeUtils.updateSetupUsingBrowser(same(activity), anyString(), anyString(), any(PackageInfo.class));
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void storeUpdateSetupFailedParameterBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        Distribute.getInstance().storeUpdateSetupFailedParameter("r", "error_message");
        verifyStatic(never());
        SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID);
        start();
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storeTesterAppUpdateSetupFailedParameterBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        Distribute.getInstance().storeTesterAppUpdateSetupFailedParameter("r", "error_message");
        verifyStatic(never());
        SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID);
        start();
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storeUpdateSetupFailedParameterWithIncorrectRequestIdBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        Distribute.getInstance().storeUpdateSetupFailedParameter("r2", "error_message");
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storeTesterAppUpdateSetupFailedParameterWithIncorrectRequestIdBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");
        Distribute.getInstance().storeTesterAppUpdateSetupFailedParameter("r2", "error_message");
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY, "error_message");
    }

    @Test
    public void storePrivateRedirectionBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Store token before start, start in background, no storage access. */
        Distribute.getInstance().storeRedirectionParameters("r", "g", "some token");
        start();
        verifyStatic(never());
        SharedPreferencesManager.putString(anyString(), anyString());
        verifyStatic(never());
        SharedPreferencesManager.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void storePublicRedirectionBeforeStart() {

        /* Setup mock. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Store token before start, start in background, no storage access. */
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);
        start();
        verifyStatic(never());
        SharedPreferencesManager.putString(anyString(), anyString());
        verifyStatic(never());
        SharedPreferencesManager.remove(anyString());

        /* Unlock the processing by going into foreground. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void postponeBrowserIfNoNetwork() throws Exception {
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Check browser not opened if no network. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
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
        restartResumeLauncher(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void resumeWhileStartingAndDisableWhileRunningBrowserCodeOnUI() throws InterruptedException {
        final AtomicReference<Runnable> runnable = new AtomicReference<>();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                runnable.set((Runnable) invocation.getArguments()[0]);
                return null;
            }
        }).when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
        Distribute.getInstance().onStarting(mAppCenterHandler);
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.getInstance().onStarted(mContext, mock(Channel.class), "a", null, true);

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
    public void disableBeforeHandleUpdateSetupFailureDialogIgnoreAction() {
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
    public void disableBeforeHandleUpdateSetupFailureDialogReinstallAction() {
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
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
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
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY);
    }

    @Test
    public void handleFailedUpdateSetupDialogIgnoreAction() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");
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
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_PACKAGE_HASH_KEY, "some_hash");
    }

    @Test
    public void verifyFailedUpdateSetupDialogIsAlreadyShownInSameActivity() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message_from_backend");

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
    public void testerAppNotInstalled() throws Exception {

        /* Setup mock. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());
    }

    @Test
    public void useBrowserUpdateSetupIfAppIsTesterApp() throws Exception {

        /* Setup mock. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenReturn(mock(PackageInfo.class));
        when(mContext.getPackageName()).thenReturn(DistributeUtils.TESTER_APP_PACKAGE_NAME);

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(anyString(), any(Activity.class));
    }

    @Test
    public void testerAppUpdateSetupFailed() throws Exception {

        /* Setup mock. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        String url = "ms-actesterapp://update-setup";
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        whenNew(Intent.class).withArguments(Intent.ACTION_VIEW, Uri.parse(url)).thenReturn(mock(Intent.class));
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenReturn(mock(PackageInfo.class));

        /* Start and resume: open tester app. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        verify(mActivity).startActivity(intent);

        /* Start and resume: open browser. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("true");
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);

        /* Start and resume: open browser. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn(null);
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
    }

    @Test
    public void happyPathUsingTesterAppUpdateSetup() throws Exception {

        /* Setup mock. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId).thenCallRealMethod();
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_TESTER_APP_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        String url = "ms-actesterapp://update-setup";
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId;
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        whenNew(Intent.class).withArguments(Intent.ACTION_VIEW, Uri.parse(url)).thenReturn(mock(Intent.class));
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenReturn(mock(PackageInfo.class));

        /*
         * Start and resume: open tester app.
         *
         * Call resume before runOnUiThread callback to emulate regular activity behaviour.
         * In this case, it should not overwrite request id to a new value.
         */
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        doNothing().when(HandlerUtils.class);
        HandlerUtils.runOnUiThread(captor.capture());
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        if (!captor.getAllValues().isEmpty()) {
            captor.getValue().run();
        }
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        verify(mActivity).startActivity(intent);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Verify that it was only one call. */
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_REQUEST_ID), anyString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

        /* Verify behavior. */
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

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
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Call is still in progress. If we restart app, nothing happens we still wait. */
        restartResumeLauncher(mActivity);

        /* Verify behavior not changed. */
        verify(mActivity).startActivity(intent);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If process is restarted, a new call will be made. Need to mock storage for that. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("g");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        Distribute.unsetInstance();
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mHttpClient, times(2)).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void happyPathUntilHangingCallWithToken() throws Exception {

        /* Setup mock. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* If browser already opened, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

        /* Verify behavior. */
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

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
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

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
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, "g");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDistributeInfoTracker).updateDistributionGroupId("g");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If process is restarted, a new call will be made. Need to mock storage for that. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("g");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        Distribute.unsetInstance();
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mHttpClient, times(2)).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void happyPathUntilHangingCallWithoutToken() {

        /* Start and resume: call public API directly. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify behavior. */
        HashMap<String, String> headers = new HashMap<>();
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If call already made, activity changed must not recall it. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify behavior. */
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Call is still in progress. If we restart app, nothing happens we still wait. */
        restartResumeLauncher(mActivity);

        /* Verify behavior not changed. */
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().startsWith(DistributeConstants.DEFAULT_API_URL);
            }
        }), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* If process is restarted, a new call will be made. Need to mock storage for that. */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mHttpClient, times(2)).callAsync(argThat(new ArgumentMatcher<String>() {

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
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn(requestId.toString());
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = "http://mock";
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        verify(mHttpClient).callAsync(argThat(new ArgumentMatcher<String>() {

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
        SharedPreferencesManager.putString(anyString(), anyString());
    }

    @Test
    public void disableBeforeStoreToken() throws Exception {
        when(mPackageManager.getPackageInfo(DistributeUtils.TESTER_APP_PACKAGE_NAME, 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Start and resume: open browser. */
        UUID requestId = UUID.randomUUID();
        mockStatic(UUID.class);
        when(UUID.randomUUID()).thenReturn(requestId);

        /* Start and resume: open browser. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        verifyStatic();
        String url = DistributeConstants.DEFAULT_INSTALL_URL;
        url += String.format(PRIVATE_UPDATE_SETUP_PATH_FORMAT, "a");
        url += "?" + PARAMETER_RELEASE_HASH + "=" + TEST_HASH;
        url += "&" + PARAMETER_REDIRECT_ID + "=" + mContext.getPackageName();
        url += "&" + PARAMETER_REDIRECT_SCHEME + "=" + "appcenter";
        url += "&" + PARAMETER_REQUEST_ID + "=" + requestId.toString();
        url += "&" + PARAMETER_PLATFORM + "=" + PARAMETER_PLATFORM_VALUE;
        url += "&" + PARAMETER_ENABLE_UPDATE_SETUP_FAILURE_REDIRECT_KEY + "=" + "true";
        url += "&" + PARAMETER_INSTALL_ID + "=" + mInstallId.toString();
        BrowserUtils.openBrowser(url, mActivity);
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_REQUEST_ID, requestId.toString());

        /* Disable. */
        Distribute.setEnabled(false);
        assertFalse(Distribute.isEnabled().get());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

        /* Verify behavior. */
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_REQUEST_ID);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Since after disabling once, the request id was deleted we can enable/disable it will also ignore the request. */
        Distribute.setEnabled(true);
        assertTrue(Distribute.isEnabled().get());

        /* Store token. */
        Distribute.getInstance().storeRedirectionParameters(requestId.toString(), "g", "some token");

        /* Verify behavior. */
        verifyStatic(never());
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some token");
    }

    @Test
    public void disableWhileCheckingRelease() {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        ServiceCall firstCall = mock(ServiceCall.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenReturn(firstCall).thenReturn(mock(ServiceCall.class));

        /* The call is only triggered when app is resumed. */
        start();
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

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
    public void releaseFailureWithDifferentIds() {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {

                /* Do the call so that ids do not match. */
                Distribute.getInstance().getLatestReleaseDetails("mockGroup", "token");
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(new HttpException(new HttpResponse(503)));
                return mock(ServiceCall.class);
            }
        }).thenAnswer(new Answer<ServiceCall>() {

            /* On second time we don't answer as it's callback from getLatestReleaseDetails above. */
            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                return mock(ServiceCall.class);
            }
        });

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify on failure we don't complete workflow if ids don't match. */
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    private void checkReleaseFailure(final Exception exception, VerificationMode deleteTokenVerificationMode) {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(exception);
                return mock(ServiceCall.class);
            }
        });

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Check token kept or not depending on the test. */
        verifyStatic(deleteTokenVerificationMode);
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* Check postpone time kept or not depending on the test. */
        verifyStatic(deleteTokenVerificationMode);
        SharedPreferencesManager.remove(PREFERENCE_KEY_POSTPONE_TIME);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkReleaseFailsRecoverable503() {
        checkReleaseFailure(new HttpException(new HttpResponse(503)), never());
    }

    @Test
    public void checkReleaseFailsWith403() {
        checkReleaseFailure(new HttpException(new HttpResponse(403)), times(1));
    }

    @Test
    public void checkReleaseFailsWithSomeSSL() {
        checkReleaseFailure(new SSLPeerUnverifiedException("unsecured connection"), never());
    }

    @Test
    public void checkReleaseFailsWithSome404urlNotFound() throws Exception {

        /* Mock error parsing. */
        mockStatic(ErrorDetails.class);
        final String errorPayload = "<html>Not Found</html>";
        when(ErrorDetails.parse(errorPayload)).thenThrow(new JSONException("Expected {"));
        final Exception exception = new HttpException(new HttpResponse(404, errorPayload));
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
        checkReleaseFailure(new HttpException(new HttpResponse(404, errorPayload)), never());
    }

    @Test
    public void releaseSuccessDifferentIds() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {

                /* Do the call so that id had changed. */
                Distribute.getInstance().getLatestReleaseDetails("mockGroup", "token");
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(new HttpResponse(200, "mock", Collections.<String, String>emptyMap()));
                return mock(ServiceCall.class);
            }
        }).thenAnswer(new Answer<ServiceCall>() {

            /* On second time we don't answer as it's callback from getLatestReleaseDetails above. */
            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                return mock(ServiceCall.class);
            }
        });

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify on failure we don't complete workflow if ids don't match. */
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void releaseSuccessActivityIsNull() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Update is more recent. */
        when(mReleaseDetails.getVersion()).thenReturn(7);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {

                /* Do the call so that id had changed. */
                Distribute.getInstance().onActivityPaused(mActivity);
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(new HttpResponse(200, "mock", Collections.<String, String>emptyMap()));
                return mock(ServiceCall.class);
            }
        }).thenAnswer(new Answer<ServiceCall>() {

            /* On second time we don't answer as it's callback from getLatestReleaseDetails above. */
            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                return mock(ServiceCall.class);
            }
        });

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify on failure we don't complete workflow if ids don't match. */
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
    }

    @Test
    public void checkReleaseFailsParsing() throws Exception {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(new HttpResponse(200, "mock", Collections.<String, String>emptyMap()));
                return mock(ServiceCall.class);
            }
        });
        Map<String, String> headers = new HashMap<>();
        when(ReleaseDetails.parse(anyString())).thenThrow(new JSONException("mock"));

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void disableBeforeCheckReleaseFails() {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Disable before it fails. */
        Distribute.setEnabled(false);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* Verify complete workflow call ignored. i.e. no more call to delete the state. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
    }

    @Test
    public void disableBeforeCheckReleaseSucceed() {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Disable before it succeeds. */
        Distribute.setEnabled(false);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_TOKEN);

        /* Verify complete workflow call skipped. i.e. no more call to delete the state. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
    }

    @Test
    public void storeBetterEncryptedToken() {

        /* Mock we already have token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some encrypted token");
        when(mCryptoUtils.decrypt(eq("some encrypted token"))).thenReturn(new CryptoUtils.DecryptedData("some token", "some better encrypted token"));
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify storage was updated with new encrypted value. */
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_UPDATE_TOKEN, "some better encrypted token");
    }


    @Test
    public void willNotReportReleaseInstallForPrivateGroupWithoutStoredReleaseHash() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some encrypted token");
        when(mCryptoUtils.decrypt(eq("some encrypted token"))).thenReturn(new CryptoUtils.DecryptedData("some token", "some better encrypted token"));

        /* Mock httpClient. */
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Trigger call. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        ArgumentMatcher<String> urlArg = new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().matches("^https://.*?/sdk/apps/a/releases/private/latest\\?release_hash=" + TEST_HASH + "$");
            }
        };
        verify(mHttpClient).callAsync(argThat(urlArg), eq("GET"), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void willNotReportReleaseInstallForPrivateGroupWhenReleaseHashesDontMatch() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("fake-distribution-id");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-release-hash");

        /* Mock httpClient. */
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Primary storage will be missing data. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        ArgumentMatcher<String> urlArg = new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().matches("^https://.*?/sdk/apps/a/releases/private/latest\\?release_hash=" + TEST_HASH + "$");
            }
        };
        verify(mHttpClient).callAsync(argThat(urlArg), eq("GET"), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void reportReleaseInstallForPrivateGroupWhenReleaseHashesMatch() {
        final String distributionGroupId = "fake-distribution-id";
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn(distributionGroupId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn(TEST_HASH);
        when(SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID)).thenReturn(4);

        /* Mock httpClient. */
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");

        /* Primary storage will be missing data. */
        Distribute.setUpdateTrack(UpdateTrack.PRIVATE);
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        ArgumentMatcher<String> urlArg = new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().matches("^https://.*?/sdk/apps/a/releases/private/latest\\?release_hash=" + TEST_HASH + "&distribution_group_id=" + distributionGroupId + "&downloaded_release_id=4$");
            }
        };
        verify(mHttpClient).callAsync(argThat(urlArg), eq("GET"), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void reportReleaseInstallForPublicGroupWhenReleaseHashesMatch() {
        final String distributionGroupId = "fake-distribution-id";
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn(distributionGroupId);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn(TEST_HASH);
        when(SharedPreferencesManager.getInt(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID)).thenReturn(4);

        /* Mock httpClient. */
        HashMap<String, String> headers = new HashMap<>();

        /* Primary storage will be missing data. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);
        ArgumentMatcher<String> urlArg = new ArgumentMatcher<String>() {

            @Override
            public boolean matches(Object argument) {
                return argument.toString().matches("^https://.*?/public/sdk/apps/a/releases/latest\\?release_hash=" + TEST_HASH + "&install_id=" + mInstallId + "&distribution_group_id=" + distributionGroupId + "&downloaded_release_id=4$");
            }
        };
        verify(mHttpClient).callAsync(argThat(urlArg), eq("GET"), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void enqueueDistributionStartSessionLogAfterEnablingUpdates() {

        /* Setup mock. */
        mockStatic(SessionContext.class);
        SessionContext sessionContext = mock(SessionContext.class);
        when(SessionContext.getInstance()).thenReturn(sessionContext);
        SessionContext.SessionInfo sessionInfo = mock(SessionContext.SessionInfo.class);
        when(sessionContext.getSessionAt(anyLong())).thenReturn(sessionInfo);
        when(sessionInfo.getSessionId()).thenReturn(UUID.randomUUID());
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Enable in-app updates. */
        start();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* Verify the log was sent. */
        verify(mChannel).enqueue(any(DistributionStartSessionLog.class), eq(Distribute.getInstance().getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void dontEnqueueDistributionStartSessionLogIfLastSessionIdIsNull() {

        /* Setup mock. */
        mockStatic(SessionContext.class);
        SessionContext sessionContext = mock(SessionContext.class);
        when(SessionContext.getInstance()).thenReturn(sessionContext);
        SessionContext.SessionInfo sessionInfo = mock(SessionContext.SessionInfo.class);
        when(sessionContext.getSessionAt(anyLong())).thenReturn(sessionInfo);
        when(sessionInfo.getSessionId()).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Enable in-app updates. */
        start();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* Verify the log was sent. */
        verify(mChannel, never()).enqueue(any(DistributionStartSessionLog.class), eq(Distribute.getInstance().getGroupName()), eq(DEFAULTS));
    }

    @Test
    public void dontEnqueueDistributionStartSessionLogIfNoSessionsWereLoggedBefore() {

        /* Setup mock. */
        mockStatic(SessionContext.class);
        SessionContext sessionContext = mock(SessionContext.class);
        when(SessionContext.getInstance()).thenReturn(sessionContext);
        when(sessionContext.getSessionAt(anyLong())).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_REQUEST_ID)).thenReturn("r");

        /* Enable in-app updates. */
        start();
        Distribute.getInstance().storeRedirectionParameters("r", "g", null);

        /* Verify the log was sent. */
        verify(mChannel, never()).enqueue(any(DistributionStartSessionLog.class), eq(Distribute.getInstance().getGroupName()), anyInt());
    }

    @Test
    public void shouldChangeDistributionGroupIdIfStoredIdDoesntMatchDownloadedId() {

        /* Mock release details. */
        String downloadedDistributionGroupId = "fake-downloaded-id";
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("fake-id");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID)).thenReturn(downloadedDistributionGroupId);

        /* Trigger call. */
        start();

        /* Verify group ID. */
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, downloadedDistributionGroupId);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }

    @Test
    public void shouldChangeDistributionGroupIdIfStoredIdIsNull() {

        /* Mock release details. */
        String downloadedDistributionGroupId = "fake-downloaded-id";
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID)).thenReturn(downloadedDistributionGroupId);

        /* Trigger call. */
        start();

        /* Verify group ID. */
        verifyStatic();
        SharedPreferencesManager.putString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID, downloadedDistributionGroupId);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }

    @Test
    public void shouldNotChangeDistributionGroupIdIfStoredIdMatchDownloadedId() {

        /* Mock release details. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("fake-id");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID)).thenReturn("fake-id");

        /* Trigger call. */
        start();

        /* Verify group ID. */
        verifyStatic(never());
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID), anyString());
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }

    @Test
    public void shouldNotChangeDistributionGroupIdIfAppWasntUpdated() {

        /* Mock release details. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-hash");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("fake-id");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID)).thenReturn(null);

        /* Trigger call. */
        start();

        /* Verify group ID. */
        verifyStatic(never());
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID), anyString());
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }

    @Test
    public void shouldNotChangeDistributionGroupIdIfCurrentPackageInfoIsNull() throws Exception {

        /* Mock release details. */
        mockStatic(DistributeUtils.class);
        when(mPackageManager.getPackageInfo(anyString(), anyInt())).thenReturn(null);
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");

        /* Verify group ID. */
        verifyStatic(never());
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID), anyString());
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID);
    }
}
