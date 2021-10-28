/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

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
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Collections;
import java.util.Map;

import static android.content.Context.NOTIFICATION_SERVICE;
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest({DistributeUtils.class, HttpUtils.class, DeviceInfoHelper.class})
public class DistributeTest extends AbstractDistributeTest {

    private static final String DISTRIBUTION_GROUP_ID = "group_id";

    private static final String RELEASE_HASH = "release_hash";

    private static final int RELEASE_ID = 123;

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
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

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launched once. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Mock download completed. */
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_COMPLETED))).thenReturn(DOWNLOAD_STATE_COMPLETED);

        /* Channel initialization. */
        start();
        Distribute.getInstance().onApplicationEnterForeground();

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationChannelNotNullNoDownload() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launched once. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_COMPLETED))).thenReturn(-1);

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
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_ENQUEUED));
        verifyStatic();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), eq(mockTime));
    }

    @Test
    public void startFromBackgroundTwice() {
        start();
        Distribute.getInstance().startFromBackground(mContext);
        verifyStatic(never());
        SharedPreferencesManager.initialize(mContext);
    }

    @Test
    public void setDownloadingReleaseDetailsNotEqualTest() {
        long mockTime = 1000000;
        Distribute.getInstance().setDownloading(mReleaseDetails, mockTime);
        verifyStatic(never());
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_ENQUEUED));
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME), eq(mockTime));
    }

    @Test
    public void setInstallingReleaseDetailsNotEqualTest() {
        Distribute.getInstance().setInstalling(mReleaseDetails);
        verifyStatic(never());
        SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_COMPLETED));
        verifyStatic(never());
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
    }

    @Test
    public void setInstallingTest() {

        /* Mock release details and startFromBackground to apply it. */
        mockReleaseDetails(false);
        Distribute.getInstance().startFromBackground(mContext);
        Distribute.getInstance().setInstalling(mReleaseDetails);

        /* Verify. */
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_STATE));
        verifyStatic(never());
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_INSTALLING));
        verifyReleaseDetailsAreStored();
    }

    @Test
    public void setInstallingMandatoryReleaseDetailsTest() {

        /* Mock release details and startFromBackground to apply it. */
        mockReleaseDetails(true);
        Distribute.getInstance().startFromBackground(mContext);
        Distribute.getInstance().setInstalling(mReleaseDetails);

        /* Verify. */
        verifyStatic();
        DistributeUtils.getStoredDownloadState();
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_INSTALLING));
        verifyReleaseDetailsAreStored();
    }

    private void mockReleaseDetails(boolean mandatoryUpdate) {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(mandatoryUpdate);
        when(mReleaseDetails.getDistributionGroupId()).thenReturn(DISTRIBUTION_GROUP_ID);
        when(mReleaseDetails.getReleaseHash()).thenReturn(RELEASE_HASH);
        when(mReleaseDetails.getId()).thenReturn(RELEASE_ID);
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
    }

    private void verifyReleaseDetailsAreStored() {
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), eq(DISTRIBUTION_GROUP_ID));
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), eq(RELEASE_HASH));
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), eq(RELEASE_ID));
    }

    @Test
    public void cancelingNotification() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);
        when(DistributeUtils.getNotificationId()).thenReturn(2);
        NotificationManager manager = mock(NotificationManager.class);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(manager);
        Distribute.getInstance().onStarted(mContext, mChannel, "a", null, true);
        Distribute.getInstance().completeWorkflow();
        verify(manager).cancel(any(Integer.class));
    }

    @Test
    public void firstDownloadNotificationApi26() throws Exception {
        firstDownloadNotification(Build.VERSION_CODES.O);
    }

    @Test
    public void firstDownloadNotificationApi21() throws Exception {
        firstDownloadNotification(Build.VERSION_CODES.LOLLIPOP);
    }

    @Test
    public void notifyDownloadNoReleaseDetails() throws Exception {
        mockStatic(DistributeUtils.class);
        whenNew(Notification.Builder.class).withAnyArguments()
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock another ReleaseDetails. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mockReleaseDetails);

        /* Verify. */
        assertTrue(notifyDownloadResult);
        verify(mNotificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void notifyDownloadStateNotified() throws Exception {
        mockStatic(DistributeUtils.class);
        whenNew(Notification.Builder.class).withAnyArguments()
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock notified state. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mReleaseDetails);

        /* Verify. */
        assertFalse(notifyDownloadResult);
        verify(mNotificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void notifyDownloadForegroundActivity() throws Exception {
        mockStatic(DistributeUtils.class);
        whenNew(Notification.Builder.class).withAnyArguments()
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(mNotificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock foreground activity. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mReleaseDetails);

        /* Verify. */
        assertFalse(notifyDownloadResult);
        verify(mNotificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void checkNotificationState() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        Distribute.getInstance().startFromBackground(mContext);
        assertTrue(Distribute.getInstance().notifyDownload(mock(ReleaseDetails.class)));
    }

    @Test
    public void updateReleaseDetailsFromBackground() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);

        /* mReleaseDownloader is null and is created. */
        Distribute.getInstance().startFromBackground(mContext);
        verifyStatic();
//        ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloader.Listener.class));

        /* mReleaseDetails not null but id is not equal to mReleaseDownloader details id. */
        Distribute.getInstance().startFromBackground(mContext);

        /* mReleaseDetails is null. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(null);
        Distribute.getInstance().startFromBackground(mContext);
        verify(mReleaseDownloader).cancel();
    }

    @Test
    public void discardDownloadAsAppUpdateTest() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is smaller than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(1L);

        resumeWorkflow(mock(Activity.class));

        /* Verify that previous tasks are cancelled. */
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_RELEASE_DETAILS));
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_STATE));
        verifyStatic();
        SharedPreferencesManager.remove(eq(PREFERENCE_KEY_DOWNLOAD_TIME));
    }

    @Test
    public void restartDownloadNotEnqueued() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's not mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(false);

        resumeWorkflow(mock(Activity.class));

        verify(mDialog, never()).show();
    }

    @Test
    public void showDownloadProgressTest() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mActivity);
        verify(mReleaseDownloaderListener).showDownloadProgress(mActivity);
    }

    @Test
    public void doNotShowDownloadProgressTestInBackground() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflowWithNoOnResume();
        verify(mReleaseDownloaderListener, never()).showDownloadProgress(mActivity);
    }

    @Test
    public void showDownloadProgressTestInForeground() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

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
        verifyStatic(never());
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void showUpdateSetupFailedDialogInForeground() {
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY)).thenReturn("failed_message");

        /* Trigger call. */
        Distribute.getInstance().onActivityResumed(mActivity);
        start();

        /* Verify dialog. */
        verify(mDialogBuilder).create();
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_UPDATE_SETUP_FAILED_MESSAGE_KEY);
    }

    @Test
    public void showDownloadProgressAndActivityTest() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        ProgressDialog progressDialog = mock(ProgressDialog.class);
        when(mReleaseDownloaderListener.showDownloadProgress(mActivity)).thenReturn(progressDialog);

        resumeWorkflow(mActivity);
        verify(mReleaseDownloaderListener).showDownloadProgress(mActivity);
        verify(progressDialog).show();
    }

    @Test
    public void showDownloadProgressNullDownloaderListenerTest() throws Exception {
        whenNew(ReleaseDownloadListener.class).withArguments(any(Context.class), any(ReleaseDetails.class)).thenReturn(null);
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_ENQUEUED);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mock(Activity.class));
        verify(mReleaseDownloaderListener, never()).showDownloadProgress(mActivity);
    }

    @Test
    public void restartedDuringMandatoryUpdate() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mock(Activity.class));

        verify(mDialog, never()).show();
    }

    @Test
    public void downloadAlreadyCheckedTest() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        resumeWorkflow(mock(Activity.class));

        /* Call resume twice when download already checked. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
    }

    @Test
    public void showUpdateDialogReleaseDownloaderNullTest() {

        /* Mock mReleaseDownloader null. */
