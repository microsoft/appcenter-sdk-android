/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.distribute.download.ReleaseDownloader;
import com.microsoft.appcenter.distribute.download.ReleaseDownloaderFactory;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpException;
import com.microsoft.appcenter.http.HttpResponse;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.DeviceInfoHelper;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_POSTPONE_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@SuppressWarnings("unused")
@PrepareForTest({ErrorDetails.class, DistributeUtils.class, DeviceInfoHelper.class})
@RunWith(PowerMockRunner.class)
public class DistributeCustomizationTest extends AbstractDistributeTest {

    private void start(Distribute distribute) {
        distribute.onStarting(mAppCenterHandler);
        distribute.onStarted(mContext, mock(Channel.class), "a", null, true);
    }

    @Test
    public void distributeListenerVersionsEqual() throws Exception {

        /* Mock hash. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("some_hash");

        /* Set the package version equal to the portal's one. */
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getVersionCode(any(PackageInfo.class))).thenReturn(10);

        /* Mock http call. */
        mockForCustomizationTest(true);

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        Distribute.setListener(listener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeListenerNoReleaseForUser() throws Exception {

        /* Mock error parsing. */
        ErrorDetails errorDetails = mock(ErrorDetails.class);
        when(errorDetails.getCode()).thenReturn(ErrorDetails.NO_RELEASES_FOR_USER_CODE);
        mockStatic(ErrorDetails.class);
        when(ErrorDetails.parse(anyString())).thenReturn(errorDetails);

        /* Mock http call. */
        mockHtpCallFailed("{code:'no_releases_for_user'}");

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        Distribute.setListener(listener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeListenerNoReleaseFound() throws Exception {

        /* Mock error parsing. */
        ErrorDetails errorDetails = mock(ErrorDetails.class);
        when(errorDetails.getCode()).thenReturn(ErrorDetails.NO_RELEASES_FOUND);
        mockStatic(ErrorDetails.class);
        when(ErrorDetails.parse(anyString())).thenReturn(errorDetails);

        /* Mock http call. */
        mockHtpCallFailed("{code:'not_found'}");

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        Distribute.setListener(listener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called again. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeNotRecent() throws Exception {

        /* Set the package version higher than the portal's one. */
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getVersionCode(any(PackageInfo.class))).thenReturn(11);

        /* Mock http call. */
        mockForCustomizationTest(true);

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        Distribute.setListener(listener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the right listener gets called again. */
        verify(listener).onNoReleaseAvailable(mActivity);
        verify(listener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeNotRecentNoListener() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNotRecentCoverage(null, listener, false);
    }

    @Test
    public void distributeNotRecentNoActivity() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNotRecentCoverage(listener, listener, true);
    }

    @Test
    public void distributeNotRecentNoListenerNoActivity() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNotRecentCoverage(null, listener, true);
    }

    private void distributeNotRecentCoverage(DistributeListener actualListener, DistributeListener verificationListener, boolean onPause) throws Exception {

        /* Set the package version higher than the portal's one. */
        mockStatic(DeviceInfoHelper.class);
        when(DeviceInfoHelper.getVersionCode(any(PackageInfo.class))).thenReturn(11);

        /* Mock http call. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), httpCallback.capture())).thenReturn(mock(ServiceCall.class));

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(details.getShortVersion()).thenReturn("2.3.4");
        when(details.isMandatoryUpdate()).thenReturn(false);
        when(details.getReleaseHash()).thenReturn("some_hash");
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock update token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        Distribute.setListener(actualListener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);
        if (onPause) {
            Distribute.getInstance().onActivityPaused(mActivity);
        }

        /* Simulate network latency. */
        httpCallback.getValue().onCallSucceeded(new HttpResponse(200, "mock"));

        /* Verify the right listener gets called. */
        verify(verificationListener, never()).onNoReleaseAvailable(any(Activity.class));
        verify(verificationListener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeNotFoundAvailableNoListener() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNoReleaseAvailableCoverage(null, listener, false);
    }

    @Test
    public void distributeNotFoundNoActivity() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNoReleaseAvailableCoverage(listener, listener, true);
    }

    @Test
    public void distributeNotFoundNoListenerNoActivity() throws Exception {
        DistributeListener listener = mock(DistributeListener.class);
        distributeNoReleaseAvailableCoverage(null, listener, true);
    }

    private void distributeNoReleaseAvailableCoverage(DistributeListener actualListener, DistributeListener verificationListener, boolean onPause) throws Exception {

        /* Mock error parsing. */
        ErrorDetails errorDetails = mock(ErrorDetails.class);
        when(errorDetails.getCode()).thenReturn(ErrorDetails.NO_RELEASES_FOUND);
        mockStatic(ErrorDetails.class);
        when(ErrorDetails.parse(anyString())).thenReturn(errorDetails);

        /* Mock http call. */
        final HttpException httpException = new HttpException(new HttpResponse(404, "{code:'not_found'}"));

        /* Mock http call. */
        ArgumentCaptor<ServiceCallback> httpCallback = ArgumentCaptor.forClass(ServiceCallback.class);
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), httpCallback.capture())).thenReturn(mock(ServiceCall.class));

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Set Distribute listener and customize it. */
        Distribute.setListener(actualListener);

        /* Resume activity. */
        Distribute.getInstance().onActivityResumed(mActivity);
        if (onPause) {
            Distribute.getInstance().onActivityPaused(mActivity);
        }

        /* Simulate network latency. */
        httpCallback.getValue().onCallFailed(httpException);

        /* Verify the right listener gets called. */
        verify(verificationListener, never()).onNoReleaseAvailable(any(Activity.class));
        verify(verificationListener, never()).onReleaseAvailable(any(Activity.class), any(ReleaseDetails.class));
    }

    @Test
    public void distributeListener() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);

