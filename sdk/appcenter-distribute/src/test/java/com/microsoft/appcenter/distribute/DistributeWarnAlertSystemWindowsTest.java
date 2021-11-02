/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static com.microsoft.appcenter.distribute.DistributeConstants.LOG_TAG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doAnswer;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.microsoft.appcenter.utils.AppCenterLog;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@SuppressLint("InlinedApi")
@SuppressWarnings("CanBeFinal")
@PrepareForTest({
        Settings.class,
        AlertDialog.class,
        AlertDialog.Builder.class,
        ReleaseInstallerListener.class,
        AppCenterLog.class,
        DistributeUtils.class,
        Distribute.class,
        AppCenterLog.class,
        Build.class
})
@RunWith(PowerMockRunner.class)
public class DistributeWarnAlertSystemWindowsTest extends AbstractDistributeTest {

    @Mock
    private AlertDialog mAlertWindowsDialog;

    @Mock
    private Activity mFirstActivity;

    @Mock
    private ReleaseInstallerListener mReleaseInstallerListener;

    @Before
    public void setUpDialog() throws Exception {

        /* Mock static classes. */
        mockStatic(Settings.class);
        mockStatic(AppCenterLog.class);

        /* Reset mock installer utils. */
        when(InstallerUtils.isSystemAlertWindowsEnabled(any(Context.class))).thenCallRealMethod();

        /* Reset mock release listener methods. */
        doCallRealMethod().when(mReleaseDownloaderListener).onComplete(anyLong());
        when(mReleaseInstallerListener.showInstallProgressDialog(any(Activity.class))).thenReturn(mock(Dialog.class));

        /* Mock release installer listener. */
        whenNew(ReleaseInstallerListener.class).withAnyArguments().thenReturn(mReleaseInstallerListener);

        /* Mock release details. */
        mockStatic(DistributeUtils.class);
        when(DistributeUtils.loadCachedReleaseDetails()).thenReturn(mReleaseDetails);

        /* Start distribute from background. */
        ArgumentCaptor<ReleaseDownloadListener> releaseDownloadListener = ArgumentCaptor.forClass(ReleaseDownloadListener.class);
        Distribute.getInstance().startFromBackground(mContext);

        /* Start distribute module. */
        start();

        /* Resume distribute. */
        Distribute.getInstance().onActivityResumed(mFirstActivity);

        /* Mock system alert windows dialog. */
        Mockito.when(mDialogBuilder.create()).thenReturn(mAlertWindowsDialog);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                Mockito.when(mAlertWindowsDialog.isShowing()).thenReturn(true);
                return null;
            }
        }).when(mAlertWindowsDialog).show();
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) {
                Mockito.when(mAlertWindowsDialog.isShowing()).thenReturn(false);
                return null;
            }
        }).when(mAlertWindowsDialog).hide();

        /* Verify that dialog wasn't shown. */
        verify(mAlertWindowsDialog, never()).show();
    }

    @After
    public void tearDown() {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", 0);
    }

    private static void mockApiLevel(int apiLevel) {
        Whitebox.setInternalState(Build.VERSION.class, "SDK_INT", apiLevel);
    }

    @Test
    public void showAndEnableAlertWindowsDialogQ() {

        /* Mock permission state for Android Q. */
        mockApiLevel(Build.VERSION_CODES.Q);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(false);

        /* Notify about complete download. */
        mReleaseDownloaderListener.onComplete(1L);

        /* Verify that downloadId was set. */
        verify(mReleaseInstallerListener).setDownloadId(anyLong());

        /* Verify that dialog was shown after complete download. */
        verify(mAlertWindowsDialog).show();

        /* Emulate click on positive button. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setPositiveButton(eq(R.string.appcenter_distribute_unknown_sources_dialog_settings), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_POSITIVE);
        verify(mFirstActivity).startActivity(any(Intent.class));

        /* Emulate behavior that settings was enabled via dialog. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onApplicationEnterBackground();

        /* Verify that dialog was closed after call onActivityPaused. */
        verify(mReleaseInstallerListener).hideInstallProgressDialog();

        /* Mock settings result and resume Distribute. */
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(true);
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify that after enabling permissions the install process was started. */
        verify(mReleaseInstallerListener).startInstall();

        /* Emulate that install a new release was started. */
        Distribute.getInstance().notifyInstallProgress(true);
        verify(mReleaseInstallerListener, times(2)).hideInstallProgressDialog();

        /* Pause and resume Distribute again. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onApplicationEnterBackground();
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify that after resume do nothing. */
        verifyStatic();
        AppCenterLog.info(eq(LOG_TAG), eq("Installing in progress..."));
    }

    @Test
    public void showAndEnableAlertWindowsDialogLowQWithEnabledSettings() {
        showAndEnableAlertWindowsDialogLowQ(true);
    }

    @Test
    public void showAndEnableAlertWindowsDialogLowQWithDisabledSettings() {
        showAndEnableAlertWindowsDialogLowQ(false);
    }

    private void showAndEnableAlertWindowsDialogLowQ(boolean isEnabled) {

        /* Mock permission state for Android M. */
        mockApiLevel(Build.VERSION_CODES.M);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(isEnabled);

        /* Notify about complete download. */
        mReleaseDownloaderListener.onComplete(1L);

        /* Verify that downloadId was set. */
        verify(mReleaseInstallerListener).setDownloadId(anyLong());

        /* Verify that dialog was shown. */
        verify(mAlertWindowsDialog, never()).show();

        /* Emulate behavior that settings was enabled. */
        Distribute.getInstance().onActivityPaused(mActivity);
        Distribute.getInstance().onApplicationEnterBackground();

        /* Verify that dialog was closed. */
        verify(mReleaseInstallerListener).hideInstallProgressDialog();

        /* Do nothing with settings and resume Distribute module. */
        Distribute.getInstance().onApplicationEnterForeground();
        Distribute.getInstance().onActivityResumed(mActivity);

        /* Verify that after enabling permissions the install process was started. */
        verify(mReleaseInstallerListener).startInstall();

        /* Emulate that install a new release was started. */
        Distribute.getInstance().notifyInstallProgress(true);
        verify(mReleaseInstallerListener, times(2)).hideInstallProgressDialog();
    }

    @Test
    public void showAndDisableAlertWindowsDialogQ() {

        /* Mock permission state for Android Q. */
        mockApiLevel(Build.VERSION_CODES.Q);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(false);

        /* Notify about complete download. */
        mReleaseDownloaderListener.onComplete(1L);

        /* Verify that downloadId was set. */
        verify(mReleaseInstallerListener).setDownloadId(anyLong());

        /* Verify that dialog was shown after complete download. */
        verify(mAlertWindowsDialog).show();

        /* Emulate click on positive button. */
        ArgumentCaptor<DialogInterface.OnClickListener> clickListener = ArgumentCaptor.forClass(DialogInterface.OnClickListener.class);
        verify(mDialogBuilder).setNegativeButton(eq(android.R.string.cancel), clickListener.capture());
        clickListener.getValue().onClick(mDialog, DialogInterface.BUTTON_NEGATIVE);

        /* Verify that after enabling permissions the install process was started. */
        verify(mReleaseInstallerListener).startInstall();
    }

    @Test
    public void showAndCancelAlertWindowsDialog() {

        /* Mock permission state for Android Q. */
        mockApiLevel(Build.VERSION_CODES.Q);
        when(Settings.canDrawOverlays(any(Context.class))).thenReturn(false);

        /* Notify about complete download. */
        mReleaseDownloaderListener.onComplete(1L);

        /* Verify that downloadId was set. */
        verify(mReleaseInstallerListener).setDownloadId(anyLong());

        /* Verify that dialog was shown after complete download. */
        verify(mAlertWindowsDialog).show();

        /* Emulate click on positive button. */
        ArgumentCaptor<DialogInterface.OnCancelListener> cancelListener = ArgumentCaptor.forClass(DialogInterface.OnCancelListener.class);
        verify(mDialogBuilder).setOnCancelListener(cancelListener.capture());
        cancelListener.getValue().onCancel(mAlertWindowsDialog);

        /* Verify that after enabling permissions the install process was started. */
        verify(mReleaseInstallerListener).startInstall();
    }
}

