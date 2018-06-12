package com.microsoft.appcenter.distribute;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.HttpClientNetworkStateHandler;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.AppNameHelper;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.async.AppCenterFuture;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_AVAILABLE;
import static com.microsoft.appcenter.distribute.DistributeConstants.DOWNLOAD_STATE_COMPLETED;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOADED_RELEASE_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_POSTPONE_TIME;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_RELEASE_DETAILS;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.appcenter.utils.storage.StorageHelper.PreferencesStorage;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@PrepareForTest(DistributeUtils.class)
public class DistributeBeforeDownloadTest extends AbstractDistributeTest {

    @Mock
    private AppCenterFuture<UUID> mAppCenterFuture;

    @Test
    public void moreRecentWithIncompatibleMinApiLevel() throws Exception {

        /* Mock we already have redirection parameters. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.JELLY_BEAN_MR2);
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getMinApiLevel()).thenReturn(Build.VERSION_CODES.KITKAT);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on incompatible version we complete workflow. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void olderVersionCode() throws Exception {

        /* Mock we already have public group, no token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(5);
        when(releaseDetails.getMinApiLevel()).thenReturn(Build.VERSION_CODES.M);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.N_MR1);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify release hash was not even considered. */
        //noinspection ResultOfMethodCallIgnored
        verify(releaseDetails, never()).getReleaseHash();
    }

    @Test
    public void sameVersionCodeSameHash() throws Exception {

        /* Mock we already have token and no group. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getReleaseHash()).thenReturn(TEST_HASH);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void moreRecentVersionCode() throws Exception {

        /* Mock we already have public group, no token. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getShortVersion()).thenReturn("7.0");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setTitle(R.string.appcenter_distribute_update_dialog_title);
        verify(mDialogBuilder).setMessage("unit-test-app7.07");
        verify(mDialogBuilder).create();
        verify(mDialog).show();

        /* After that if we resume app we refresh dialog. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* No more http call. */
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* But dialog refreshed. */
        InOrder order = inOrder(mDialog);
        order.verify(mDialog).hide();
        order.verify(mDialog).show();
        order.verifyNoMoreInteractions();
        verify(mDialog, times(2)).show();
        verify(mDialogBuilder, times(2)).create();

        /* Disable does not hide the dialog. */
        Distribute.setEnabled(false);

        /* We already called hide once, make sure its not called a second time. */
        verify(mDialog).hide();

        /* Also no toast if we don't click on actionable button. */
        verify(mToast, never()).show();
    }