        /* Start Distribute service. */
        restartProcessAndSdk();

        /* Resume with another activity. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify the default update dialog is built. */
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(false).thenReturn(true);
        Distribute.setListener(listener);

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the listener gets called. */
        verify(listener).onReleaseAvailable(mActivity, details);

        /* Verify the default update dialog is built. The count includes previous call. */
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the listener gets called. */
        verify(listener).onReleaseAvailable(mActivity, details);

        /* Verify the default update dialog is NOT built. The count includes previous call. */
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void handleUserUpdateActionNotProceededWithoutListener() throws Exception {

        /* Mock. */
        mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);
        Distribute.unsetInstance();
        Distribute distribute = spy(Distribute.getInstance());
        doNothing().when(distribute).completeWorkflow();

        /* Counters to verify multiple times for specific methods. */
        int appCenterLogErrorCounter = 0;
        int getStoredDownloadStateCounter = 0;

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Verify the method is called by onActivityCreated. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Disable the service. */
        distribute.setInstanceEnabled(false);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* mReleaseListener is null. */
        Mockito.when(ReleaseDownloaderFactory.create(any(Context.class), any(ReleaseDetails.class), any(ReleaseDownloadListener.class))).thenReturn(null);
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Enable the service. */
        ReleaseDownloader cleanupReleaseDownloader = mock(ReleaseDownloader.class);
        Mockito.when(ReleaseDownloaderFactory.create(any(Context.class), isNull(ReleaseDetails.class), any(ReleaseDownloadListener.class))).thenReturn(cleanupReleaseDownloader);
        distribute.setInstanceEnabled(true);

