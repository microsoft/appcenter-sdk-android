/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.distribute.ingestion.models.DistributionStartSessionLog;
import com.microsoft.appcenter.distribute.ingestion.models.json.DistributionStartSessionLogFactory;
import com.microsoft.appcenter.ingestion.models.json.LogFactory;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PrepareForTest;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest(DistributeUtils.class)
public class DistributeTest extends AbstractDistributeTest {

    private String mMockGroupId = "group_id";
    private String mMockReleaseHash = "release_hash";
    private int mReleaseId = 123;

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

        /* Our activity is launch one. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onActivityCreated(mActivity, null);
        Distribute.getInstance().onActivityCreated(mActivity, null);

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
        Distribute.getInstance().onActivityCreated(mActivity, null);

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationNullIntent() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launch one. */
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(null);
        ComponentName componentName = mock(ComponentName.class);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());

        /* Callback. */
        Distribute.getInstance().onActivityCreated(mActivity, null);

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationChannelNotNull() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launch one. */
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
        Distribute.getInstance().onActivityCreated(mActivity, null);

        /* No exceptions. */
    }

    @Test
    public void recreateLauncherActivityBeforeFullInitializationChannelNotNullNoDownload() {

        /* SharedPreferencesManager isn't initialized yet. */
        when(SharedPreferencesManager.getInt(anyString(), anyInt()))
                .thenThrow(new NullPointerException());

        /* Our activity is launch one. */
        Intent intent = mock(Intent.class);
        when(mPackageManager.getLaunchIntentForPackage(anyString())).thenReturn(intent);
        ComponentName componentName = mock(ComponentName.class);
        when(intent.resolveActivity(mPackageManager)).thenReturn(componentName);
        when(componentName.getClassName()).thenReturn(mActivity.getClass().getName());
        mockStatic(SharedPreferencesManager.class);
        when(SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_COMPLETED))).thenReturn(-1);

        /* Callback. */
        start();
        Distribute.getInstance().onActivityCreated(mActivity, null);

        /* No exceptions. */
    }

    @Test
    public void setDownloadingReleaseDetailsEqualTest() {

        /* Moc release details and startFromBackground to apply it. */
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

        /* Moc release details and startFromBackground to apply it. */
        mockReleaseDetails(false);
        Distribute.getInstance().startFromBackground(mContext);

        Distribute.getInstance().setInstalling(mReleaseDetails);

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

        /* Moc release details and startFromBackground to apply it. */
        mockReleaseDetails(true);
        Distribute.getInstance().startFromBackground(mContext);

        Distribute.getInstance().setInstalling(mReleaseDetails);

        verifyStatic();
        DistributeUtils.getStoredDownloadState();
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), eq(DOWNLOAD_STATE_INSTALLING));
        verifyReleaseDetailsAreStored();
    }

    private void mockReleaseDetails(boolean mandatoryUpdate) {
        when(mReleaseDetails.isMandatoryUpdate()).thenReturn(mandatoryUpdate);
        when(mReleaseDetails.getDistributionGroupId()).thenReturn(mMockGroupId);
        when(mReleaseDetails.getReleaseHash()).thenReturn(mMockReleaseHash);
        when(mReleaseDetails.getId()).thenReturn(mReleaseId);
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
    }

    private void verifyReleaseDetailsAreStored() {
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_DISTRIBUTION_GROUP_ID), eq(mMockGroupId));
        verifyStatic();
        SharedPreferencesManager.putString(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH), eq(mMockReleaseHash));
        verifyStatic();
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID), eq(mReleaseId));
    }

    @Test
    public void cancelingNotification() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);
        when(DistributeUtils.getNotificationId()).thenReturn(2);
        NotificationManager manager = mock(NotificationManager.class);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(manager);
        Distribute.getInstance().onStarted(mContext, mChannel, any(String.class), any(String.class), false);
        Distribute.getInstance().completeWorkflow();
        verify(manager).cancel(any(Integer.class));
    }

    @Test
    public void firstDownloadNotificationApi26() throws Exception {
        firstDownloadNotification(Build.VERSION_CODES.O);
    }

    @Test
    public void firstDownloadNotificationApi19() throws Exception {
        firstDownloadNotification(Build.VERSION_CODES.KITKAT);
    }

    @Test
    public void notifyDownloadNoReleaseDetails() throws Exception {
        mockStatic(DistributeUtils.class);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock another ReleaseDetails. */
        ReleaseDetails mockReleaseDetails = mock(ReleaseDetails.class);

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mockReleaseDetails, mInstallIntent);

        /* Verify. */
        assertTrue(notifyDownloadResult);
        verify(notificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void notifyDownloadStateNotified() throws Exception {
        mockStatic(DistributeUtils.class);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock notified state. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_NOTIFIED);

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mReleaseDetails, mInstallIntent);

        /* Verify. */
        assertFalse(notifyDownloadResult);
        verify(notificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void notifyDownloadForegroundActivity() throws Exception {
        mockStatic(DistributeUtils.class);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(notificationManager);
        Distribute.getInstance().startFromBackground(mContext);

        /* Mock foreground activity. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Call notify download. */
        boolean notifyDownloadResult = Distribute.getInstance().notifyDownload(mReleaseDetails, mInstallIntent);

        /* Verify. */
        assertFalse(notifyDownloadResult);
        verify(notificationManager, never()).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    @Test
    public void updateReleaseDetailsFromBackground() {
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        Distribute.getInstance().startFromBackground(mContext);
        verifyStatic();
        ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloader.Listener.class));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(null);
        Distribute.getInstance().startFromBackground(mContext);
        verify(mReleaseDownloader).cancel();
    }

    private void firstDownloadNotification(int apiLevel) throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);
        mockStatic(DistributeUtils.class);
        NotificationManager manager = mock(NotificationManager.class);
        Notification.Builder notificationBuilder = mockNotificationBuilderChain();
        when(notificationBuilder.build()).thenReturn(mock(Notification.class));
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);
        when(DistributeUtils.getNotificationId()).thenReturn(0);
        when(mContext.getSystemService(NOTIFICATION_SERVICE)).thenReturn(manager);
        Distribute.getInstance().startFromBackground(mContext);
        Distribute.getInstance().onStarted(mContext, mChannel, "0", "Anna", false);
        Distribute.getInstance().notifyDownload(mReleaseDetails, mInstallIntent);
        verify(manager).notify(eq(DistributeUtils.getNotificationId()), any(Notification.class));
    }

    private Notification.Builder mockNotificationBuilderChain() throws Exception {
        Notification.Builder notificationBuilder = mock(Notification.Builder.class);
        whenNew(Notification.Builder.class).withAnyArguments().thenReturn(notificationBuilder);
        when(notificationBuilder.setTicker(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentTitle(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentText(anyString())).thenReturn(notificationBuilder);
        when(notificationBuilder.setSmallIcon(anyInt())).thenReturn(notificationBuilder);
        when(notificationBuilder.setContentIntent(any(PendingIntent.class))).thenReturn(notificationBuilder);
        when(notificationBuilder.setChannelId(anyString())).thenReturn(notificationBuilder);
        return notificationBuilder;
    }
}