//        when(ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloadListener.class))).thenReturn(null);

        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mock(Activity.class));

        /*
         * Call resume twice when download already checked, mReleaseDetails are not null, package is installing.
         * mReleaseDownloader is null.
         */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder).create();
    }

    @Test
    public void showUpdateDialogReleaseDownloaderDownloadingTest() {

        /* Mock mReleaseDownloader is downloading. */
        when(mReleaseDownloader.isDownloading()).thenReturn(true);

        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mock(Activity.class));

        /*
         * Call resume twice when download already checked, mReleaseDetails are not null, package is installing.
         * mReleaseDownloader is downloading.
         */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, never()).create();
    }

    private void resumeWorkflow(Activity activity) {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        start();
        Distribute.setEnabledForDebuggableBuild(true);
        Distribute.getInstance().onActivityResumed(activity);
    }

    private void resumeWorkflowWithNoOnResume() {
        Whitebox.setInternalState(mApplicationInfo, "flags", ApplicationInfo.FLAG_DEBUGGABLE);
        start();
        Distribute.setEnabledForDebuggableBuild(true);
    }

    @Test
    public void getLastReleaseDetailsWithDifferentHttpClients() {

        /* Prepare data. */
        HttpClient mockHttpClient = mock(HttpClient.class);
        DependencyConfiguration.setHttpClient(mockHttpClient);
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("mock-hash");

        /* Call get last release details. */
        Distribute.getInstance().getLatestReleaseDetails(anyString(), anyString());

        /* Verify. */
        verifyStatic(never());
        HttpUtils.createHttpClient(any(Context.class));

        /* Clear http client. */
        DependencyConfiguration.setHttpClient(null);

        /* Call get last release details. */
        Distribute.getInstance().getLatestReleaseDetails(anyString(), anyString());

        /* Verify. */
        verifyStatic();
        HttpUtils.createHttpClient(any(Context.class));
    }

    @Test
    public void showMandatoryDownloadReadyDialogTest() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1).thenReturn(DOWNLOAD_STATE_INSTALLING);

        /* Mock that download time is bigger than packageInfo.lastUpdateTime. */
        when(SharedPreferencesManager.getLong(eq(PREFERENCE_KEY_DOWNLOAD_TIME))).thenReturn(3L);

        /* mReleaseDetails is not null and it's a mandatory update. */
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);

        resumeWorkflow(mock(Activity.class));

        /*
         * Call resume twice when download already checked, mReleaseDetails are not null, package is installing.
         * current dialog is null.
         */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

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

        /* Complete workflow and unset mReleaseDetails. */
        Distribute.getInstance().completeWorkflow();
        doNothing().when(mToast).show();

        /* Click. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);

        /* Verify that download is resumed after click Install but only twice (on third time Distribute is disabled). */
        verify(mReleaseDownloader, times(2)).resume();
    }

    @Test
    public void tryResetWorkflowWhenApplicationEnterForegroundWhenChannelNull() {

        /* Mock download state. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_COMPLETED);
        final ServiceCall call = mock(ServiceCall.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded(new HttpResponse(200, ""));
                return call;
            }
        }).when(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Starting distribute. */
        Distribute.getInstance().onStarting(mAppCenterHandler);

        /* Start activity. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is not checked after we reset workflow. */
        verify(mHttpClient, never()).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void tryResetWorkflowWhenApplicationEnterForegroundWhenChannelNotNull() {

        /* Prepare data. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_COMPLETED);
        final ServiceCall call = mock(ServiceCall.class);
        doAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocationOnMock) {
                ((ServiceCallback) invocationOnMock.getArguments()[4]).onCallSucceeded(new HttpResponse(200, ""));
                return call;
            }
        }).when(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Start distribute. */
        start();

        /* Start activity. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is checked after we reset workflow. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Stop activity. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onApplicationEnterBackground();

        /* Verify that all calls were completed. */
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Enter foreground again. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify download is checked after we reset workflow again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }
    
    @Test
    public void checkUpdateReleaseAfterInterruptDownloading() {

        /* Prepare data. */
        mockStatic(DistributeUtils.class);
        mockStatic(DeviceInfoHelper.class);
        when(mReleaseDetails.getVersion()).thenReturn(1);
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(true);
        when(DeviceInfoHelper.getVersionCode(any(PackageInfo.class))).thenReturn(0);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);

        /* Start distribute. */
        start();

        /* Start activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify that check release for update was called. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        verify(mHttpClient).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());

        /* Complete the first call. */
        httpCallback.getValue().onCallSucceeded(mock(HttpResponse.class));

        /* Disable and enable distribute module. */
        Distribute.setEnabled(false);
        Distribute.setEnabled(true);

        /* Verify that check release for update was called again. */
        verify(mHttpClient, times(2)).callAsync(anyString(), anyString(), eq(Collections.<String, String>emptyMap()), any(HttpClient.CallTemplate.class), httpCallback.capture());
    }

    private void firstDownloadNotification(int apiLevel) throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);
        mockStatic(DistributeUtils.class);
        NotificationManager manager = mock(NotificationManager.class);
        whenNew(Notification.Builder.class).withAnyArguments()
                .thenReturn(mock(Notification.Builder.class, RETURNS_MOCKS));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(DistributeUtils.getNotificationId()).thenReturn(0);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(manager);
        Distribute.getInstance().startFromBackground(mContext);
        Distribute.getInstance().onStarted(mContext, mChannel, "0", "Anna", false);
        Distribute.getInstance().notifyDownload(mReleaseDetails);
        verify(manager).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }
}
