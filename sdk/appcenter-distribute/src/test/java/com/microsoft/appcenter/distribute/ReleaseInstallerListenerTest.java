/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License.
 */

package com.microsoft.appcenter.distribute;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import org.mockito.Matchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;

@PrepareForTest({
        ProgressDialog.class,
        InstallerUtils.class,
        FileInputStream.class,
        PackageInstaller.SessionCallback.class,
        Distribute.class,
        HandlerUtils.class,
        AppCenterLog.class,
        Toast.class,
        ReleaseInstallerListener.class
})
public class ReleaseInstallerListenerTest {

    @Rule
    public PowerMockRule mPowerMockRule = new PowerMockRule();

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

        /* Mock context. */
        Context mockContext = mock(Context.class);

        /* Mock static classes. */
        mockStatic(InstallerUtils.class);
        mockStatic(HandlerUtils.class);
        mockStatic(Distribute.class);
        mockStatic(AppCenterLog.class);
        mockStatic(Toast.class);

        /* Mock progress dialog. */
        whenNew(android.app.ProgressDialog.class).withAnyArguments().thenReturn(mMockProgressDialog);
        when(mMockProgressDialog.isIndeterminate()).thenReturn(false);

        /* Mock toast. */
        when(Toast.makeText(any(Context.class), anyString(), anyInt())).thenReturn(mToast);

        /* Mock Distribute. */
        when(Distribute.getInstance()).thenReturn(mDistribute);
        doNothing().when(mDistribute).notifyInstallProgress(anyBoolean());

        /* Mock constructors and classes. */
        whenNew(FileInputStream.class).withAnyArguments().thenReturn(mock(FileInputStream.class));

        /* Mock download manager. */
        when(mDownloadManager.openDownloadedFile(anyLong())).thenReturn(mock(ParcelFileDescriptor.class));
        when(mockContext.getSystemService(anyString())).thenReturn(mDownloadManager);

        /* Create installer listener. */
        mReleaseInstallerListener = new ReleaseInstallerListener(mockContext);

        /* Set downloadId. */
        mReleaseInstallerListener.setDownloadId(1);

        /* Init install progress dialog. */
        mReleaseInstallerListener.showInstallProgressDialog(mock(Activity.class));

        /* Verify call methods. */
        verify(mMockProgressDialog).setProgressPercentFormat(any(NumberFormat.class));
        verify(mMockProgressDialog).setProgressNumberFormat(anyString());
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
        verifyStatic();
        AppCenterLog.error(anyString(), anyString(), any(FileNotFoundException.class));
    }

    @Test
    public void releaseInstallProcessWhenOnFinnishFailureWithContext() throws IOException {

        /* Mock progress dialog. */
        when(mMockProgressDialog.isIndeterminate()).thenReturn(true);

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic();
        InstallerUtils.installPackage(Matchers.<InputStream>any(), Matchers.<Context>any(), sessionListener.capture());

        /* Emulate session status. */
        sessionListener.getValue().onCreated(mMockSessionId);

        /* Verify that installer process was triggered in the Distribute. */
        sessionListener.getValue().onActiveChanged(mMockSessionId, true);
        verify(mDistribute).notifyInstallProgress(eq(true));

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        verifyStatic();
        HandlerUtils.runOnUiThread(any(Runnable.class));

        /* Verify that progress dialog was closed after finish install process*/
        sessionListener.getValue().onFinished(mMockSessionId, false);

        /* Verify that the handler was called again. */
        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mToast).show();
    }

    @Test
    public void releaseInstallerProcessWhenProgressDialogNull() throws Exception {

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic();
        InstallerUtils.installPackage(Matchers.<InputStream>any(), Matchers.<Context>any(), sessionListener.capture());

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
        verifyStatic();
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verity that progress dialog was updated. */
        sessionListener.getValue().onProgressChanged(mMockSessionId, 1);

        /* Verify that the handler was called and catch runnable. */
        runnable = ArgumentCaptor.forClass(Runnable.class);
        verifyStatic(times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog, never()).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process*/
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(times(3));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenDialogIsIndeterminate() throws Exception {

        /* Mock progress dialog. */
        when(mMockProgressDialog.isIndeterminate()).thenReturn(true);

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic();
        InstallerUtils.installPackage(Matchers.<InputStream>any(), Matchers.<Context>any(), sessionListener.capture());

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
        verifyStatic();
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog).setProgress(anyInt());
        verify(mMockProgressDialog).setMax(anyInt());
        verify(mMockProgressDialog, times(2)).setProgressPercentFormat(any(NumberFormat.class));
        verify(mMockProgressDialog, times(2)).setProgressNumberFormat(anyString());
        verify(mMockProgressDialog, times(2)).setIndeterminate(anyBoolean());

        /* Verify that progress dialog was closed after finish install process*/
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }

    @Test
    public void releaseInstallerProcessWhenWithContext() throws Exception {

        /* Start install process. */
        mReleaseInstallerListener.startInstall();

        /* Verify that installPackage method was called. */
        ArgumentCaptor<PackageInstaller.SessionCallback> sessionListener = ArgumentCaptor.forClass(PackageInstaller.SessionCallback.class);
        verifyStatic();
        InstallerUtils.installPackage(Matchers.<InputStream>any(), Matchers.<Context>any(), sessionListener.capture());

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
        verifyStatic();
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that the progress dialog was updated. */
        verify(mMockProgressDialog).setProgress(anyInt());

        /* Verify that progress dialog was closed after finish install process*/
        sessionListener.getValue().onFinished(mMockSessionId, true);

        /* Verify that the handler was called again. */
        verifyStatic(times(2));
        HandlerUtils.runOnUiThread(runnable.capture());
        runnable.getValue().run();

        /* Verify that installer process was triggered in the Distribute again. */
        verify(mDistribute).notifyInstallProgress(eq(false));
    }
}
