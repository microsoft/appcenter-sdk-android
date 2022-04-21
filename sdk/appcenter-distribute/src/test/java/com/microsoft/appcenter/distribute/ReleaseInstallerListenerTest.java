/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;

import com.microsoft.appcenter.utils.AppCenterLog;
import com.microsoft.appcenter.utils.HandlerUtils;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.NumberFormat;

@PrepareForTest({
        AppCenterLog.class,
        Distribute.class,
        FileInputStream.class,
        HandlerUtils.class,
        InstallerUtils.class,
        PackageInstaller.SessionCallback.class,
        ProgressDialog.class,
        ReleaseInstallerListener.class,
        Toast.class
})
public class ReleaseInstallerListenerTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

    @Mock
    private Context mContext;

    @Mock
    private Distribute mDistribute;

    @Mock
    private Toast mToast;

    @Mock
    private DownloadManager mDownloadManager;

    @Mock
    private android.app.ProgressDialog mMockProgressDialog;

    private final int mMockSessionId = 1;

    private ReleaseInstallerListener mReleaseInstallerListener;

    @Before
    public void setUp() throws Exception {

        /* Mock static classes. */
        mockStatic(AppCenterLog.class);
        mockStatic(Distribute.class);
        mockStatic(HandlerUtils.class);
        mockStatic(InstallerUtils.class);
        mockStatic(Toast.class);

        /* Mock progress dialog. */
        whenNew(android.app.ProgressDialog.class).withAnyArguments().thenReturn(mMockProgressDialog);
        when(mMockProgressDialog.isIndeterminate()).thenReturn(false);

        /* Mock toast. */
        when(mContext.getString(anyInt())).thenReturn("localized_message");
        when(Toast.makeText(any(Context.class), anyString(), anyInt())).thenReturn(mToast);

        /* Mock Distribute. */
        when(Distribute.getInstance()).thenReturn(mDistribute);
        doNothing().when(mDistribute).notifyInstallProgress(anyBoolean());

        /* Mock constructors and classes. */
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(mock(FileInputStream.class));

        /* Mock download manager. */
        when(mDownloadManager.openDownloadedFile(anyLong())).thenReturn(mock(ParcelFileDescriptor.class));
        when(mContext.getSystemService(anyString())).thenReturn(mDownloadManager);

        /* Create installer listener. */
        mReleaseInstallerListener = new ReleaseInstallerListener(mContext);

        /* Set downloadId. */
        mReleaseInstallerListener.setDownloadId(1);

        /* Init install progress dialog. */
        mReleaseInstallerListener.showInstallProgressDialog(mock(Activity.class));

        /* Verify call methods. */
        verify(mMockProgressDialog).setProgressPercentFormat(isNull());
        verify(mMockProgressDialog).setProgressNumberFormat(isNull());
        verify(mMockProgressDialog).setIndeterminate(anyBoolean());
    }

    @After
    public void cleanUp() {
        mReleaseInstallerListener = null;
    }

    @Test
    public void throwIOExceptionAfterStartInstall() throws Exception {

        /* Throw exception. */
        when(mDownloadManager.openDownloadedFile(anyLong())).thenThrow(new FileNotFoundException());

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that exception was called. */
        verifyStatic(AppCenterLog.class);
        AppCenterLog.error(anyString(), anyString(), any(FileNotFoundException.class));
    }

    @Test
    public void releaseInstallProcessWhenOnFinnishFailureWithContext() {

        /* Mock progress dialog. */
        when(mMockProgressDialog.isIndeterminate()).thenReturn(true);

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic(InstallerUtils.class);
        InstallerUtils.installPackage(any(InputStream.class), any(Context.class), sessionListener.capture());

        /* Emulate session status. */
        sessionListener.getValue().onCreated(mMockSessionId);

        /* Verify that installer process was triggered in the Distribute. */
        sessionListener.getValue().onActiveChanged(mMockSessionId, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Verify that progress dialog was closed after finish install process. */
        sessionListener.getValue().onFinished(mMockSessionId, false);

        /* Verify that the handler was called again. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mToast).show();
    }

    @Test
    public void releaseInstallerProcessWhenProgressDialogNull() {

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic(InstallerUtils.class);
        InstallerUtils.installPackage(any(InputStream.class), any(Context.class), sessionListener.capture());

        /* Emulate session status. */
        sessionListener.getValue().onCreated(mMockSessionId);
        sessionListener.getValue().onBadgingChanged(mMockSessionId);

        /* Verify that installer process was triggered in the Distribute. */
        sessionListener.getValue().onActiveChanged(mMockSessionId, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Hide dialog. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Verify that runnable was called. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog, never()).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process. */
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(3));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenDialogIsIndeterminate() {

        /* Mock progress dialog. */
        when(mMockProgressDialog.isIndeterminate()).thenReturn(true);

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic(InstallerUtils.class);
        InstallerUtils.installPackage(any(InputStream.class), any(Context.class), sessionListener.capture());

        /* Emulate session status. */
        sessionListener.getValue().onCreated(mMockSessionId);
        sessionListener.getValue().onBadgingChanged(mMockSessionId);

        /* Verify that installer process was triggered in the Distribute. */
        sessionListener.getValue().onActiveChanged(mMockSessionId, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog).setProgress(anyInt());
        verify(mMockProgressDialog).setMax(anyInt());
        verify(mMockProgressDialog).setProgressPercentFormat(any(NumberFormat.class));
        verify(mMockProgressDialog).setProgressNumberFormat(anyString());
        verify(mMockProgressDialog, times(2)).setIndeterminate(anyBoolean());

        /* Verify that progress dialog was closed after finish install process. */
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenWithContext() {

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic(InstallerUtils.class);
        InstallerUtils.installPackage(any(InputStream.class), any(Context.class), sessionListener.capture());

        /* Emulate session status. */
        sessionListener.getValue().onCreated(mMockSessionId);
        sessionListener.getValue().onBadgingChanged(mMockSessionId);

        /* Verify that installer process was triggered in the Distribute. */
        sessionListener.getValue().onActiveChanged(mMockSessionId, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process. */
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(HandlerUtils.class, times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void startInstallWhenFileIsInvalid() throws FileNotFoundException {

        /* Mock file description. */
        ParcelFileDescriptor mockFileDescriptor = mock(ParcelFileDescriptor.class);
        when(mockFileDescriptor.getStatSize()).thenReturn(0L);
        when(mDownloadManager.openDownloadedFile(anyLong())).thenReturn(mockFileDescriptor);

        /* Set total size. */
        mReleaseInstallerListener.setTotalSize(1L);

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that the install process never starts. */
        verifyStatic(InstallerUtils.class, never());
        InstallerUtils.installPackage(any(InputStream.class), any(Context.class), any(PackageInstaller.SessionCallback.class));
    }

    @Test
    public void releaseInstallerHideDialogTwice() {

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Hide dialog. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Try to hide dialog again. */
        mReleaseInstallerListener.hideInstallProgressDialog();

        /* Verify that runnable was called once only. */
        verifyStatic(HandlerUtils.class);
        HandlerUtils.runOnUiThread(any(Runnable.class));
    }
}
