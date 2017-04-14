package com.microsoft.azure.mobile.distribute;

import android.app.Activity;
import android.content.DialogInterface;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;
import com.microsoft.azure.mobile.utils.MobileCenterLog;
import com.microsoft.azure.mobile.utils.storage.StorageHelper;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import static com.microsoft.azure.mobile.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_IGNORED_RELEASE_ID;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.azure.mobile.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest({DistributeUtils.class, AsyncTaskUtils.class})
public class DistributeCustomizationTest extends AbstractDistributeTest {

    @Test
    public void distributeListener() throws Exception {

        /* Mock http call. */
        PowerMockito.when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Start Distribute service. */
        Distribute.unsetInstance();
        Distribute.getInstance().onStarted(mActivity, "", mock(Channel.class));

        /* Resume with another activity. */
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify the default update dialog is built. */
        verify(mDialogBuilder).setPositiveButton(eq(R.string.mobile_center_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Set Distribute listener and customize it. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onNewReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(false).thenReturn(true);
        Distribute.setListener(listener);

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the listener gets called. */
        verify(listener).onNewReleaseAvailable(mActivity, details);

        /* Verify the default update dialog is built. The count includes previous call. */
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.mobile_center_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Resume activity again to invoke update request. */
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify the listener gets called. */
        verify(listener).onNewReleaseAvailable(mActivity, details);

        /* Verify the default update dialog is NOT built. The count includes previous call. */
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.mobile_center_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));
    }

    @Test
    public void handleUserUpdateActionNotProceeded() throws Exception {

        /* Mock http call. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock others. */
        mockStatic(DistributeUtils.class);
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        Distribute.unsetInstance();
        Distribute distribute = spy(Distribute.getInstance());
        doNothing().when(distribute).completeWorkflow();

        /* Counters to verify multiple times for specific methods. */
        int mobileCenterLogErrorCounter = 0;
        int getStoredDownloadStateCounter = 0;

        /* Start Distribute service. */
        distribute.onStarted(mActivity, "", mock(Channel.class));
        distribute.onActivityResumed(mActivity);

        /* Verify the method is called by onActivityCreated. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Disable the service. */
        distribute.setInstanceEnabled(false);

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.POSTPONE);

        /* Verify the user action is not processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++mobileCenterLogErrorCounter));
        MobileCenterLog.error(anyString(), anyString());

        /* Enable the service. */
        distribute.setInstanceEnabled(true);

        /* Verify the method is called by resumeDistributeWorkflow. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.POSTPONE);

        /* Verify the user action is not processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++mobileCenterLogErrorCounter));
        MobileCenterLog.error(anyString(), anyString());

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.POSTPONE);

        /* Verify the user action is not processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
        verifyStatic(times(++mobileCenterLogErrorCounter));
        MobileCenterLog.error(anyString(), anyString());

        /* Verify again to make sure the user action has not been processed yet. */
        verify(distribute, never()).completeWorkflow();
    }

    @Test
    public void handleUserUpdateActionProceededForOptionalUpdate() throws Exception {

        /* Mock http call. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock others. */
        mockStatic(DistributeUtils.class);
        mockStatic(AsyncTaskUtils.class);
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onNewReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());
        doNothing().when(distribute).completeWorkflow();

        /* Start Distribute service. */
        distribute.onStarted(mActivity, "", mock(Channel.class));
        distribute.onActivityResumed(mActivity);

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.POSTPONE);

        /* Verify POSTPONE has been processed. */
        verify(distribute).completeWorkflow();

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.IGNORE);

        /* Verify IGNORE has been processed. */
        verifyStatic();
        StorageHelper.PreferencesStorage.putInt(PREFERENCE_KEY_IGNORED_RELEASE_ID, details.getId());

        /* Call handleUserUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(mActivity)).thenReturn(true);
        distribute.handleUserUpdateAction(UserUpdateAction.DOWNLOAD);

        /* Verify DOWNLOAD has been processed. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), any(DownloadTask.class));
    }

    @Test
    public void handleUserUpdateActionProceededForMandatoryUpdate() throws Exception {

        /* Mock http call. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(details.isMandatoryUpdate()).thenReturn(true);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock others. */
        mockStatic(DistributeUtils.class);
        mockStatic(AsyncTaskUtils.class);
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onNewReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);
        Distribute distribute = spy(Distribute.getInstance());
        doNothing().when(distribute).completeWorkflow();

        /* Start Distribute service. */
        distribute.onStarted(mActivity, "", mock(Channel.class));
        distribute.onActivityResumed(mActivity);

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.POSTPONE);

        /* Verify POSTPONE has not been processed. */
        verify(distribute, never()).completeWorkflow();

        /* Call handleUserUpdateAction. */
        distribute.handleUserUpdateAction(UserUpdateAction.IGNORE);

        /* Verify IGNORE has not been processed. */
        verifyStatic(never());
        StorageHelper.PreferencesStorage.putInt(PREFERENCE_KEY_IGNORED_RELEASE_ID, details.getId());

        /* Call handleUserUpdateAction. */
        when(InstallerUtils.isUnknownSourcesEnabled(mActivity)).thenReturn(true);
        distribute.handleUserUpdateAction(UserUpdateAction.DOWNLOAD);

        /* Verify DOWNLOAD has been processed. */
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), any(DownloadTask.class));
    }


    @Test
    public void handleUserUpdateActionInvalidUserAction() throws Exception {

        /* Mock http call. */
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) throws Throwable {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* Mock data model. */
        mockStatic(ReleaseDetails.class);
        ReleaseDetails details = mock(ReleaseDetails.class);
        when(details.getId()).thenReturn(1);
        when(details.getVersion()).thenReturn(10);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock others. */
        mockStatic(DistributeUtils.class);
        mockStatic(AsyncTaskUtils.class);
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");

        /* Set Distribute listener so that Distribute doesn't use default update dialog. */
        DistributeListener listener = mock(DistributeListener.class);
        when(listener.onNewReleaseAvailable(eq(mActivity), any(ReleaseDetails.class))).thenReturn(true);
        Distribute.unsetInstance();
        Distribute.setListener(listener);

        /* Mock the download state to DOWNLOAD_STATE_AVAILABLE. */
        when(DistributeUtils.getStoredDownloadState()).thenReturn(DOWNLOAD_STATE_AVAILABLE);

        /* Start Distribute service. */
        Distribute.getInstance().onStarted(mActivity, "", mock(Channel.class));
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Call handleUserUpdateAction with invalid user action. */
        int invalidUserAction = 10000;
        Distribute.notifyUserUpdateAction(invalidUserAction);

        /* Verify update has not been processed. */
        verifyStatic();
        MobileCenterLog.error(anyString(), contains(String.valueOf(invalidUserAction)));
    }
}
