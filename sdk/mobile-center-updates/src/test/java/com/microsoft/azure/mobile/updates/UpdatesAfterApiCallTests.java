package com.microsoft.azure.mobile.updates;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import com.microsoft.azure.mobile.channel.Channel;
import com.microsoft.azure.mobile.http.HttpClient;
import com.microsoft.azure.mobile.http.HttpClientNetworkStateHandler;
import com.microsoft.azure.mobile.http.ServiceCall;
import com.microsoft.azure.mobile.http.ServiceCallback;
import com.microsoft.azure.mobile.utils.HashUtils;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.HashMap;

import static com.microsoft.azure.mobile.updates.UpdateConstants.PREFERENCE_KEY_DOWNLOAD_URI;
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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

public class UpdatesAfterApiCallTests extends AbstractUpdatesTest {

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
        when(ReleaseDetails.parse(anyString())).thenReturn(mock(ReleaseDetails.class));
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
        when(ReleaseDetails.parse(anyString())).thenReturn(mock(ReleaseDetails.class)); // 0 vs 6

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void sameVersionCodeAndSameHash() throws Exception {

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
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getFingerprint()).thenReturn(HashUtils.sha256("com.contoso:1.2.3:6"));
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify on failure we complete workflow. */
        verifyStatic();
        PreferencesStorage.remove(PREFERENCE_KEY_DOWNLOAD_URI);
        verify(mDialog, never()).show();

        /* After that if we resume app nothing happens. */
        Updates.getInstance().onActivityPaused(mock(Activity.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));
    }

    @Test
    public void moreRecentHashNoReleaseNotesDialog() throws Exception {

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
        when(releaseDetails.getVersion()).thenReturn(6);
        when(releaseDetails.getFingerprint()).thenReturn("mock");
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        Updates.getInstance().onStarted(mContext, "a", mock(Channel.class));
        Updates.getInstance().onActivityResumed(mock(Activity.class));
        verify(httpClient).callAsync(anyString(), anyString(), eq(headers), any(HttpClient.CallTemplate.class), any(ServiceCallback.class));

        /* Verify dialog. */
        verify(mDialogBuilder).setTitle(R.string.mobile_center_updates_update_dialog_title);
        verify(mDialogBuilder).setMessage(R.string.mobile_center_updates_update_dialog_message);
        verify(mDialogBuilder, never()).setMessage(any(CharSequence.class));
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
        verifyNoMoreInteractions(mDialog);
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
        verify(mDialog).show();
    }
}
