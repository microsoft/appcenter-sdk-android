/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_ENQUEUED;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_INSTALLING;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_NOTIFIED;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.widget.Toast;

import com.microsoft.appcenter.DependencyConfiguration;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.HttpUtils;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;

import java.util.Collections;
import java.util.Map;

@PrepareForTest({
        DistributeUtils.class,
        DeviceInfoHelper.class
})
public class DistributeTest extends AbstractDistributeTest {

    private static final String DISTRIBUTION_GROUP_ID = "group_id";

    private static final String RELEASE_HASH = "release_hash";

    private static final int RELEASE_ID = 123;

    @Mock
    private UpdateInstaller mReleaseInstaller;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_COMPLETED);
        whenNew(UpdateInstaller.class).withAnyArguments().thenReturn(mReleaseInstaller);
    }

    @Test
    public void singleton() {
        assertSame(Distribute.getInstance(), Distribute.getInstance());
    }

    @Test
    public void checkFactories() {
        Map<String, LogFactory> factories = Distribute.getInstance().getLogFactories();
        assertNotNull(factories);
        assertTrue(factories.remove(DistributionStartSessionLog.TYPE) instanceof DistributionStartSessionLogFactory);
        assertTrue(factories.isEmpty());
    }

    @Test
    public void recreateActivityTwice() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launched once. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitialization() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launch one. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationNullIntent() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launched once. */
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(null);
        ComponentName componentName = mock(ComponentName.class);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationChannelNotNull() {

        /* Our activity is launched once. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Channel initialization. */
        start();
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationChannelNotNullNoDownload() {

        /* Our activity is launched once. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Callback. */
        start();
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void resumeNullActivity() {
        start();
        Distribute.getInstance().onActivityResumed(null);

        /* No exceptions. */
    }

    @Test
    public void setDownloadingReleaseDetailsEqualTest() {

        /* Mock release details and startFromBackground to apply it. */
        mockReleaseDetails(true);
        Distribute.getInstance().startFromBackground(mContext);

        long mockTime = 1000000;
        Distribute.getInstance().setDownloading(mReleaseDetails, mockTime);
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_ENQUEUED));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), eq(mockTime));
    }

    @Test
    public void startFromBackgroundTwice() {
        start();
        Distribute.getInstance().startFromBackground(mContext);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.initialize(mContext);
    }

    @Test
    public void setDownloadingReleaseDetailsNotEqualTest() {
        long mockTime = 1000000;
        Distribute.getInstance().setDownloading(mReleaseDetails, mockTime);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_ENQUEUED));
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), eq(mockTime));
    }

    @Test
    public void setInstallingReleaseDetailsNotEqualTest() {
        Distribute.getInstance().setInstalling(mReleaseDetails, mUri);
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_COMPLETED));
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }

    @Test
    public void setInstallingTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* Mock release details and resumeWorkflow to apply it. */
        mockReleaseDetails(false);
        resumeWorkflow(mActivity);
        Distribute.getInstance().setInstalling(mReleaseDetails, mUri);

        /* Verify. */
        verifyReleaseDetailsAreStored();
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_INSTALLING));
        verify(mReleaseInstaller).install(mUri);

        /* Start installing second time from notified state. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().setInstalling(mReleaseDetails, mUri);
        verifyStatic(SharedPreferencesManager.class, times(2));
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_INSTALLING));
        verify(mReleaseInstaller, times(2)).install(mUri);

        /* Installer should be aware of resume event. */
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mReleaseInstaller).resume();

        /* Completing workflow clears installer. */
        Distribute.getInstance().completeWorkflow(mReleaseDetails);
        verify(mReleaseInstaller).clear();
    }

    private void mockReleaseDetails(boolean mandatoryUpdate) {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(mandatoryUpdate);
        when(mReleaseDetails.getDistributionGroupId()).thenReturn(DISTRIBUTION_GROUP_ID);
        when(mReleaseDetails.getReleaseHash()).thenReturn(RELEASE_HASH);
        when(mReleaseDetails.getId()).thenReturn(RELEASE_ID);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
    }

    private void verifyReleaseDetailsAreStored() {
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), eq(DISTRIBUTION_GROUP_ID));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), eq(RELEASE_HASH));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), eq(RELEASE_ID));
    }

    @Test
    public void cancelingNotification() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);
        Distribute.getInstance().onStarted(mContext, mChannel, "a", null, true);
        Distribute.getInstance().completeWorkflow();
        verifyStatic(DistributeUtils.class);
        DistributeUtils.cancelNotification(mContext);
    }

    @Test
    public void postDownloadNotification() {
        when(DistributeUtils.getStoredDownloadState())
                .thenReturn(DOWNLOAD_STATE_ENQUEUED)
                .thenReturn(DOWNLOAD_STATE_NOTIFIED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        mockReleaseDetails(false);
        Intent intent = mock(Intent.class);
        when(DistributeUtils.getResumeAppIntent(any(Context.class))).thenReturn(intent);

        /* Set installing state. */
        Distribute.getInstance().startFromBackground(mContext);
        Distribute.getInstance().onStarted(mContext, mChannel, "0", null, false);
        Distribute.getInstance().setInstalling(mReleaseDetails, mock(Uri.class));

        /* Verify. */
        verifyStatic(DistributeUtils.class);
        DistributeUtils.postNotification(eq(mContext), anyString(), anyString(), eq(intent));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_NOTIFIED));

        /* Resume after posting notification resumes downloader that produces one more completion event. */
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mReleaseDownloader).resume();
    }

    @Test
    public void updateReleaseDetailsFromBackground() {
        mockReleaseDetails(false);

        /* mReleaseDownloader is null and is created. */
        Distribute.getInstance().startFromBackground(mContext);
        verifyStatic(ReleaseDownloaderFactory.class);
        ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloader.Listener.class));

        /* mReleaseDetails not null but id is not equal to mReleaseDownloader details id. */
        Distribute.getInstance().startFromBackground(mContext);

        /* mReleaseDetails is null. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(null);
        Distribute.getInstance().startFromBackground(mContext);
        verify(mReleaseDownloader).cancel();

        /* Cannot resume download without release details. */
        Distribute.getInstance().resumeDownload();
        verify(mReleaseDownloader, never()).resume();
    }

    @Test
    public void discardDownloadAsAppUpdateTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_INSTALLING);

        /* Mock that download time is smaller than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(1L);

        resumeWorkflow(mActivity);

        /* Verify that previous tasks are cancelled. */
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_STATE));
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_TIME));
    }

    @Test
    public void restartDownloadNotEnqueued() {

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's not mandatory update. */
        mockReleaseDetails(false);
        resumeWorkflow(mActivity);

        verify(mDialog, never()).show();
    }

    @Test
    public void showDownloadProgressTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);
        verify(mReleaseDownloaderListener).showDownloadProgress(mActivity);
    }

    @Test
    public void doNotShowDownloadProgressTestInBackground() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflowWithNoOnResume();
        verify(mReleaseDownloaderListener, never()).showDownloadProgress(mActivity);
    }

    @Test
    public void showDownloadProgressTestInForeground() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);
        verify(mReleaseDownloaderListener).showDownloadProgress(mActivity);
    }

    @Test
    public void doNotShowUpdateSetupFailedDialogInBackground() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message");

        /* Trigger call. */
        start();

        /* Verify dialog. */
        verify(mDialogBuilder, never()).create();
        verifyStatic(SharedPreferencesManager.class, never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void doNotShowUpdateSetupFailedDialogInNullForegroundActivity() {
        /* Trigger call. */
        start();
        Distribute.getInstance().showUpdateSetupFailedDialog();

        /* Verify dialog. */
        verify(mDialogBuilder, never()).create();
    }

    @Test
    public void showUpdateSetupFailedDialogInForeground() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message");

        /* Trigger call. */
        Distribute.getInstance().onActivityResumed(mActivity);
        start();

        /* Verify dialog. */
        verify(mDialogBuilder).create();
        verifyStatic(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void showDownloadProgressAndActivityTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);

        @SuppressWarnings({"deprecation", "RedundantSuppression"})
        android.app.ProgressDialog progressDialog = mock(android.app.ProgressDialog.class);
        doReturn(progressDialog).when(mReleaseDownloaderListener).showDownloadProgress(mActivity);

        resumeWorkflow(mActivity);
        verify(mReleaseDownloaderListener).showDownloadProgress(mActivity);
        verify(progressDialog).show();
    }

    @Test
    public void showDownloadProgressNullDownloaderListenerTest() throws Exception {
        whenNew(ReleaseDownloadListener.class)
                .withArguments(any(Context.class), any(ReleaseDetails.class))
                .thenReturn(null);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);

        resumeWorkflow(mock(Activity.class));
        verify(mReleaseDownloaderListener, never()).showDownloadProgress(mActivity);
    }

    @Test
    public void restartedDuringMandatoryUpdate() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);

        verify(mDialog, never()).show();
    }

    @Test
    public void downloadAlreadyCheckedTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        resumeWorkflow(mActivity);

        /* Call resume twice when download already checked. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
    }

    @Test
    public void showUpdateDialogReleaseDownloaderNullTest() {
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Mock mReleaseDownloader null. */
        when(ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloadListener.class))).thenReturn(null);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);
        verify(mDialogBuilder).create();
    }

    @Test
    public void showUpdateDialogReleaseDownloaderDownloadingTest() {

        /* Mock mReleaseDownloader is downloading. */
        when(mReleaseDownloader.isDownloading()).thenReturn(true);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);

        /*
         * Call resume twice when download already checked, mReleaseDetails are not null, package is installing.
         * mReleaseDownloader is downloading.
         */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, never()).create();
    }

    private void resumeWorkflow(Activity activity) {
        Whitebox.setInternalState(mApplicationInfo, "flags", FLAG_DEBUGGABLE);
        start();
        Distribute.setEnabledForDebuggableBuild(true);
        Distribute.getInstance().onActivityResumed(activity);
    }

    private void resumeWorkflowWithNoOnResume() {
        Whitebox.setInternalState(mApplicationInfo, "flags", FLAG_DEBUGGABLE);
        start();
        Distribute.setEnabledForDebuggableBuild(true);
    }

    @Test
    public void getLastReleaseDetailsWithDifferentHttpClients() {

        /* Prepare data. */
        HttpClient mockHttpClient = mock(HttpClient.class);
        DependencyConfiguration.setHttpClient(mockHttpClient);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("mock-hash");
        Distribute.getInstance().startFromBackground(mContext);

        /* Call get last release details. */
        Distribute.getInstance().getLatestReleaseDetails(DISTRIBUTION_GROUP_ID, "token");

        /* Verify. */
        verifyStatic(HttpUtils.class, never());
        HttpUtils.createHttpClient(any(Context.class));

        /* Clear http client. */
        DependencyConfiguration.setHttpClient(null);

        /* Call get last release details. */
        Distribute.getInstance().getLatestReleaseDetails(DISTRIBUTION_GROUP_ID, "token");

        /* Verify. */
        verifyStatic(HttpUtils.class);
        HttpUtils.createHttpClient(any(Context.class));
    }

    @Test
    public void showMandatoryDownloadReadyDialogTest() {
        when(DistributeUtils.getStoredDownloadState())
                .thenReturn(DOWNLOAD_STATE_COMPLETED)
                .thenReturn(DOWNLOAD_STATE_INSTALLING);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        mockReleaseDetails(true);
        resumeWorkflow(mActivity);
        verify(mDialog, never()).show();

        /*
         * Call resume twice when download already checked, mReleaseDetails are not null, package is installing.
         * current dialog is null.
         */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();

        /* Ignore if release details are not equal. */
        Distribute.getInstance().showMandatoryDownloadReadyDialog(mock(ReleaseDetails.class));
        verifyNoMoreInteractions(mDialog);

        /* Ignore in background. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().showMandatoryDownloadReadyDialog(mReleaseDetails);
        verifyNoMoreInteractions(mDialog);

        /* Call resume third time to trigger dialog refresh but it's not yet showing. */
        when(mDialog.isShowing()).thenReturn(false).thenReturn(true);
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Call resume fourth time to trigger dialog refresh and mock that it's showing, verify it's hidden. */
        Distribute.getInstance().onActivityResumed(mActivity);
        verify(mDialog, times(1)).hide();

        /*
         * Call resume fifth time with the same activity reference as before so that
         * mLastActivityWithDialog and mForegroundActivity are equal and refreshed dialog
         * is the same and it's not hidden.
         */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify install dialog is created. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(3)).setPositiveButton(eq(R.string.appcenter_distribute_install), clickListener.capture());
        verify(mDialogBuilder, times(3)).create();

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verify(mReleaseDownloader).resume();

        /* Complete workflow and unset mReleaseDetails. */
        Distribute.getInstance().completeWorkflow();
        doNothing().when(mToast).show();

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verifyNoMoreInteractions(mReleaseDownloader);
    }

    @Test
    public void tryResetWorkflowWhenApplicationEnterForegroundWhenChannelNull() {
        final ServiceCall call = mock(ServiceCall.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded(new HttpResponse(200, ""));
                return call;
            }
        }).when(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Starting distribute. */
        Distribute.getInstance().onStarting(mAppCenterHandler);

        /* Start activity. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is not checked after we reset workflow. */
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void tryResetWorkflowWhenApplicationEnterForegroundWhenChannelNotNull() {
        final ServiceCall call = mock(ServiceCall.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded(new HttpResponse(200, ""));
                return call;
            }
        }).when(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Start distribute. */
        start();

        /* Start activity. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is checked after we reset workflow. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Stop activity. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onApplicationEnterBackground();

        /* Verify that all calls were completed. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Enter foreground again. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is checked after we reset workflow again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void checkUpdateReleaseAfterInterruptDownloading() {
        mockReleaseDetails(true);
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(DeviceInfoHelper.getVersionCode(any(PackageInfo.class))).thenReturn(0);

        /* Start distribute. */
        start();

        /* Start activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify that check release for update was called. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete the first call. */
        HttpResponse response = mock(HttpResponse.class);
        when(response.getPayload()).thenReturn("<mock_release_details>");
        httpCallback.getValue().onCallSucceeded(response);

        /* Disable and enable distribute module. */
        Distribute.setEnabled(false);
        Distribute.setEnabled(true);

        /* Verify that check release for update was called again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());
    }

    @Test
    public void showToast() {
        int messageId = R.string.appcenter_distribute_dialog_actioned_on_disabled_toast;
        start();

        /* Without activity. */
        Distribute.getInstance().showToast(messageId);
        verify(mToast).show();
        verifyStatic(Toast.class);
        Toast.makeText(eq(mContext), eq(messageId), anyInt());

        /* With activity. */
        Distribute.getInstance().onActivityResumed(mActivity);
        Distribute.getInstance().showToast(messageId);
        verify(mToast, times(2)).show();
        verifyStatic(Toast.class);
        Toast.makeText(eq(mActivity), eq(messageId), anyInt());
    }
}
