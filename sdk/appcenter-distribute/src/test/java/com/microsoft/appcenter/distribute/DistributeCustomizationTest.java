package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.DialogInterface;

import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.storage.StorageHelper;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

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
import static org.powermock.api.mockito.PowerMockito.whenNew;

@SuppressWarnings("unused")
@PrepareForTest(DistributeUtils.class)
public class DistributeCustomizationTest extends AbstractDistributeTest {

    private void start(Distribute distribute) {
        distribute.onStarting(mAppCenterHandler);
        distribute.onStarted(mContext, "a", null, mock(Channel.class));
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

        /* Verify the user action has NOT been processed. */
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
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
        verifyStatic(times(++getStoredDownloadStateCounter));
        DistributeUtils.getStoredDownloadState();
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
        StorageHelper.PreferencesStorage.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
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
        when(InstallerUtils.isUnknownSourcesEnabled(mActivity)).thenReturn(true);
        distribute.handleUpdateAction(UpdateAction.UPDATE);

        /* Verify UPDATE has been processed. */
        verify(distribute).enqueueDownloadOrShowUnknownSourcesDialog(any(ReleaseDetails.class));
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
        StorageHelper.PreferencesStorage.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
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
        when(InstallerUtils.isUnknownSourcesEnabled(mActivity)).thenReturn(true);
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
        when(details.getShortVersion()).thenReturn("2.3.4");
        when(details.isMandatoryUpdate()).thenReturn(mandatory);
        when(ReleaseDetails.parse(anyString())).thenReturn(details);

        /* Mock update token. */
        when(StorageHelper.PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        return details;
    }

    private void mockToGetRealDownloadState() {

        /* Mock PreferenceStorage to simulate real download state. */
        final int[] currentDownloadState = {DOWNLOAD_STATE_COMPLETED};
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                currentDownloadState[0] = (Integer) invocation.getArguments()[1];
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                currentDownloadState[0] = DOWNLOAD_STATE_COMPLETED;
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        when(StorageHelper.PreferencesStorage.getInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt()))
                .thenAnswer(new Answer<Integer>() {

                    @Override
                    public Integer answer(InvocationOnMock invocation) throws Throwable {
                        return currentDownloadState[0];
                    }
                });
    }
}