        /* Verify the method is called by resumeDistributeWorkflow. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Verify again to make sure the user action has NOT been processed yet. */
        verify(distribute, never()).completeWorkflow();
    }

    @Test
    public void handleUserUpdateActionNotProceededWithListener() throws Exception {

        /* Mock. */
        mockForCustomizationTest(false);
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(false);
        mockStatic(DistributeUtils.class);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());
        doNothing().when(distribute).completeWorkflow();

        /* Counters to verify multiple times for specific methods. */
        int appCenterLogErrorCounter = 0;
        int getStoredDownloadStateCounter = 0;

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Verify the method is called by onActivityCreated. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Disable the service. */
        distribute.setInstanceEnabled(false);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Enable the service. */
        distribute.setInstanceEnabled(true);

        /* Verify the method is called by resumeDistributeWorkflow. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++appCenterLogErrorCounter));
        AppCenterLog.error(anyString(), anyString());

        /* Verify again to make sure the user action has NOT been processed yet. */
        verify(distribute, never()).completeWorkflow();
    }

    @Test
    public void handleUserUpdateActionPostponeForOptionalUpdate() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify POSTPONE has been processed. */
        verify(distribute).completeWorkflow();
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
    }

    @Test
    public void handleUserUpdateActionErrorIsDownloading() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        when(mReleaseDownloader.isDownloading()).thenReturn(true);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify POSTPONE has not been processed. */
        verify(distribute, never()).completeWorkflow();
    }

    @Test
    public void handleUserUpdateActionErrorNotAvailable() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        when(mReleaseDownloader.isDownloading()).thenReturn(true);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.getStoredDownloadState()).thenReturn(-1);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify POSTPONE has not been processed. */
        verify(distribute, never()).completeWorkflow();
    }

    @Test
    public void handleUserUpdateActionDownloadForOptionalUpdate() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(eq(mContext))).thenReturn(true);
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has been processed. */
        verify(distribute).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
    }

    @Test
    public void doNotShowUnknownSourcesDialogWhileInBackground() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);
        distribute.onActivityPaused(mActivity);

        /* Call handleUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(eq(mContext))).thenReturn(false);
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has not been processed. */
        verify(distribute).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
        verify(mDialogBuilder, never()).create();
    }

    @Test
    public void doShowUnknownSourcesDialogWhileInForeground() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(eq(mContext))).thenReturn(false);
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has been processed. */
        verify(distribute).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
        verify(mDialogBuilder).create();
    }

    @Test
    public void handleUserUpdateActionPostponeForMandatoryUpdate() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(true);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify POSTPONE has NOT been processed. */
        verify(distribute, never()).completeWorkflow();
        verifyStatic(never());
        SharedPreferencesManager.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
    }

    @Test
    public void handleUserUpdateActionDownloadForMandatoryUpdate() throws Exception {

        /* Mock. */
        ReleaseDetails details = mockForCustomizationTest(true);
        mockStatic(DistributeUtils.class);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(eq(mContext))).thenReturn(true);
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has been processed. */
        verify(distribute).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
    }

    @Test
    @SuppressWarnings("ResourceType")
    public void handleUserUpdateActionInvalidUserAction() throws Exception {

        /* Mock. */
        mockForCustomizationTest(false);
        mockStatic(DistributeUtils.class);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Start Distribute service. */
        start();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Call handleUpdateAction with invalid user action. */
        int invalidUserAction = 10000;
        Distribute.notifyUpdateAction(invalidUserAction);

        /* Verify update has NOT been processed. */
        verifyStatic();
        AppCenterLog.error(anyString(), contains(String.valueOf(invalidUserAction)));
    }

    @Test
    public void notifyUserUpdateActionPostponeAndThenDownload() throws Exception {

        /* Mock. */
        mockForCustomizationTest(false);
        mockToGetRealDownloadState();

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());

        /* Start Distribute service. */
        start(distribute);
        distribute.onActivityResumed(mActivity);

        /* Call handleUpdateAction. */
        distribute.handleUpdateAction(UpdateAction.POSTPONE);

        /* Verify POSTPONE has been processed. */
        verify(distribute).completeWorkflow();

        /* Call handleUpdateAction again. */
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has NOT been processed. */
        verify(distribute, never()).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
    }

    private ReleaseDetails mockForCustomizationTest(boolean mandatory) throws Exception {

        /* Mock http call. */
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded(new HttpResponse(200, "mock"));
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(details.getShortVersion()).thenReturn("2.3.4");
        when(details.isMandatoryUpdate()).thenReturn(mandatory);
        when(details.getReleaseHash()).thenReturn("some_hash");
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock update token. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        return details;
    }

    private void mockHtpCallFailed(String payload) {
        final HttpException httpException = new HttpException(new HttpResponse(404, payload));

        /* Mock http call. */
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallFailed(httpException);
                return mock(ServiceCall.class);
            }
        });
    }

    private void mockToGetRealDownloadState() {

        /* Mock PreferenceStorage to simulate real download state. */
        final int[] currentDownloadState = {DOWNLOAD_STATE_COMPLETED};
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                currentDownloadState[0] = (Integer) invocation.getArguments()[1];
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                currentDownloadState[0] = DOWNLOAD_STATE_COMPLETED;
                return null;
            }
        }).when(SharedPreferencesManager.class);
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        when(SharedPreferencesManager.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt()))
                .thenAnswer(new Answer<Integer>() {

                    @Override
                    public Integer answer(InvocationOnMock invocation) {
                        return currentDownloadState[0];
                    }
                });
    }
}
