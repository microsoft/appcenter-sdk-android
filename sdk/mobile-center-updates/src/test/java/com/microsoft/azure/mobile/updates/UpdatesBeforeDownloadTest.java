package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.AsyncTaskUtils;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_URI;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_IGNORED_RELEASE_ID;
import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static com.microsoft.azure.mobile.utils.storage.StorageHelper.PreferencesStorage;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class UpdatesBeforeDownloadTest extends AbstractUpdatesTest {

    @Test
    public void failsToCompareVersion() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("com.contoso");
        PackageManager packageManager = mock(PackageManager.class);
        when(context.getPackageManager()).thenReturn(packageManager);
        when(packageManager.getPackageInfo("com.contoso", 0)).thenThrow(new PackageManager.NameNotFoundException());

        /* Trigger call. */
        Updates.getInstance().onStarted(context, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void olderVersionCode() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(5);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void sameVersionCode() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(6);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void moreRecentVersionWithoutReleaseNotesDialog() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setTitle(R.string.mobile_center_updates_update_dialog_title);
        verify(mDialogBuilder).setMessage(R.string.mobile_center_updates_update_dialog_message);
        verify(mDialogBuilder, never()).setMessage(any(CharSequence.class));
        verify(mDialogBuilder).create();
        verify(mDialog).show();

        /* After that if we resume app we refresh dialog. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

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
        Updates.setEnabled(false);

        /* We already called hide once, make sure its not called a second time. */
        verify(mDialog).hide();

        /* Also no toast if we don't click on actionable button. */
        verify(mToast, never()).show();
    }

    @Test
    public void moreRecentVersionWithReleaseNotesDialog() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getReleaseNotes()).thenReturn("mock");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setTitle(R.string.mobile_center_updates_update_dialog_title);
        verify(mDialogBuilder).setMessage("mock");
        verify(mDialogBuilder).create();
        verify(mDialog).show();
    }

    @Test
    public void dialogActivityStateChanges() throws Exception {

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
        headers.put(UpdateConstants.HEADER_API_TOKEN, "some token");
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.getReleaseNotes()).thenReturn("mock");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Activity activity = mock(Activity.class);
        Updates.getInstance().onActivityResumed(activity);
        Updates.getInstance().onActivityPaused(activity);
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Release call in background. */
        beforeSemaphore.release();
        afterSemaphore.acquireUninterruptibly();

        /* Verify dialog not shown. */
        verify(mDialogBuilder, never()).create();
        verify(mDialog, never()).show();

        /* Go foreground. */
        Updates.getInstance().onActivityResumed(activity);

        /* Verify dialog now shown. */
        verify(mDialogBuilder).create();
        verify(mDialog).show();

        /* Pause/resume should not alter dialog. */
        Updates.getInstance().onActivityPaused(activity);
        Updates.getInstance().onActivityResumed(activity);

        /* Only once check, and no hiding. */
        verify(mDialogBuilder).create();
        verify(mDialog).show();
        verify(mDialog, never()).hide();

        /* Cover activity. Dialog must be replaced. */
        Updates.getInstance().onActivityPaused(activity);
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialogBuilder, times(2)).create();
        verify(mDialog, times(2)).show();
        verify(mDialog).hide();
    }

    @Test
    public void cancelDialog() throws Exception {

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnCancelListener> cancelListener = ArgumentCaptor.forClass(DialogInterface.OnCancelListener.class);
        verify(mDialogBuilder).setOnCancelListener(cancelListener.capture());
        verify(mDialog).show();

        /* Cancel it. */
        cancelListener.getValue().onCancel(mDialog);
        when(mDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Restart should check release and show dialog again. */
        Updates.unsetInstance();
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(2)).show();
        verify(httpClient, times(2)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void postponeDialog() throws Exception {

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNeutralButton(eq(R.string.mobile_center_updates_update_dialog_postpone), clickListener.capture());
        verify(mDialog).show();

        /* Postpone it. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEUTRAL);
        when(mDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Restart should check release and show dialog again. */
        Updates.unsetInstance();
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog, times(2)).show();
        verify(httpClient, times(2)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void ignoreDialog() throws Exception {

        /* Mock ignore storage calls. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn((String) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putString(eq(PREFERENCE_KEY_IGNORED_RELEASE_ID), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_IGNORED_RELEASE_ID);

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(R.string.mobile_center_updates_update_dialog_ignore), clickListener.capture());
        verify(mDialog).show();

        /* Ignore it. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Restart app to check ignore. */
        Updates.unsetInstance();
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify second http call was made but dialog was skipped (e.g. shown only once). */
        verify(httpClient, times(2)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verify(mDialog).show();
        verifyStatic(times(2));
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Disable: it will prompt again as we clear storage. */
        Updates.setEnabled(false);
        Updates.setEnabled(true);
        verify(httpClient, times(3)).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verify(mDialog, times(2)).show();
    }

    @Test
    public void disableBeforeCancelDialog() throws Exception {

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnCancelListener> cancelListener = ArgumentCaptor.forClass(DialogInterface.OnCancelListener.class);
        verify(mDialogBuilder).setOnCancelListener(cancelListener.capture());
        verify(mDialog).show();

        /* Disable. */
        Updates.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Cancel it. */
        cancelListener.getValue().onCancel(mDialog);
        when(mDialog.isShowing()).thenReturn(false);

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
    }

    @Test
    public void disableBeforeIgnoreDialog() throws Exception {

        /* Mock ignore storage calls. */
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn((String) invocation.getArguments()[1]);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.putString(eq(PREFERENCE_KEY_IGNORED_RELEASE_ID), anyString());
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                when(PreferencesStorage.getString(invocation.getArguments()[0].toString())).thenReturn(null);
                return null;
            }
        }).when(PreferencesStorage.class);
        PreferencesStorage.remove(PREFERENCE_KEY_IGNORED_RELEASE_ID);

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(R.string.mobile_center_updates_update_dialog_ignore), clickListener.capture());
        verify(mDialog).show();

        /* Disable. */
        Updates.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Ignore it. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_IGNORED_RELEASE_ID);
        verifyStatic(never());
        PreferencesStorage.putString(eq(PREFERENCE_KEY_IGNORED_RELEASE_ID), anyString());
    }

    @Test
    @PrepareForTest(AsyncTaskUtils.class)
    public void disableBeforeDownload() throws Exception {

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
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn("someId");
        when(releaseDetails.getVersion()).thenReturn(7);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);
        mockStatic(AsyncTaskUtils.class);
        when(InstallerUtils.isUnknownSourcesEnabled(any(Context.class))).thenReturn(true);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));

        /* Verify dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.mobile_center_updates_update_dialog_download), clickListener.capture());
        verify(mDialog).show();

        /* Disable. */
        Updates.setEnabled(false);
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Click on download. */
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Since we were disabled, no action but toast to explain what happened. */
        verify(mToast).show();

        /* Verify no more calls, e.g. happened only once. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(httpClient).callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);

        /* Verify no download scheduled. */
        verifyStatic(never());
        AsyncTaskUtils.execute(anyString(), any(Updates.DownloadTask.class), Mockito.<Void>anyVararg());
    }
}
