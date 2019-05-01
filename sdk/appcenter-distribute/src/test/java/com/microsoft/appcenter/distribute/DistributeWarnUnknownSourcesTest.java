/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.Settings;

import com.microsoft.appcenter.http.HttpClient;
import com.microsoft.appcenter.http.ServiceCall;
import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.test.TestUtils;
import com.microsoft.appcenter.utils.AsyncTaskUtils;
import com.microsoft.appcenter.utils.storage.SharedPreferencesManager;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.util.Arrays;
import java.util.Collection;

import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DISTRIBUTION_GROUP_ID;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_DOWNLOAD_STATE;
import static com.microsoft.appcenter.distribute.DistributeConstants.PREFERENCE_KEY_UPDATE_TOKEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
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

@SuppressWarnings("CanBeFinal")
@RunWith(Parameterized.class)
public class DistributeWarnUnknownSourcesTest extends AbstractDistributeTest {

    @Parameterized.Parameter
    public boolean mMandatoryUpdate;

    @Mock
    private AlertDialog mUnknownSourcesDialog;

    @Mock
    private Activity mFirstActivity;

    @Parameterized.Parameters(name = "mandatory_update={0}")
    public static Collection<Boolean> data() {
        return Arrays.asList(false, true);
    }

    @Before
    public void setUpDialog() throws Exception {

        /* Mock we already have redirection parameters. */
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_DISTRIBUTION_GROUP_ID)).thenReturn("some group");
        when(SharedPreferencesManager.getString(PREFERENCE_KEY_UPDATE_TOKEN)).thenReturn("some token");
        when(mHttpClient.callAsync(anyString(), anyString(), anyMapOf(String.class, String.class), any(HttpClient.CallTemplate.class), any(ServiceCallback.class))).thenAnswer(new Answer<ServiceCall>() {

            @Override
            public ServiceCall answer(InvocationOnMock invocation) {
                ((ServiceCallback) invocation.getArguments()[4]).onCallSucceeded("mock", null);
                return mock(ServiceCall.class);
            }
        });
        ReleaseDetails releaseDetails = mock(ReleaseDetails.class);
        when(releaseDetails.getId()).thenReturn(4);
        when(releaseDetails.getVersion()).thenReturn(7);
        when(releaseDetails.isMandatoryUpdate()).thenReturn(mMandatoryUpdate);
        when(ReleaseDetails.parse(anyString())).thenReturn(releaseDetails);

        /* Trigger call. */
        start();
        Distribute.getInstance().onActivityResumed(mFirstActivity);

        /* Mock second dialog. */
        when(mDialogBuilder.create()).thenReturn(mUnknownSourcesDialog);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(mUnknownSourcesDialog.isShowing()).thenReturn(true);
                return null;
            }
        }).when(mUnknownSourcesDialog).show();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                when(mUnknownSourcesDialog.isShowing()).thenReturn(false);
                return null;
            }
        }).when(mUnknownSourcesDialog).hide();

        /* Click on first dialog. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_update_dialog_download), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        when(mDialog.isShowing()).thenReturn(false);

        /* Second should show. */
        verify(mUnknownSourcesDialog).show();
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    @Test
    public void cancelDialogWithBack() {

        /* Mandatory update cannot be canceled. */
        if (mMandatoryUpdate) {

            /* 1 for update dialog, 1 for unknown sources dialog. */
            verify(mDialogBuilder, times(2)).setCancelable(false);
            return;
        }

        /* Cancel. */
        ArgumentCaptor<DialogInterface.OnCancelListener> cancelListener = ArgumentCaptor.forClass(DialogInterface.OnCancelListener.class);
        verify(mDialogBuilder).setOnCancelListener(cancelListener.capture());
        cancelListener.getValue().onCancel(mUnknownSourcesDialog);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }

    @Test
    public void cancelDialogWithButton() {

        /* Mandatory update cannot be canceled. */
        Assume.assumeFalse(mMandatoryUpdate);

        /* Cancel. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(android.R.string.cancel), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }

    @Test
    public void disableBeforeCancelWithBack() {

        /* Mandatory update cannot be canceled. */
        Assume.assumeFalse(mMandatoryUpdate);

        /* Disable. */
        Distribute.setEnabled(false);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Cancel. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(android.R.string.cancel), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_NEGATIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify cancel did nothing more. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }

    @Test
    public void disableBeforeCancelWithButton() {

        /* Mandatory update cannot be canceled. */
        Assume.assumeFalse(mMandatoryUpdate);

        /* Disable. */
        Distribute.setEnabled(false);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Cancel. */
        ArgumentCaptor<DialogInterface.OnCancelListener> cancelListener = ArgumentCaptor.forClass(DialogInterface.OnCancelListener.class);
        verify(mDialogBuilder).setOnCancelListener(cancelListener.capture());
        cancelListener.getValue().onCancel(mUnknownSourcesDialog);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify cancel did nothing more. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }

    @Test
    public void coverActivity() {

        /* Pause/resume should not alter dialog. */
        Distribute.getInstance().onActivityPaused(mFirstActivity);
        Distribute.getInstance().onActivityResumed(mFirstActivity);

        /* Only once check, and no hiding. */
        verify(mDialog).show();
        verify(mDialog, never()).hide();
        verify(mUnknownSourcesDialog).show();
        verify(mUnknownSourcesDialog, never()).hide();

        /* Cover activity. Second dialog must be replaced. First one skipped. */
        Distribute.getInstance().onActivityPaused(mFirstActivity);
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mDialog, never()).hide();
        verify(mUnknownSourcesDialog, times(2)).show();
        verify(mUnknownSourcesDialog).hide();
    }

    @Test
    public void clickSettingsGoBackWithoutEnabling() throws Exception {

        /* Click settings. */
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_SECURITY_SETTINGS).thenReturn(intent);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify navigation. */
        verify(mFirstActivity).startActivity(intent);

        /* Simulate we go back and forth to settings without changing the value. */
        Distribute.getInstance().onActivityPaused(mFirstActivity);
        Distribute.getInstance().onActivityResumed(mFirstActivity);

        /* Second dialog will be back directly, no update dialog again. */
        verify(mDialog).show();
        verify(mDialog, never()).hide();
        verify(mUnknownSourcesDialog, times(2)).show();
        verify(mUnknownSourcesDialog, never()).hide();
    }

    @Test
    @PrepareForTest(Build.class)
    @SuppressLint("InlinedApi")
    public void clickSettingsOnAndroidO() throws Exception {

        /* Click settings. */
        Intent intentSecuritySettings = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_SECURITY_SETTINGS).thenReturn(intentSecuritySettings);
        Intent intentManageUnknownAppSources = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).thenReturn(intentManageUnknownAppSources);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());

        /* Verify behaviour on old version. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", BuildConfig.MIN_SDK_VERSION);
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        verify(mFirstActivity).startActivity(intentSecuritySettings);
        verify(mFirstActivity, never()).startActivity(intentManageUnknownAppSources);
        reset(mFirstActivity);

        /* Verify behaviour on Android O. */
        TestUtils.setInternalState(Build.VERSION.class, "SDK_INT", Build.VERSION_CODES.O);
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        verify(mFirstActivity, never()).startActivity(intentSecuritySettings);
        verify(mFirstActivity).startActivity(intentManageUnknownAppSources);
    }

    @Test
    @PrepareForTest(AsyncTaskUtils.class)
    public void clickSettingsThenEnableThenBack() throws Exception {

        /* Click settings. */
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_SECURITY_SETTINGS).thenReturn(intent);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify navigation. */
        verify(mFirstActivity).startActivity(intent);

        /* Simulate we go to settings, change value then go back. */
        mockStatic(AsyncTaskUtils.class);
        Distribute.getInstance().onActivityPaused(mFirstActivity);
        when(InstallerUtils.isUnknownSourcesEnabled(mContext)).thenReturn(true);
        Distribute.getInstance().onActivityResumed(mFirstActivity);

        /* No more dialog, start download. */
        verify(mDialog).show();
        verify(mDialog, never()).hide();
        verify(mUnknownSourcesDialog).show();
        verify(mUnknownSourcesDialog, never()).hide();
        verifyStatic();
        AsyncTaskUtils.execute(anyString(), argThat(new ArgumentMatcher<AsyncTask<Object, ?, ?>>() {

            @Override
            public boolean matches(Object argument) {
                return argument instanceof DownloadTask;
            }
        }), anyVararg());
    }

    @Test
    public void clickSettingsFailsToNavigate() throws Exception {

        /* Click settings. */
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_SECURITY_SETTINGS).thenReturn(intent);
        doThrow(new ActivityNotFoundException()).when(mFirstActivity).startActivity(intent);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify navigation attempted. */
        verify(mFirstActivity).startActivity(intent);

        /* Verify failure is treated as a cancel dialog. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }

    @Test
    public void disableThenClickSettingsThenFailsToNavigate() throws Exception {

        /* Disable. */
        Distribute.setEnabled(false);
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Click settings. */
        Intent intent = mock(Intent.class);
        whenNew(Intent.class).withArguments(Settings.ACTION_SECURITY_SETTINGS).thenReturn(intent);
        doThrow(new ActivityNotFoundException()).when(mFirstActivity).startActivity(intent);
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());
        clickListener.getValue().onClick(mUnknownSourcesDialog, DialogInterface.BUTTON_POSITIVE);
        when(mUnknownSourcesDialog.isShowing()).thenReturn(false);

        /* Verify navigation attempted. */
        verify(mFirstActivity).startActivity(intent);

        /* Verify cleaning behavior happened only once, e.g. completeWorkflow skipped. */
        verifyStatic();
        SharedPreferencesManager.remove(PREFERENCE_KEY_DOWNLOAD_STATE);

        /* Verify no more calls, e.g. happened only once. */
        Distribute.getInstance().onActivityPaused(mock(Activity.class));
        Distribute.getInstance().onActivityResumed(mock(Activity.class));
        verify(mDialog).show();
        verify(mUnknownSourcesDialog).show();
    }
}