    @Test
    public void sameVersionDifferentHashWithHardcodedAppName() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        HashMap<String, String> headers = new HashMap<>();
        headers.put(DistributeConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getShortVersion()).thenReturn("1.2.3");
        when(releaseDetails.getReleaseHash()).thenReturn("9f52199c986d9210842824df695900e1656180946212bd5e8978501a5b732e60");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Mock app name to be not localizable. */
        mockStatic(AppNameHelper.class);
        when(AppNameHelper.getAppName(mContext)).thenReturn("hardcoded-app-name");

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setTitle(R.string.appcenter_distribute_update_dialog_title);
        verify(mDialogBuilder).setMessage("hardcoded-app-name1.2.36");
        verify(mDialogBuilder).create();
        verify(mDialog).show();
    }

    @Test
    public void dialogActivityStateChanges() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final Semaphore beforeSemaphore = new Semaphore(0);
        final Semaphore afterSemaphore = new Semaphore(0);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(final InvocationOnMock invocation) {
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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getReleaseNotes()).thenReturn("mock");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Activity activity = mock(Activity.class);
        Distribute.getInstance().onActivityResumed(activity);
        Distribute.getInstance().onActivityPaused(activity);
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Release call in background. */
        beforeSemaphore.release();
        afterSemaphore.acquireUninterruptibly();

        /* Verify dialog not shown. */
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* Go foreground. */
        Distribute.getInstance().onActivityResumed(activity);

        /* Verify dialog now shown. */
        verify(mDialogBuilder).create();
        verify(mDialog).show();

        /* Pause/resume should not alter dialog. */
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityResumed(activity);

        /* Only once check, and no hiding. */
        verify(mDialogBuilder).create();
        verify(mDialog).show();
        verify(mDialog, never()).hide();

        /* Cover activity. Dialog must be replaced. */
        Distribute.getInstance().onActivityPaused(activity);
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, times(2)).create();
        verify(mDialog, times(2)).show();
        verify(mDialog).hide();
    }

    @Test
    public void postponeDialog() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(R.string.appcenter_distribute_update_dialog_postpone), clickListener.capture());
        verify(mDialog).show();

        /* Postpone it. */
        long now = 20122112L;
        mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(now);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(PreferencesStorage.getLong(invocation.getArguments()[0].toString(), 0)).thenReturn((Long) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_RELEASE_DETAILS);
        verifyStatic();
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), eq(now));

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Restart should check release and should not show dialog again until 1 day has elapsed. */
        now += DistributeConstants.POSTPONE_TIME_THRESHOLD - 1;
        when(System.currentTimeMillis()).thenReturn(now);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();

        /* Now its time to show again. */
        now += 1;
        when(System.currentTimeMillis()).thenReturn(now);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(2)).show();

        /* Postpone again. */
        clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setNegativeButton(eq(R.string.appcenter_distribute_update_dialog_postpone), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);

        /* Check postpone again. */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(2)).show();

        /* If mandatory release, we ignore postpone and still show dialog. */
        when(releaseDetails.isMandatoryUpdate()).thenReturn(true);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(3)).show();

        /* Set back in time to make SDK clean state and force update. */
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_POSTPONE_TIME);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(false);
        now = 1;
        when(System.currentTimeMillis()).thenReturn(now);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(4)).show();
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_POSTPONE_TIME);
    }

    @Test
    public void disableBeforePostponeDialog() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(R.string.appcenter_distribute_update_dialog_postpone), clickListener.capture());
        verify(mDialog).show();

        /* Disable. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Postpone it. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        verifyStatic(never());
        PreferencesStorage.putLong(eq(PREFERENCE_KEY_POSTPONE_TIME), anyLong());
    }

    @Test
    @PrepareForTest(AsyncTaskUtils.class)
    public void disableBeforeDownload() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        mockStatic(AsyncTaskUtils.class);
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        verify(mDialog).show();

        /* Disable. */
        Distribute.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Click on download. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no download scheduled. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), any(DownloadTask.class), Mockito.<Void>anyVararg());
    }

    @Test
    public void mandatoryUpdateDialogAndCacheTests() throws Exception {

        /* Mock some storage calls. */
        mockSomeStorage();

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final AtomicReference<ServiceCallback> serviceCallbackRef = new AtomicReference<>();
        final ServiceCall serviceCall = mock(ServiceCall.class);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                Object serviceCallback = invocation.getArguments()[4];
                if (serviceCallback instanceof ServiceCallback) {
                    serviceCallbackRef.set((ServiceCallback) serviceCallback);
                }
                return serviceCall;
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(true);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        assertNotNull(serviceCallbackRef.get());
        serviceCallbackRef.get().onCallSucceeded("mock");
        serviceCallbackRef.set(null);

        /* Verify release notes persisted. */
        verifyStatic();
        PreferencesStorage.putString(PREFERENCE_KEY_RELEASE_DETAILS, "mock");
        verifyStatic();
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);

        /* Verify dialog. */
        verify(mDialogBuilder, never()).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Verify dialog restored offline even if process restarts. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));
        verify(mDialogBuilder, never()).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));
        verify(httpClient, times(2)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        assertNotNull(serviceCallbackRef.get());

        /* Simulate network back and get same release again, should do nothing particular. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        serviceCallbackRef.get().onCallSucceeded("mock");

        /* Check we didn't change state, e.g. happened only once. */
        verifyStatic();
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);

        /* Restart and this time we will detect a more recent optional release. */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify call is made and that we restored again mandatory update dialog in the mean time. */
        verify(httpClient, times(3)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verify(mDialogBuilder, times(3)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));
        verify(mDialogBuilder, never()).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));

        /* Then detect new release in background. */
        releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(5);
        when(releaseDetails.getVersion()).thenReturn(8);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(false);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        serviceCallbackRef.get().onCallSucceeded("mock");

        /* Check state updated again when we detect it. */
        verifyStatic(times(2));
        PreferencesStorage.putInt(PREFERENCE_KEY_DOWNLOAD_STATE, DOWNLOAD_STATE_AVAILABLE);

        /* Restart SDK, even offline, should show optional dialog. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, times(4)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));
        verify(mDialogBuilder).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));

        /* And still check again for further update. */
        verify(httpClient, times(4)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Unblock call with network up. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(true);
        serviceCallbackRef.get().onCallSucceeded("mock");

        /* If we restart SDK online, its an optional update so dialog will not be restored until new call made. */
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog behavior happened only once. */
        verify(mDialogBuilder).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));

        /* Dialog shown only after new call made in that scenario. */
        serviceCallbackRef.get().onCallSucceeded("mock");
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(5)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        verify(mDialogBuilder, times(2)).setNegativeButton(anyInt(), any(DialogInterface.OnClickListener.class));

        /* If we finally click on download, no call cancel since already successful. */
        when(InstallerUtils.isUnknownSourcesEnabled(mContext)).thenReturn(true);
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verify(serviceCall, never()).cancel();
    }

    @Test
    public void cancelGetReleaseCallIfDownloadingCachedDialogAfterRestart() throws Exception {

        /* Mock some storage calls. */
        mockSomeStorage();

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        final AtomicReference<ServiceCallback> serviceCallbackRef = new AtomicReference<>();
        final ServiceCall serviceCall = mock(ServiceCall.class);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                Object serviceCallback = invocation.getArguments()[4];
                if (serviceCallback instanceof ServiceCallback) {
                    serviceCallbackRef.set((ServiceCallback) serviceCallback);
                }
                return serviceCall;
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(false);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        serviceCallbackRef.get().onCallSucceeded("mock");
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), any(DialogInterface.OnClickListener.class));

        /* Restart offline. */
        when(mNetworkStateHelper.isNetworkConnected()).thenReturn(false);
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog restored and call scheduled. */
        verify(httpClient, times(2)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());

        /* We are offline and call is scheduled, clicking download must cancel pending call. */
        when(InstallerUtils.isUnknownSourcesEnabled(mContext)).thenReturn(true);
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verify(serviceCall).cancel();
    }

    @Test
    public void releaseNotes() throws Exception {

        /* Mock we already have redirection parameters. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });

        /* No release notes. */
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        verify(mDialogBuilder, never()).setNeutralButton(eq(R.string.appcenter_distribute_update_dialog_view_release_notes), any(DialogInterface.OnClickListener.class));
        verify(mDialog).show();
        reset(mDialog);

        /* Release notes but somehow no URL. */
        when(releaseDetails.getReleaseNotes()).thenReturn("Fix a bug");
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, never()).setNeutralButton(eq(R.string.appcenter_distribute_update_dialog_view_release_notes), any(DialogInterface.OnClickListener.class));
        verify(mDialog).show();
        reset(mDialog);

        /* Release notes URL this time. */
        final Uri uri = mock(Uri.class);
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Intent.ACTION_VIEW, uri).thenReturn(intent);
        when(releaseDetails.getReleaseNotesUrl()).thenReturn(uri);

        /* Empty release notes and URL. */
        when(releaseDetails.getReleaseNotes()).thenReturn("");
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, never()).setNeutralButton(eq(R.string.appcenter_distribute_update_dialog_view_release_notes), any(DialogInterface.OnClickListener.class));
        verify(mDialog).show();
        reset(mDialog);

        /* Release notes and URL. */
        when(releaseDetails.getReleaseNotes()).thenReturn("Fix a bug");
        restartProcessAndSdk();
        Distribute.getInstance().onActivityResumed(mActivity);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNeutralButton(eq(R.string.appcenter_distribute_update_dialog_view_release_notes), clickListener.capture());
        verify(mDialog).show();
        reset(mDialog);

        /* Click and check navigation. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEUTRAL);
        verify(mActivity).startActivity(intent);

        /* We thus leave app. */
        Distribute.getInstance().onActivityPaused(mActivity);
        when(mDialog.isShowing()).thenReturn(false);

        /* Going back should restore dialog. */
        Distribute.getInstance().onActivityResumed(mActivity);
        clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder, times(2)).setNeutralButton(eq(R.string.appcenter_distribute_update_dialog_view_release_notes), clickListener.capture());
        verify(mDialog).show();

        /* Do the same test and simulate failed navigation. */
        mockStatic(AppCenterLog.class);
        ActivityNotFoundException exception = new ActivityNotFoundException();
        doThrow(exception).when(mActivity).startActivity(intent);
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEUTRAL);
        verify(mActivity, times(2)).startActivity(intent);
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), eq(exception));
    }

    @Test
    public void shouldRemoveReleaseHashStorageIfReportedSuccessfully() throws Exception {

        /* Mock release hash storage. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-hash");

        /* Mock install id from AppCenter. */
        UUID installId = UUID.randomUUID();
        when(mAppCenterFuture.get()).thenReturn(installId);
        when(AppCenter.getInstallId()).thenReturn(mAppCenterFuture);

        /* Mock we already have token and no group. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getReleaseHash()).thenReturn(TEST_HASH);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID);
    }

    @Test
    public void shouldNotRemoveReleaseHashStorageIfHashesDontMatch() throws Exception {

        /* Mock release hash storage. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH)).thenReturn("fake-hash");
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.computeReleaseHash(any(PackageInfo.class))).thenReturn("fake-old-hash");

        /* Mock install id from AppCenter. */
        UUID installId = UUID.randomUUID();
        when(mAppCenterFuture.get()).thenReturn(installId);
        when(AppCenter.getInstallId()).thenReturn(mAppCenterFuture);

        /* Mock we already have token and no group. */
        when(PreferencesStorage.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        HttpClientNetworkStateHandler httpClient = mock(HttpClientNetworkStateHandler.class);
        whenNew(HttpClientNetworkStateHandler.class).withAnyArguments().thenReturn(httpClient);
        when(httpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock");
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getReleaseHash()).thenReturn(TEST_HASH);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mock(Activity.class));

        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_HASH);
        verifyStatic(never());
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOADED_RELEASE_ID);
    }

    /**
     * Mock some storage calls.
     */
    private void mockSomeStorage() {
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                PowerMockito.when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn((Integer) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putInt(eq(PREFERENCE_KEY_DOWNLOAD_STATE), anyInt());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                PowerMockito.when(PreferencesStorage.getInt(invocation.getArguments()[0].toString(), DOWNLOAD_STATE_COMPLETED)).thenReturn(DOWNLOAD_STATE_COMPLETED);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_STATE);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                PowerMockito.when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(invocation.getArguments()[1].toString());
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putString(eq(PREFERENCE_KEY_RELEASE_DETAILS), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                PowerMockito.when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_RELEASE_DETAILS);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }
}
